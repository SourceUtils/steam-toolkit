package com.timepath.steam.io.blob;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.logging.Logger;

/**
 * @author TimePath
 * @deprecated Blob files are no longer in use
 */
@Deprecated
public class BlobNode extends DefaultMutableTreeNode {

    private static final Logger LOG = Logger.getLogger(BlobNode.class.getName());
    private String name;
    private int dataType = -1;
    private int meta;

    public BlobNode() {
    }

    public BlobNode(Object obj) {
        setUserObject(obj);
    }

    private BlobNode(String name, Object obj) {
        this.name = name;
        setUserObject(obj);
    }

    public int getDataType() {
        return dataType;
    }

    public void setDataType(int dataType) {
        this.dataType = dataType;
    }

    @Override
    public String toString() {
        Object o = getUserObject();
        if(o == null) {
            return "unnamed";
        } else {
            return ( o instanceof String )
                   ? (String) o
                   : ( ( ( name != null ) ? name : o.getClass().getSimpleName() ) + ": " + o );
        }
    }

    public boolean isMeta() {
        return meta != 0;
    }

    public int getMeta() {
        return meta;
    }

    public void setMeta(int meta) {
        this.meta = meta;
    }
}
