package com.timepath.steam.io.util;

import com.timepath.steam.io.VDF1;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

/**
 * @author TimePath
 */
public class VDFNode1 extends DefaultMutableTreeNode {

    private static final Logger LOG = Logger.getLogger(VDFNode1.class.getName());
    private VDF1.VDFToken key;
    private VDF1.VDFToken value;
    private String        fileName;

    protected VDFNode1() {
    }

    public VDFNode1(VDF1.VDFToken key) {
        this.key = key;
    }

    public VDFNode1(String str) {
        key = new VDF1.VDFToken(str);
    }

    public VDFNode1 get(int index) {
        return (VDFNode1) getChildAt(index);
    }

    public VDFNode1 get(String key) {
        VDFNode1 node;
        for(Object o : children) {
            if(!( o instanceof VDFNode1 )) {
                continue;
            }
            node = (VDFNode1) o;
            if(node.getKey().equals(key)) {
                return node;
            }
        }
        return null;
    }

    public String getKey() {
        return key.getValue();
    }

    public void setKey(String key) {
        this.key.setValue(key);
    }

    @Override
    public void add(MutableTreeNode v) {
        super.add(v);
    }

    @Override
    public String toString() {
        return isLeaf() ? ( getKey() + " == " + getValue() ) : getKey();
    }

    public String getValue() {
        if(value == null) {
            return null;
        }
        return value.getValue();
    }

    public void setValue(VDF1.VDFToken t) {
        value = t;
    }

    public void setValue(Object o) {
        value.setValue(o.toString());
    }

    public int intValue() {
        return Integer.parseInt(getValue());
    }

    public List<Property> getProperties() {
        List<Property> props = new LinkedList<>();
        for(int i = 0; i < getChildCount(); i++) {
            TreeNode tn = getChildAt(i);
            if(!( tn instanceof VDFNode1 )) {
                continue;
            }
            VDFNode1 v = (VDFNode1) tn;
            props.add(new Property(v.getKey(), v.getValue(), ""));
        }
        return props;
    }

    public void setFile(File file) {
        setFile(file.getName());
    }

    public String getFile() {
        return fileName;
    }

    protected void setFile(String name) { // TODO: case insensitivity
        if(name == null) {
            fileName = null;
            return;
        }
        if(name.contains(".")) {
            fileName = name.split("\\.")[0];
        } else {
            fileName = name;
        }
    }
}
