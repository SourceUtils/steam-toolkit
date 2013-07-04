package com.timepath.steam.io;

import com.timepath.io.utils.Savable;
import com.timepath.steam.io.VDF.Token.Type;
import com.timepath.steam.io.util.VDFNode;
import java.io.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.tree.DefaultMutableTreeNode;

/**
 *
 * http://hpmod.googlecode.com/svn/trunk/tier1/KeyValues.cpp
 * http://hlssmod.net/he_code/public/tier1/KeyValues.h
 *
 * Standard KeyValues format loader
 *
 * @author timepath
 */
public class VDF implements Savable {

    private static final Logger LOG = Logger.getLogger(VDF.class.getName());

    protected VDFNode root;

    private Level logLevel = Level.FINER; // ALL == disabled

    public void setLogLevel(Level logLevel) {
        this.logLevel = logLevel;
    }

    public VDFNode getRoot() {
        return root;
    }

    public VDF() {
        root = new VDFNode("VDF");
    }

    // http://stackoverflow.com/questions/5695240/php-regex-to-ignore-escaped-quotes-within-quotes/5696141#5696141
    // OR together:
    // //(.*)
    // Group 1 will be a comment
    // ([^"\\]*(?:\\.[^"\\]*)*)
    // Group 2 will be the unquoted text
    // \[(!)?\$(.*)\]
    // Group 3 will be "!" if it is present
    // Group 4 will be the platform name
    // ([^\s{}"]+)
    // Group 5 will be a block of text
    // |(\{)|(\})
    // Group 6 will be found if indenting
    // Group 7 will be found if returning
    private static final Pattern regex = Pattern.compile(
            "//(.*)|\"([^\"\\\\]*(?:\\\\.[^\"\\\\]*)*)\"|\\[(!)?\\$(.*)\\]|([^\\s{}\"]+)|(\\{)|(\\})");

    static class Token {

        enum Type {

            COMMENT,
            TEXT,
            CONDITION,
            PLATFORM,
            IN,
            OUT

        }

        Type type;

        String val;

        String leading;

        int line;

        Token(Type t, String val, String leading, int line) {
            this.type = t;
            this.val = val;
            this.leading = leading;
            this.line = line;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(val.length());
            sb.append(val);
            return sb.toString();
        }

    }

    protected void processAnalyze(Scanner scanner, DefaultMutableTreeNode parent) {
        List<Token> matchList = new ArrayList<Token>();
        int line = 0;
        StringBuilder sb = new StringBuilder();
        while(scanner.hasNext()) {
            String str = scanner.nextLine();
            sb.append(str).append("\n");
            line++;
            if(str.length() == 0) {
                continue;
            }
        }
        String str = sb.toString();
        Matcher matcher = regex.matcher(str);
        String leading;
        int previous = 0;
        while(matcher.find()) {
            leading = str.substring(previous, matcher.start());
            previous = matcher.end();
            if(matcher.group(1) != null) {
                matchList.add(new Token(Type.COMMENT, matcher.group(1), leading, line));
            } else if(matcher.group(2) != null) {
                matchList.add(new Token(Type.TEXT, matcher.group(2), leading, line));
            } else if(matcher.group(3) != null) {
                matchList.add(new Token(Type.CONDITION, matcher.group(3), leading, line));
            } else if(matcher.group(4) != null) {
                matchList.add(new Token(Type.PLATFORM, matcher.group(4), leading, line));
            } else if(matcher.group(5) != null) {
                matchList.add(new Token(Type.TEXT, matcher.group(5), leading, line));
            } else if(matcher.group(6) != null) {
                matchList.add(new Token(Type.IN, matcher.group(6), leading, line));
            } else if(matcher.group(7) != null) {
                matchList.add(new Token(Type.OUT, matcher.group(7), leading, line));
            } else {
                LOG.log(Level.SEVERE, "Error parsing {0}", str);
            }
        }
        Token[] tokens = matchList.toArray(new Token[0]);
        LOG.log(logLevel, "{0}:{1}", new Object[] {tokens.length, Arrays.toString(tokens)});

        recurse(tokens, 0, (VDFNode) parent);
    }

    private int recurse(Token[] tokens, int offset, VDFNode parent) {
        VDFNode previous = null;
        for(int i = offset; i < tokens.length; i++) {
            Token token = tokens[i];
            LOG.log(logLevel, "i = {0}", i);
            switch(token.type) {
                case TEXT:
                    LOG.log(logLevel, token.val);
                    if(previous == null) {
                        previous = new VDFNode(token.val);
                        parent.add(previous);
                    } else {
                        if(previous.getValue() == null) {
                            previous.setValue(token.val);
                            parent.add(previous);
                            previous = null;
                        } else {
                            LOG.log(Level.WARNING, "Unhandled second var");
                        }
                    }
                    break;
                case IN:
                    LOG.log(logLevel, token.val);
                    i++;
                    LOG.log(logLevel, "Reading {0}", new Object[] {previous});
                    i = recurse(tokens, i, previous);
                    previous = null;
                    break;
                case OUT:
                    LOG.log(logLevel, token.val);
                    int read = i - offset;
                    LOG.log(logLevel, "Left {0} after reading {1}", new Object[] {parent, read});
                    return i;
                default:
                    LOG.log(logLevel, "Unhandled {0}", token);
                    break;
            }
        }
        return tokens.length;
    }

    @Override
    public void readExternal(InputStream in) {
        readExternal(in, "UTF-8");
    }

    public void readExternal(InputStream in, String encoding) {
        Scanner s = null;
        try {
            s = new Scanner(in, encoding);
            processAnalyze(s, root);
        } catch(StackOverflowError ex) {
            LOG.log(Level.WARNING, "Too deep (Stack overflowed)");
        } finally {
            if(s != null) {
                s.close();
            }
        }
    }

    @Override
    public void readExternal(ByteBuffer buf) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void writeExternal(OutputStream out) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public static boolean isBinary(File f) {
        try {
            RandomAccessFile rf = new RandomAccessFile(f, "r");
            rf.seek(rf.length() - 1);
            int r = rf.read();
            return (r == 0x00 || r == 0x08);
        } catch(FileNotFoundException ex) {
            Logger.getLogger(VDF.class.getName()).log(Level.SEVERE, null, ex);
        } catch(IOException ex) {
            Logger.getLogger(VDF.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }

}
