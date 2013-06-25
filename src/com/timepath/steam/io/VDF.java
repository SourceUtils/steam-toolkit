package com.timepath.steam.io;

import com.timepath.io.utils.Savable;
import com.timepath.steam.io.util.VDFNode;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
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
    private static final Pattern quoteRegex = Pattern.compile("[^\\s\"']+|\"([^\"]*)\"");
    private static final Pattern platformRegex = Pattern.compile("(?:\\[(!)?\\$)(.*)(?:\\])");

    protected void processAnalyze(Scanner scanner, DefaultMutableTreeNode parent, int lineNum) {
        while(scanner.hasNext()) {
            String line = scanner.nextLine().trim();
            lineNum++;
            if(line.length() == 0) {
                continue;
            }
            String comment = null;
            int cIndex = line.indexOf("//");
            if(cIndex != -1) {
                comment = line.substring(cIndex);
                line = line.substring(0, cIndex);
            }

            List<String> matchList = new ArrayList<String>();
            Matcher regexMatcher = quoteRegex.matcher(line);
            while(regexMatcher.find()) {
                if(regexMatcher.group(1) != null) {
                    // Add double-quoted string without the quotes
                    matchList.add(regexMatcher.group(1));
                } else {
                    // Add unquoted word
                    matchList.add(regexMatcher.group());
                }
            }
            if(matchList.isEmpty()) {
                if(comment != null && comment.trim().length() > 0) {
                    LOG.log(logLevel, "Carrying extra: [{0}]", comment);
                    parent.add(new DefaultMutableTreeNode(comment));
                }
                continue;
            }
            String[] args = matchList.toArray(new String[0]);
            LOG.log(logLevel, "{0}:{1}", new Object[] {args.length, Arrays.toString(args)});

            String val = null;
            if(args.length >= 2) {
                val = args[1];
                Matcher plafMatcher = platformRegex.matcher(args[args.length - 1]);
                if(plafMatcher.find()) {
                    boolean bool = plafMatcher.group(1) == null; // true if "!" is present
                    String platform = plafMatcher.group(2);
                    if(args[args.length - 1] == val) { // yes, this is supposed to be a direct check
                        val = null;
                    }
                } else if(args.length > 2) {
                    LOG.log(Level.WARNING, "More than 2 args on line {0}({1}): {2}", new Object[] {lineNum, line, Arrays.toString(args)});
                }
            }

            if(args[0].equals("{")) { // just a { on its own line
                continue;
            }

            if(args[0].equals("}")) { // for returning out of recursion: analyze: processAnalyze > processAnalyze < break < break
                Object obj = parent.getUserObject();
//                if(obj instanceof Element) {
//                    ((Element)e).validate(); // TODO: Thread safety. oops
//                }
                LOG.log(logLevel, "Leaving {0}", obj);
                break; // TODO: /tf/scripts/HudAnimations_tf.txt
            } else if(val == null) { // very good assumption
                val = "{";
            }

            VDFNode p = new VDFNode(args[0], val);
//            p.setFile(file);
            parent.add(p);
            if(val.equals("{")) { // make new sub
                LOG.log(logLevel, "Stepping into {0}", p);
                processAnalyze(scanner, p, lineNum);
            }
        }
    }

    @Override
    public void readExternal(InputStream in) {
        readExternal(in, "UTF-8");
    }

    public void readExternal(InputStream in, String encoding) {
        Scanner s = null;
        try {
            s = new Scanner(in, encoding);
            processAnalyze(s, root, 0);
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
