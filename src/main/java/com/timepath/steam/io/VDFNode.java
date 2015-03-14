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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.text.MessageFormat;
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

    private static final Comparator<VDFProperty> COMPARATOR_KEY = new Comparator<VDFProperty>() {
        @Override
        public int compare(@NotNull VDFProperty o1, @NotNull VDFProperty o2) {
            return o1.getKey().compareTo(o2.getKey());
        }
    };
    private static final Comparator<VDFProperty> COMPARATOR_VALUE = new Comparator<VDFProperty>() {
        @Override
        public int compare(@NotNull VDFProperty o1, @NotNull VDFProperty o2) {
            return o1.getValue().hashCode() - o2.getValue().hashCode();
        }
    };
    private static final Logger LOG = Logger.getLogger(VDFNode.class.getName());
    @Nullable
    private String conditional;

    protected VDFNode() {
        this("VDF");
    }

    public VDFNode(@NotNull InputStream is, @NotNull Charset c) throws IOException {
        @NotNull VDFLexer lexer = new VDFLexer(new ANTLRInputStream(new InputStreamReader(is, c)));
        @NotNull CommonTokenStream tokens = new CommonTokenStream(lexer);
        @NotNull VDFParser parser = new VDFParser(tokens);
        @NotNull final Deque<VDFNode> stack = new LinkedList<>();
        stack.push(this);
        ParseTreeWalker.DEFAULT.walk(new VDFBaseListener() {
            @Override
            public void enterNode(@NotNull NodeContext ctx) {
                @Nullable String conditional = ctx.conditional != null ? ctx.conditional.getText() : null;
                stack.push(new VDFNode(u(ctx.name.getText())));
                stack.peek().conditional = conditional;
            }

            @Override
            public void exitNode(NodeContext ctx) {
                @NotNull VDFNode current = stack.pop();
                stack.peek().addNode(current);
            }

            @Override
            public void exitPair(@NotNull PairContext ctx) {
                @Nullable String conditional = ctx.conditional != null ? ctx.conditional.getText() : null;
                stack.peek().addProperty(new VDFProperty(u(ctx.key.getText()), u(ctx.value.getText()), conditional));
            }

            @NotNull
            private String u(@NotNull String s) {
                if (s.startsWith("\"")) return s.substring(1, s.length() - 1).replace("\\\"", "\"");
                return s;
            }
        }, parser.parse());
    }

    public VDFNode(Object name) {
        super(name);
    }

    @Nullable
    public String getConditional() {
        return conditional;
    }

    public void setConditional(@Nullable String conditional) {
        this.conditional = conditional;
    }

    @NotNull
    @Override
    public String toString() {
        return super.toString() + (conditional == null ? "" : "    " + conditional);
    }

    @NotNull
    public Diff<VDFNode> rdiff2(@NotNull VDFNode other) {
        @NotNull Diff<VDFNode> d = new Diff<>();
        d.in = this;
        d.out = other;
        @NotNull VDFNode removed = new VDFNode("Removed");
        @NotNull VDFNode added = new VDFNode("Added");
        @NotNull VDFNode potential = new VDFNode("Potential");
        @NotNull VDFNode potential2 = new VDFNode("Potential2");
        @NotNull VDFNode same = new VDFNode("Same");
        for (@NotNull VDFNode v : getNodes()) {
            @Nullable VDFNode match = other.get(v.custom);
            if (match == null) { // Not in new copy
                removed.addNode(v);
            } else {
                potential.addNode(match);
            }
        }
        for (@NotNull VDFNode v : other.getNodes()) {
            @Nullable VDFNode match = get(v.custom);
            if (match == null) { // Not in this copy
                added.addNode(v);
            } else {
                potential2.addNode(match);
            }
        }
        for (@NotNull VDFNode v : potential.getNodes()) {
            @Nullable VDFNode v2 = potential2.get(v.custom);
            @NotNull Diff<VDFProperty> diff = v2.diff(v); // FIXME: backwards for some reason
            if ((diff.added.size() + diff.removed.size() + diff.modified.size()) > 0) { // Something was changed
                @NotNull VDFNode na = new VDFNode(v.custom);
                @NotNull VDFNode nr = new VDFNode(v.custom);
                for (VDFProperty a : diff.added) {
                    na.addProperty(a);
                }
                added.addNode(na);
                for (VDFProperty r : diff.removed) {
                    nr.addProperty(r);
                }
                removed.addNode(nr);
                for (@NotNull Pair<VDFProperty, VDFProperty> p : diff.modified) { // TODO: push to modified
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
     * @param other The other node
     * @return
     */
    @NotNull
    Diff<VDFProperty> diff(@NotNull VDFNode other) {
        return Diff.diff(getProperties(), other.getProperties(), COMPARATOR_KEY, COMPARATOR_VALUE);
    }

    @NotNull
    public String save() {
        @NotNull StringBuilder sb = new StringBuilder();
        // preceding header
        for (@NotNull VDFProperty p : properties) {
            if (String.valueOf(p.getValue()).isEmpty()) {
                if ("\\n".equals(p.getKey())) {
                    sb.append('\n');
                }
                if ("//".equals(p.getKey())) {
                    sb.append("//").append(p.getInfo()).append('\n');
                }
            }
        }
        sb.append(custom).append('\n');
        sb.append("{\n");
        for (@NotNull VDFProperty p : properties) {
            if (!String.valueOf(p.getValue()).isEmpty()) {
                if ("\\n".equals(p.getKey())) {
                    sb.append("\t    \n");
                } else {
                    sb.append("\t    ").append(p.getKey()).append("\t    ").append(p.getValue());
                    if (p.getInfo() != null) sb.append(' ').append(p.getInfo());
                    sb.append('\n');
                }
            }
        }
        sb.append("}\n");
        return sb.toString();
    }

    /**
     * Diffs the child nodes of both VDF nodes. TODO: breadth first would be more efficient with large differences
     *
     * @param other The other node
     * @return
     */
    @NotNull
    @Override
    public Diff<VDFNode> rdiff(@NotNull VDFNode other) {
        @NotNull Diff<VDFNode> d = new Diff<>();
        d.in = this;
        d.out = other;
        @NotNull VDFNode removed = new VDFNode("Removed");
        @NotNull VDFNode added = new VDFNode("Added");
        @NotNull VDFNode same = new VDFNode("Same");
        @NotNull VDFNode modified = new VDFNode("Modified");
        for (@NotNull VDFNode v : getNodes()) {
            @Nullable VDFNode match = other.get(v.custom);
            if (match == null) { // Not in new copy
                removed.addNode(v);
            } else {
                same.addNode(match); // TODO: check for differences
            }
        }
        for (@NotNull VDFNode v : other.getNodes()) {
            @Nullable VDFNode match = get(v.custom);
            if (match == null) { // Not in this copy
                added.addNode(v);
            }
            //else {
            //    Diff<VDFProperty> diff = v.diff(same.get(v.custom));
            //    if(diff.added.size() + diff.removed.size() + diff.modified.size() >= 0) { // Something was changed
            //        // This could be a mixture of additions, removals or modifications, as well as unchanged values
            //    }
            //}
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
        private String conditional;

        public VDFProperty(String key, Object val) {
            this(key, val, null);
        }

        public VDFProperty(String key, Object val, String conditional) {
            super(key, val);
            this.conditional = conditional;
        }

        public String getConditional() {
            return conditional;
        }

        public void setConditional(String conditional) {
            this.conditional = conditional;
        }

        @NotNull
        @Override
        public String toString() {
            return MessageFormat.format("''{1}''{0}''{2}''{3}",
                    TAB,
                    getKey(),
                    getValue(),
                    conditional == null ? "" : TAB + conditional);
        }

        @NotNull
        public String getInfo() {
            return "";
        }
    }
}
