package com.timepath.steam.io.storage;

import com.timepath.steam.io.util.ExtendedVFile;
import com.timepath.util.concurrent.DaemonThreadFactory;
import com.timepath.vfs.SimpleVFile;
import sun.java2d.pipe.SpanShapeRenderer;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author TimePath
 */
public class Files extends ExtendedVFile {

    private static final Logger LOG = Logger.getLogger(Files.class.getName());
    private static final ExecutorService pool = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors() * 10,
            new DaemonThreadFactory()
    );
    protected static List<FileHandler> handlers = new LinkedList<>();

    static {
        try {
            Class.forName(VPK.class.getName());
        } catch (ClassNotFoundException e) {
            LOG.log(Level.SEVERE, null, e);
        }
    }

    /**
     * Archive to directory map
     */
    private final Map<Collection<? extends SimpleVFile>, SimpleVFile> archives = new HashMap<>();
    private final File file;

    public Files(File f) {
        this(f, f.isDirectory());
    }

    /**
     * Reserved for internal use to prevent starting more searches
     *
     * @param f
     * @param recursive
     */
    private Files(File f, boolean recursive) {
        file = f;
        if (recursive) {
            insert(f);
        }
    }

    /**
     * Adds one file to another, merging any directories.
     * TODO: additive/union directories to avoid this kludge
     *
     * @param src
     * @param parent
     */
    private static void merge(SimpleVFile src, SimpleVFile parent) {
        SimpleVFile existing = parent.get(src.getName());
        if (existing == null) {
            // Parent does not have this file, simple case
            parent.add(src);
        } else {
            // Add all child files, silently ignore duplicates
            Collection<? extends SimpleVFile> children = src.list();
            for (SimpleVFile f : children.toArray(new SimpleVFile[children.size()])) { // Defensive copy
                merge(f, existing);
            }
        }
    }

    public static void registerHandler(FileHandler h) {
        handlers.add(h);
    }

    @Override
    public Object getAttributes() {
        return null;
    }

    @Override
    public ExtendedVFile getRoot() {
        return this;
    }

    @Override
    public boolean isComplete() {
        return true;
    }

    @Override
    public String getName() {
        return file.getName();
    }

    @Override
    public InputStream openStream() {
        try {
            return new BufferedInputStream(new FileInputStream(file));
        } catch (FileNotFoundException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
        return null;
    }

    @Override
    public boolean isDirectory() {
        return file.isDirectory();
    }

    @Override
    public long lastModified() {
        return file.lastModified();
    }

    @Override
    public long length() {
        return file.length();
    }

    /**
     * Insert children of a directory
     *
     * @param f the directory
     */
    private void insert(File f) {
        long start = System.currentTimeMillis();
        final Collection<Future> tasks = new LinkedList<>();
        visit(f, new FileVisitor() {
            @Override
            public void visit(final File file, final Files parent) {
                Files entry = new Files(file, false);
                parent.add(entry);
                if (file.isDirectory()) {
                    entry.visit(file, this); // Depth first search
                    return;
                }
                // File identification
                tasks.add(pool.submit(new Callable<Void>() {
                    @Override
                    public Void call() throws Exception {
                        for (FileHandler h : handlers) {
                            Collection<? extends SimpleVFile> root = h.handle(file);
                            if (root == null) continue;
                            archives.put(root, parent);
                        }
                        return null;
                    }
                }));
            }
        });
        for (Future fut : tasks) {
            try {
                fut.get();
            } catch (InterruptedException | ExecutionException ex) {
                LOG.log(Level.SEVERE, null, ex);
            }
        }
        LOG.log(Level.INFO, "Recursive file load took {0}ms", System.currentTimeMillis() - start);
        for (Map.Entry<Collection<? extends SimpleVFile>, SimpleVFile> e : archives.entrySet()) {
            Collection<? extends SimpleVFile> files = e.getKey();
            SimpleVFile directory = e.getValue();
            for (SimpleVFile file : files.toArray(new SimpleVFile[files.size()])) { // Defensive copy
                merge(file, directory);
            }
        }
    }

    private void visit(File dir, FileVisitor v) {
        File[] ls = dir.listFiles();
        if (ls == null) {
            return;
        }
        for (File f : ls) {
            v.visit(f, this);
        }
    }

    public static interface FileHandler {

        Collection<? extends SimpleVFile> handle(File file) throws IOException;
    }

    private interface FileVisitor {

        void visit(File f, Files parent);
    }
}
