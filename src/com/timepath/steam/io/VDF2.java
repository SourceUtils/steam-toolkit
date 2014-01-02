package com.timepath.steam.io;

import com.timepath.Diff;
import com.timepath.Node;
import com.timepath.Pair;
import com.timepath.steam.io.VDF2.VDFProperty;
import java.util.*;
import java.util.logging.Logger;

/**
 *
 * Standard KeyValues format loader
 *
 * @author TimePath
 */
public class VDF2 extends Node<VDFProperty, VDF2> {

    private static final Comparator<VDFProperty> COMPARATOR_KEY = new Comparator<VDFProperty>() {

        public int compare(VDFProperty o1, VDFProperty o2) {
            return o1.getKey().compareTo(o2.getKey());
        }

    };

    private static final Comparator<VDFProperty> COMPARATOR_VALUE = new Comparator<VDFProperty>() {

        public int compare(VDFProperty o1, VDFProperty o2) {
            return o1.getValue().hashCode() - o2.getValue().hashCode();
        }

    };

    private static final Logger LOG = Logger.getLogger(VDF2.class.getName());

    public VDF2() {
        this("VDF");
    }

    public VDF2(Object name) {
        super(name);
    }

    public VDF2 deepClone() {
        VDF2 clone = new VDF2(this.custom);
        for(VDF2 v : this.getNodes()) {
            clone.addNode(v.deepClone());
        }
        for(VDFProperty p : this.getProperties()) {
            clone.addProperty(new VDFProperty(p.getKey(), p.getValue()));
        }
        return clone;
    }

    /**
     * Diffs the properties of both VDF nodes
     * <p>
     * @param other The other node
     * <p>
     * @return
     */
    public Diff<VDFProperty> diff(VDF2 other) {
        return Diff.diff(this.getProperties(), other.getProperties(), COMPARATOR_KEY, COMPARATOR_VALUE);
    }

    public Diff<VDF2> rdiff2(VDF2 other) {
        Diff<VDF2> d = new Diff<VDF2>();

        d.in = this;
        d.out = other;

        VDF2 removed = new VDF2("Removed");
        VDF2 added = new VDF2("Added");
        VDF2 potential = new VDF2("Potential");
        VDF2 potential2 = new VDF2("Potential2");
        VDF2 same = new VDF2("Same");
        
        for(VDF2 v : this.getNodes()) {
            VDF2 match = other.getNamedNode(v.custom);
            if(match == null) { // Not in new copy
                removed.addNode(v);
            } else {
                potential.addNode(match);
            }
        }
        for(VDF2 v : other.getNodes()) {
            VDF2 match = this.getNamedNode(v.custom);
            if(match == null) { // Not in this copy
                added.addNode(v);
            } else {
                potential2.addNode(match);
            }
        }

        for(VDF2 v : potential.getNodes()) {
            VDF2 v2 = potential2.getNamedNode(v.custom);
            Diff<VDFProperty> diff = v2.diff(v); // FIXME: backwards for some reason
            if(diff.added.size() + diff.removed.size() + diff.modified.size() > 0) { // Something was changed
                VDF2 na = new VDF2(v.custom), nr = new VDF2(v.custom);
                for(VDFProperty a : diff.added) {
                    na.addProperty(a);
                }
                added.addNode(na);
                for(VDFProperty r : diff.removed) {
                    nr.addProperty(r);
                }
                removed.addNode(nr);
                for(Pair<VDFProperty, VDFProperty> p : diff.modified) { // TODO: push to modified
                    nr.addProperty(p.getKey());
                    na.addProperty(p.getValue());
                }
                // This could be a mixture of additions, removals or modifications, as well as unchanged values
            } else {
                same.addNode(v);
            }
            
        }

        debug(d.in, d.out, removed, added);

        return d;
    }

    /**
     * Diffs the child nodes of both VDF nodes. TODO: breadth first would be more efficient with large differences
     * <p>
     * @param other The other node
     * <p>
     * @return
     */
    public Diff<VDF2> rdiff(VDF2 other) {
        Diff<VDF2> d = new Diff<VDF2>();

        d.in = this;
        d.out = other;

        VDF2 removed = new VDF2("Removed");
        VDF2 added = new VDF2("Added");
        VDF2 same = new VDF2("Same");
        VDF2 modified = new VDF2("Modified");

        for(VDF2 v : this.getNodes()) {
            VDF2 match = other.getNamedNode(v.custom);
            if(match == null) { // Not in new copy
                removed.addNode(v);
            } else {
                same.addNode(match); // TODO: check for differences
            }
        }
        for(VDF2 v : other.getNodes()) {
            VDF2 match = this.getNamedNode(v.custom);
            if(match == null) { // Not in this copy
                added.addNode(v);
            } else {
                Diff<VDFProperty> diff = v.diff(same.getNamedNode(v.custom));
                if(diff.added.size() + diff.removed.size() + diff.modified.size() >= 0) { // Something was changed
                    // This could be a mixture of additions, removals or modifications, as well as unchanged values
                }
            }
        }

        d.removed = Arrays.asList(removed);
        d.same = Arrays.asList(same);
        d.added = Arrays.asList(added);
//        d.modified = Arrays.asList(modified.getChildren();

        return d;
    }

    public static class VDFProperty extends Pair<String, Object> {

        private static final Logger LOG = Logger.getLogger(VDFProperty.class.getName());

        private static final String TAB = "    ";

        public VDFProperty(String key, Object val) {
            super(key, val);
        }

        @Override
        public String toString() {
            return "\"" + this.getKey() + "\"" + TAB + "\"" + this.getValue() + "\"";
        }

    }

}
