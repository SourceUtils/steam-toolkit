package com.timepath.steam.io.storage;

import com.timepath.steam.io.util.ExtendedVFile;
import com.timepath.vfs.SimpleVFile;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author TimePath
 */
public class Files extends ExtendedVFile {

    private static final Logger            LOG      = Logger.getLogger(Files.class.getName());
    private static final ExecutorService   pool     = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors() * 10,
            new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    Thread t = Executors.defaultThreadFactory()
                                        .newThread(r);
                    t.setDaemon(true);
                    return t;
                }
            }
                                                                                  );
    protected static     List<FileHandler> handlers = new LinkedList<>();

    static {
        try {
            Class.forName(VPK.class.getName());
        } catch(ClassNotFoundException e) {
            LOG.log(Level.SEVERE, null, e);
        }
    }

    /**
     * Archive to directory map
     */
    private final Map<SimpleVFile, SimpleVFile> archives = new HashMap<>();
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
        if(recursive) {
            insert(f);
        }
    }

    private static void merge(SimpleVFile r, SimpleVFile parent) {
        if(parent.get(r.getName()) == null) {
            parent.add(r);
            return;
        }
        for(SimpleVFile d : r.list()) {
            SimpleVFile existing = null;
            for(SimpleVFile t : parent.list()) {
                if(t.getName().equals(d.getName())) {
                    existing = t;
                    break;
                }
            }
            if(existing == null) {
                parent.add(d);
            } else {
                merge(d, existing);
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
        } catch(FileNotFoundException ex) {
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

    private void insert(File f) {
        long start = System.currentTimeMillis();
        final Collection<Future> tasks = new LinkedList<>();
        visit(f, new FileVisitor() {
            @Override
            public void visit(final File file, final Files parent) {
                Files e = new Files(file, false);
                parent.add(e);
                if(file.isDirectory()) {
                    e.visit(file, this);
                    return;
                }
                tasks.add(pool.submit(new Callable<Void>() {
                    @Override
                    public Void call() throws Exception {
                        for(FileHandler h : handlers) {
                            SimpleVFile root = h.handle(file);
                            if(root == null) continue;
                            archives.put(root, parent);
                        }
                        return null;
                    }
                }));
            }
        });
        for(Future fut : tasks) {
            try {
                fut.get();
            } catch(InterruptedException | ExecutionException ex) {
                LOG.log(Level.SEVERE, null, ex);
            }
        }
        LOG.log(Level.INFO, "Recursive file load took {0}ms", System.currentTimeMillis() - start);
        // TODO: additive directories to avoid this kludge
        for(Map.Entry<SimpleVFile, SimpleVFile> e : archives.entrySet()) {
            merge(e.getKey(), e.getValue());
        }
    }

    private void visit(File dir, FileVisitor v) {
        File[] ls = dir.listFiles();
        if(ls == null) {
            return;
        }
        for(File f : ls) {
            v.visit(f, this);
        }
    }

    public static interface FileHandler {

        SimpleVFile handle(File file) throws IOException;
    }

    private interface FileVisitor {

        void visit(File f, Files parent);
    }
}
