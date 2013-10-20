package com.timepath.steam.io;

import com.timepath.io.utils.Savable;
import com.timepath.steam.io.VDF.VDFToken.Type;
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
    // Group 3 will be the conditional
    // ([^\s{}"]+)
    // Group 4 will be a block of text
    // |(\{)|(\})
    // Group 5 will be found if indenting
    // Group 6 will be found if returning
    private static final Pattern regex = Pattern.compile(
            "//(.*)|\"([^\"\\\\]*(?:\\\\.[^\"\\\\]*)*)\"|\\[(.*\\$.*)\\]|([^\\s{}\"]+)|(\\{)|(\\})");

    public static class VDFToken {

        /**
         * @return the value
         */
        public String getValue() {
            return value;
        }

        /**
         * @param val the value to set
         */
        public void setValue(String val) {
            this.value = val;
        }

        /**
         * @return the type
         */
        public Type getType() {
            return type;
        }

        /**
         * @param type the type to set
         */
        public void setType(Type type) {
            this.type = type;
        }

        public enum Type {

            COMMENT,
            QUOTED,
            TEXT,
            CONDITION,
            IN,
            OUT

        }

        private Type type;

        private String value;

        private String leading;

        private int line;

        public VDFToken(String value) {
            this(Type.QUOTED, value, null, -1);
        }

        VDFToken(Type t, String value, String leading, int line) {
            this.type = t;
            this.value = value;
            this.leading = leading;
            this.line = line;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(leading);
            if(getType() == Type.COMMENT) {
                sb.append("//");
            }
            if(getType() == Type.QUOTED) {
                sb.append("\"");
            } else if(getType() == Type.CONDITION) {
                sb.append("[");
            }
            sb.append(getValue());
            if(getType() == Type.QUOTED) {
                sb.append("\"");
            } else if(getType() == Type.CONDITION) {
                sb.append("]");
            }
            return sb.toString();
        }

    }
    
    List<VDFToken> tokens = new ArrayList<VDFToken>();

    protected void processAnalyze(Scanner scanner, DefaultMutableTreeNode parent) {
        List<Integer> lineEnds = new ArrayList<Integer>();
        lineEnds.add(0);

        StringBuilder sb = new StringBuilder();
        while(scanner.hasNext()) {
            String str = scanner.nextLine();
            int cumulative = lineEnds.get(lineEnds.size() - 1);
            lineEnds.add(cumulative + str.length() + 1); // +1 = \n. TODO: \r
            sb.append(str).append("\n");
        }
        String str = sb.toString();

        Matcher matcher = regex.matcher(str);
        String leading;
        int previous = 0;
        while(matcher.find()) {
            int globalIndex = matcher.start();
            int lineIndex = 1;
            while(lineIndex < lineEnds.size()) {
                if(globalIndex < lineEnds.get(lineIndex)) {
                    lineIndex--;
                    LOG.log(Level.FINER, "{0} <= {1}, therefore {2}", new Object[] {globalIndex,
                                                                                    lineEnds.get(
                        lineIndex),
                                                                                    lineIndex});
                    break;
                }
                lineIndex++;
            }
            leading = str.substring(previous, matcher.start());
            previous = matcher.end();
            if(matcher.group(1) != null) {
                tokens.add(new VDFToken(Type.COMMENT, matcher.group(1), leading, lineIndex));
            } else if(matcher.group(2) != null) {
                tokens.add(new VDFToken(Type.QUOTED, matcher.group(2),//.replace("\n", "\\\\n"),
                                           leading, lineIndex));
            } else if(matcher.group(3) != null) { // TODO: fit this into the regex
                String cond = matcher.group(3);
                tokens.add(new VDFToken(Type.CONDITION, cond, leading, lineIndex));
            } else if(matcher.group(4) != null) {
                tokens.add(new VDFToken(Type.TEXT, matcher.group(4),//.replace("\n", "\\\\n"),
                                           leading, lineIndex));
            } else if(matcher.group(5) != null) {
                tokens.add(new VDFToken(Type.IN, matcher.group(5), leading, lineIndex));
            } else if(matcher.group(6) != null) {
                tokens.add(new VDFToken(Type.OUT, matcher.group(6), leading, lineIndex));
            } else {
                LOG.log(Level.SEVERE, "Error parsing {0}", str);
            }
        }
        VDFToken[] tokens = this.tokens.toArray(new VDFToken[0]);
        LOG.log(logLevel, "{0}:{1}", new Object[] {tokens.length, Arrays.toString(tokens)});

        recurse(tokens, 0, (VDFNode) parent);
    }

    private int recurse(VDFToken[] tokens, int offset, VDFNode parent) {
        VDFNode previous = null;
        for(int i = offset; i < tokens.length; i++) {
            VDFToken token = tokens[i];
            LOG.log(logLevel, "i = {0}", i);
            switch(token.getType()) {
                case QUOTED:
                case TEXT:
                    LOG.log(logLevel, token.getValue());
                    if(previous == null) {
                        previous = new VDFNode(token);
                        parent.add(previous);
                    } else {
                        if(previous.getValue() == null) {
                            previous.setValue(token);
                            parent.add(previous);
                            previous = null;
                        } else {
                            LOG.log(Level.WARNING, "Unhandled second var");
                        }
                    }
                    break;
                case IN:
                    LOG.log(logLevel, token.getValue());
                    i++;
                    LOG.log(logLevel, "Reading {0}", new Object[] {previous});
                    i = recurse(tokens, i, previous);
                    previous = null;
                    break;
                case OUT:
                    LOG.log(logLevel, token.getValue());
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
    
    public String save() {
        StringBuilder sb = new StringBuilder();
        for(VDFToken t : tokens) {
            sb.append(t.toString());
        }
        return sb.toString();
    }

}
