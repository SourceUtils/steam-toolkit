package com.timepath.steam.io;

import com.timepath.steam.io.VDFNode.VDFProperty;
import com.timepath.steam.io.VDFParser.NodeContext;
import com.timepath.steam.io.VDFParser.PairContext;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Deque;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author TimePath
 */
public class VDF {

    public static boolean isBinary(File f) {
        try {
            RandomAccessFile rf = new RandomAccessFile(f, "r");
            rf.seek(rf.length() - 1);
            int r = rf.read();
            return ( r == 0x00 ) || ( r == 0x08 ) || ( r == 0xFF );
        } catch(IOException ex) {
            Logger.getLogger(VDF1.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }

    public static VDFNode load(File f) throws IOException {
        return load(new FileInputStream(f));
    }

    public static VDFNode load(InputStream is) throws IOException {
        return load(is, StandardCharsets.UTF_8);
    }

    public static VDFNode load(InputStream is, Charset c) throws IOException {
        VDFLexer lexer = new VDFLexer(new ANTLRInputStream(new InputStreamReader(is, c)));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        VDFParser parser = new VDFParser(tokens);
        VDFNode v = new VDFNode();
        final Deque<VDFNode> stack = new LinkedList<>();
        stack.push(v);
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
        return v;
    }

    public static VDFNode load(File f, Charset c) throws IOException {
        return load(new FileInputStream(f), c);
    }
}
