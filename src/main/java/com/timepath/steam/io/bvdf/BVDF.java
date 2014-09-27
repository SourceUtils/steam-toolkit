package com.timepath.steam.io.bvdf;

import com.timepath.io.ByteBufferInputStream;
import com.timepath.io.utils.Savable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.tree.DefaultMutableTreeNode;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Creates a swing tree from a binary VDF file
 *
 * @author TimePath
 */
public class BVDF implements Savable {

    private static final Logger LOG = Logger.getLogger(BVDF.class.getName());
    @NotNull
    private final DataNode root;

    public BVDF() {
        root = new DataNode("BVDF");
    }

    @NotNull
    public DataNode getRoot() {
        return root;
    }

    @Override
    public void readExternal(@NotNull ByteBuffer buf) {
        readExternal(new ByteBufferInputStream(buf));
    }

    @Override
    public void readExternal(@NotNull InputStream in) {
        try {
            new BVDFStream(in, new BVDFListener() {
                @NotNull
                private DataNode last = root;

                @Override
                public void emit(String key, Object value) {
                    last.add(new DataNode(key, value));
                }

                @Override
                public void pop() {
                    last = (DataNode) last.getParent();
                }

                @Override
                public void push(@NotNull Object section) {
                    @NotNull DataNode node = new DataNode(section);
                    last.add(node);
                    last = node;
                }
            }).read();
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Not supported yet
     */
    @Override
    public void writeExternal(OutputStream out) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public static class DataNode extends DefaultMutableTreeNode {

        public String name;
        public Object value;

        DataNode(@NotNull Object obj) {
            name = obj.toString();
        }

        DataNode(String name, Object obj) {
            this.name = name;
            value = obj;
        }

        @Nullable
        public DataNode get(String key) {
            for (Object o : children) {
                if (!(o instanceof DataNode)) {
                    continue;
                }
                @NotNull DataNode node = (DataNode) o;
                if (node.name.equals(key)) {
                    return node;
                }
            }
            return null;
        }

        @NotNull
        @Override
        public String toString() {
            String nameStr = "";
            if (name != null) {
                nameStr = name;
            }
            @NotNull String splitStr = "";
            if ((name != null) && (value != null)) {
                splitStr = ": ";
            }
            String valStr = "";
            if (value != null) {
                valStr = value.toString();
                if (value instanceof byte[]) {
                    valStr = Arrays.toString((byte[]) value);
                }
                valStr += " [" + value.getClass().getSimpleName() + ']';
            }
            return MessageFormat.format("{0}{1}{2}", nameStr, splitStr, valStr);
        }
    }
}
