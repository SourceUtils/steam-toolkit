package com.timepath.steam.io;

import com.timepath.io.ByteBufferInputStream;
import com.timepath.io.utils.Savable;
import java.io.*;
import java.nio.ByteBuffer;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.tree.DefaultMutableTreeNode;

/**
 *
 * https://github.com/harvimt/steam_launcher/blob/master/binvdf.py
 * https://github.com/barneygale/bvdf/blob/master/bvdf.py
 * https://github.com/DHager/hl2parse
 * http://cs.rin.ru/forum/viewtopic.php?f=20&t=61506&hilit=appinfo
 * http://cs.rin.ru/forum/viewtopic.php?f=20&t=62438&hilit=packageinfo
 * http://media.steampowered.com/steamcommunity/public/images/apps/[appID]/[sha].[ext]
 * http://cdr.xpaw.ru/app/5/#section_info
 * http://hlssmod.net/he_code/public/tier1/KeyValues.h
 * http://hpmod.googlecode.com/svn/trunk/tier1/KeyValues.cpp
 *
 * @author TimePath
 */
public class BVDF implements Savable {

    private static final Logger LOG = Logger.getLogger(BVDF.class.getName());

    private DataNode root;

    public BVDF() {
        root = new DataNode("BVDF");
    }

    public DataNode getRoot() {
        return root;
    }

    @Override
    public void readExternal(ByteBuffer buf) {
        readExternal(new ByteBufferInputStream(buf));
    }

    @Override
    public void readExternal(InputStream in) {
        try {
            new BVDFStream(in, new BVDFListener() {
                private DataNode last = root;

                @Override
                public void emit(String key, Object val) {
                    last.add(new DataNode(key, val));
                }

                @Override
                public void pop() {
                    last = (DataNode) last.getParent();
                }

                @Override
                public void push(Object key) {
                    DataNode node = new DataNode(key);
                    last.add(node);
                    last = node;
                }
            }).read();
        } catch(IOException ex) {
            Logger.getLogger(BVDF.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void writeExternal(OutputStream out) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public static class DataNode extends DefaultMutableTreeNode {

        public String name;

        public Object value;

        DataNode(Object obj) {
            this.name = obj.toString();
        }

        DataNode(String name, Object obj) {
            this.name = name;
            this.value = obj;
        }

        DataNode() {
        }

        public DataNode get(String key) {
            DataNode node;
            for(Object o : this.children) {
                if(!(o instanceof DataNode)) {
                    continue;
                }
                node = (DataNode) o;
                if(node.name.equals(key)) {
                    return node;
                }
            }
            return null;
        }

        @Override
        public String toString() {
            String nameStr = "";
            if(name != null) {
                nameStr = name;
            }
            String splitStr = "";
            if(name != null && value != null) {
                splitStr = ": ";
            }
            String valStr = "";
            if(value != null) {
                valStr = value.toString();
                if(value instanceof byte[]) {
                    valStr = Arrays.toString((byte[]) value);
                }
                valStr += " [" + value.getClass().getSimpleName() + "]";
            }
            return MessageFormat.format("{0}{1}{2}", nameStr, splitStr, valStr);
        }

    }

}
