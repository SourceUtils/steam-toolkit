package com.timepath.steam.io.util;

import java.util.logging.Logger;
import javax.swing.tree.DefaultMutableTreeNode;

/**
 *
 * @author TimePath
 */
public class BlobNode extends DefaultMutableTreeNode {

    public BlobNode() {
    }

    public BlobNode(Object obj) {
        this.setUserObject(obj);
    }

    public BlobNode(String name, Object obj) {
        this.name = name;
        this.setUserObject(obj);
    }

    private String name;

    private int dataType = -1;

    public int getDataType() {
        return dataType;
    }

    public void setDataType(int dataType) {
        this.dataType = dataType;
    }

    @Override
    public String toString() {
        Object o = this.getUserObject();
        if(o == null) {
            return "unnamed";
        } else if(o instanceof String) {
            return (String) o;
        } else {
            return (name != null ? name : o.getClass().getSimpleName()) + ": " + o;
        }
    }

    private int meta;

    public boolean isMeta() {
        return meta != 0;
    }

    public int getMeta() {
        return meta;
    }

    public void setMeta(int meta) {
        this.meta = meta;
    }

    private static final Logger LOG = Logger.getLogger(BlobNode.class.getName());

}