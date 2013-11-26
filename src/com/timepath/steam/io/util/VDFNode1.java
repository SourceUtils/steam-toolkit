package com.timepath.steam.io.util;

import com.timepath.steam.io.VDF1.VDFToken;
import java.io.File;
import java.util.ArrayList;
import java.util.logging.Logger;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;

/**
 *
 * @author TimePath
 */
public class VDFNode1 extends DefaultMutableTreeNode {

    public VDFNode1() {
    }

    private VDFToken key;

    public VDFNode1(VDFToken key) {
        this.key = key;
    }

    public VDFNode1(String str) {
        this.key = new VDFToken(str);
    }

    public void setKey(String key) {
        this.key.setValue(key);
    }

    public String getKey() {
        return key.getValue();
    }

    public VDFNode1 get(int index) {
        return (VDFNode1) this.getChildAt(index);
    }

    public VDFNode1 get(String key) {
        VDFNode1 node;
        for(Object o : this.children) {
            if(!(o instanceof VDFNode1)) {
                continue;
            }
            node = (VDFNode1) o;
            if(node.getKey().equals(key)) {
                return node;
            }
        }
        return null;
    }

    public void add(VDFNode1 v) {
        super.add(v);
    }

    private VDFToken value;

    public void setValue(VDFToken t) {
        value = t;
    }

    public void setValue(Object o) {
        value.setValue(o.toString());
    }

    public String getValue() {
        if(value == null) {
            return null;
        }
        return value.getValue();
    }

    public int intValue() {
        return Integer.parseInt(getValue());
    }

    @Override
    public String toString() {
        if(this.isLeaf()) {
            return getKey() + " == " + getValue();
        } else {
            return this.getKey();
        }
    }

    private static final Logger LOG = Logger.getLogger(VDFNode1.class.getName());

    public ArrayList<Property> getProperties() {
        ArrayList<Property> props = new ArrayList<Property>();
        for(int i = 0; i < this.getChildCount(); i++) {
            TreeNode tn = this.getChildAt(i);
            if(!(tn instanceof VDFNode1)) {
                continue;
            }
            VDFNode1 v = (VDFNode1) tn;
            props.add(new Property(v.getKey(), v.getValue(), ""));
        }
        return props;
    }

    protected String fileName;

    public void setFile(File file) {
        setFile(file.getName());
    }

    public void setFile(String name) { // todo: case insensitivity
        if(name == null) {
            this.fileName = null;
            return;
        }
        if(name.contains(".")) {
            this.fileName = name.split("\\.")[0];
        } else {
            this.fileName = name;
        }
    }

    public String getFile() {
        return fileName;
    }

}
