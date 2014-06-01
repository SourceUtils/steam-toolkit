package com.timepath.steam.io;

import com.timepath.Diff;
import com.timepath.Node;
import com.timepath.Pair;

import java.util.Arrays;
import java.util.Comparator;
import java.util.logging.Logger;

/**
 * Standard KeyValues format loader
 *
 * @author TimePath
 */
public class VDFNode extends Node<VDFNode.VDFProperty, VDFNode> {

    private static final Comparator<VDFProperty> COMPARATOR_KEY   = new Comparator<VDFProperty>() {
        @Override
        public int compare(VDFProperty o1, VDFProperty o2) {
            return o1.getKey().compareTo(o2.getKey());
        }
    };
    private static final Comparator<VDFProperty> COMPARATOR_VALUE = new Comparator<VDFProperty>() {
        @Override
        public int compare(VDFProperty o1, VDFProperty o2) {
            return o1.getValue().hashCode() - o2.getValue().hashCode();
        }
    };
    private static final Logger                  LOG              = Logger.getLogger(VDFNode.class.getName());

    VDFNode() {
        this("VDF");
    }

    public VDFNode(Object name) {
        super(name);
    }

    public Diff<VDFNode> rdiff2(VDFNode other) {
        Diff<VDFNode> d = new Diff<>();
        d.in = this;
        d.out = other;
        VDFNode removed = new VDFNode("Removed");
        VDFNode added = new VDFNode("Added");
        VDFNode potential = new VDFNode("Potential");
        VDFNode potential2 = new VDFNode("Potential2");
        VDFNode same = new VDFNode("Same");
        for(VDFNode v : getNodes()) {
            VDFNode match = other.get(v.custom);
            if(match == null) { // Not in new copy
                removed.addNode(v);
            } else {
                potential.addNode(match);
            }
        }
        for(VDFNode v : other.getNodes()) {
            VDFNode match = get(v.custom);
            if(match == null) { // Not in this copy
                added.addNode(v);
            } else {
                potential2.addNode(match);
            }
        }
        for(VDFNode v : potential.getNodes()) {
            VDFNode v2 = potential2.get(v.custom);
            Diff<VDFProperty> diff = v2.diff(v); // FIXME: backwards for some reason
            if(( diff.added.size() + diff.removed.size() + diff.modified.size() ) > 0) { // Something was changed
                VDFNode na = new VDFNode(v.custom);
                VDFNode nr = new VDFNode(v.custom);
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
        Node.debug(d.in, d.out, removed, added);
        return d;
    }

    /**
     * Diffs the properties of both VDF nodes
     *
     * @param other
     *         The other node
     *
     * @return
     */
    Diff<VDFProperty> diff(VDFNode other) {
        return Diff.diff(getProperties(), other.getProperties(), COMPARATOR_KEY, COMPARATOR_VALUE);
    }

    /**
     * Diffs the child nodes of both VDF nodes. TODO: breadth first would be more efficient with large differences
     *
     * @param other
     *         The other node
     *
     * @return
     */
    @Override
    public Diff<VDFNode> rdiff(VDFNode other) {
        Diff<VDFNode> d = new Diff<>();
        d.in = this;
        d.out = other;
        VDFNode removed = new VDFNode("Removed");
        VDFNode added = new VDFNode("Added");
        VDFNode same = new VDFNode("Same");
        VDFNode modified = new VDFNode("Modified");
        for(VDFNode v : getNodes()) {
            VDFNode match = other.get(v.custom);
            if(match == null) { // Not in new copy
                removed.addNode(v);
            } else {
                same.addNode(match); // TODO: check for differences
            }
        }
        for(VDFNode v : other.getNodes()) {
            VDFNode match = get(v.custom);
            if(match == null) { // Not in this copy
                added.addNode(v);
            }
            //            else {
            //                Diff<VDFProperty> diff = v.diff(same.get(v.custom));
            //                if(diff.added.size() + diff.removed.size() + diff.modified.size() >= 0) { // Something was changed
            //                    // This could be a mixture of additions, removals or modifications, as well as unchanged values
            //                }
            //            }
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
            return '"' + getKey() + '"' + TAB + '"' + getValue() + '"';
        }
    }
}
