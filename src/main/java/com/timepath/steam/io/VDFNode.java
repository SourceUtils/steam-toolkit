package com.timepath.steam.io;

import com.timepath.Diff;
import com.timepath.Node;
import com.timepath.Pair;
import com.timepath.steam.io.VDFNode.VDFProperty;
import com.timepath.steam.io.VDFParser.NodeContext;
import com.timepath.steam.io.VDFParser.PairContext;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedList;
import java.util.logging.Logger;

/**
 * Standard KeyValues format loader
 *
 * @author TimePath
 */
public class VDFNode extends Node<VDFProperty, VDFNode> {

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

    protected VDFNode() {
        this("VDF");
    }

    public VDFNode(InputStream is, Charset c) throws IOException {
        VDFLexer lexer = new VDFLexer(new ANTLRInputStream(new InputStreamReader(is, c)));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        VDFParser parser = new VDFParser(tokens);
        final Deque<VDFNode> stack = new LinkedList<>();
        stack.push(this);
        ParseTreeWalker.DEFAULT.walk(new VDFBaseListener() {
            @Override
            public void enterNode(NodeContext ctx) {
                stack.push(new VDFNode(u(ctx.name.getText())));
            }

            @Override
            public void exitNode(NodeContext ctx) {
                VDFNode current = stack.pop();
                stack.peek().addNode(current);
            }

            @Override
            public void exitPair(PairContext ctx) {
                stack.peek().addProperty(new VDFProperty(u(ctx.key.getText()), u(ctx.value.getText())));
            }

            private String u(String s) {
                if(s.startsWith("\"")) return s.substring(1, s.length() - 1).replace("\\\"", "\"");
                return s;
            }
        }, parser.parse());
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

    public String save() {
        StringBuilder sb = new StringBuilder();
        // preceding header
        for(VDFProperty p : properties) {
            if(String.valueOf(p.getValue()).isEmpty()) {
                if("\\n".equals(p.getKey())) {
                    sb.append('\n');
                }
                if("//".equals(p.getKey())) {
                    sb.append("//").append(p.getInfo()).append('\n');
                }
            }
        }
        sb.append(custom).append('\n');
        sb.append("{\n");
        for(VDFProperty p : properties) {
            if(!String.valueOf(p.getValue()).isEmpty()) {
                sb.append("\\n".equals(p.getKey()) ? "\t    \n" : ( "\t    " + p.getKey() + "\t    " + p.getValue() +
                                                                    ( ( p.getInfo() != null ) ? ( ' ' + p.getInfo() ) : "" ) +
                                                                    '\n' ));
            }
        }
        sb.append("}\n");
        return sb.toString();
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

        public String getInfo() {
            return "";
        }
    }
}
