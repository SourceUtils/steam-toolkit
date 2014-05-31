package com.timepath.steam.io;

import com.timepath.Diff;
import com.timepath.Node;
import com.timepath.Pair;
import com.timepath.steam.io.VDFParser.NodeContext;
import com.timepath.steam.io.VDFParser.PairContext;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedList;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Standard KeyValues format loader
 *
 * @author TimePath
 */
public class VDF2 extends Node<VDF2.VDFProperty, VDF2> {

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
    private static final Logger                  LOG              = Logger.getLogger(VDF2.class.getName());

    private VDF2() {
        this("VDF");
    }

    public VDF2(Object name) {
        super(name);
    }

    public static VDF2 load(InputStream is) throws IOException {
        VDFLexer lexer = new VDFLexer(new ANTLRInputStream(is));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        VDFParser parser = new VDFParser(tokens);
        VDF2 v = new VDF2();
        final Deque<VDF2> stack = new LinkedList<>();
        stack.push(v);
        ParseTreeWalker.DEFAULT.walk(new VDFBaseListener() {
            private final Pattern UNQUOTE = Pattern.compile("\"((?:\\\\\"|.)*?)\"");

            @Override
            public void enterNode(NodeContext ctx) {
                stack.push(new VDF2(u(ctx.name.getText())));
            }

            @Override
            public void exitNode(NodeContext ctx) {
                VDF2 current = stack.pop();
                stack.peek().addNode(current);
            }

            @Override
            public void exitPair(PairContext ctx) {
                stack.peek().addProperty(new VDFProperty(u(ctx.key.getText()), u(ctx.value.getText())));
            }

            private String u(String s) {
                if(s.startsWith("\"")) {
                    Matcher m = UNQUOTE.matcher(s);
                    if(m.find()) return m.group(1);
                }
                return s;
            }
        }, parser.parse());
        return v;
    }

    VDF2 deepClone() {
        VDF2 clone = new VDF2(custom);
        for(VDF2 v : getNodes()) {
            clone.addNode(v.deepClone());
        }
        for(VDFProperty p : getProperties()) {
            clone.addProperty(new VDFProperty(p.getKey(), p.getValue()));
        }
        return clone;
    }

    public Diff<VDF2> rdiff2(VDF2 other) {
        Diff<VDF2> d = new Diff<>();
        d.in = this;
        d.out = other;
        VDF2 removed = new VDF2("Removed");
        VDF2 added = new VDF2("Added");
        VDF2 potential = new VDF2("Potential");
        VDF2 potential2 = new VDF2("Potential2");
        VDF2 same = new VDF2("Same");
        for(VDF2 v : getNodes()) {
            VDF2 match = other.getNamedNode(v.custom);
            if(match == null) { // Not in new copy
                removed.addNode(v);
            } else {
                potential.addNode(match);
            }
        }
        for(VDF2 v : other.getNodes()) {
            VDF2 match = getNamedNode(v.custom);
            if(match == null) { // Not in this copy
                added.addNode(v);
            } else {
                potential2.addNode(match);
            }
        }
        for(VDF2 v : potential.getNodes()) {
            VDF2 v2 = potential2.getNamedNode(v.custom);
            Diff<VDFProperty> diff = v2.diff(v); // FIXME: backwards for some reason
            if(( diff.added.size() + diff.removed.size() + diff.modified.size() ) > 0) { // Something was changed
                VDF2 na = new VDF2(v.custom);
                VDF2 nr = new VDF2(v.custom);
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
    Diff<VDFProperty> diff(VDF2 other) {
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
    public Diff<VDF2> rdiff(VDF2 other) {
        Diff<VDF2> d = new Diff<>();
        d.in = this;
        d.out = other;
        VDF2 removed = new VDF2("Removed");
        VDF2 added = new VDF2("Added");
        VDF2 same = new VDF2("Same");
        VDF2 modified = new VDF2("Modified");
        for(VDF2 v : getNodes()) {
            VDF2 match = other.getNamedNode(v.custom);
            if(match == null) { // Not in new copy
                removed.addNode(v);
            } else {
                same.addNode(match); // TODO: check for differences
            }
        }
        for(VDF2 v : other.getNodes()) {
            VDF2 match = getNamedNode(v.custom);
            if(match == null) { // Not in this copy
                added.addNode(v);
            }
            //            else {
            //                Diff<VDFProperty> diff = v.diff(same.getNamedNode(v.custom));
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
