package com.timepath.steam.io.storage;

import com.timepath.steam.io.util.ExtendedVFile;
import com.timepath.vfs.SimpleVFile;

import java.io.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author TimePath
 */
public class Files extends ExtendedVFile {

    private static final Logger                            LOG      = Logger.getLogger(Files.class.getName());
    private static final ExecutorService                   pool     = Executors.newFixedThreadPool(
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
    /**
     * Archive to directory map
     */
    private final        Map<ExtendedVFile, ExtendedVFile> archives = new HashMap<>();
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
        for(SimpleVFile d : r.children()) {
            SimpleVFile existing = null;
            for(SimpleVFile t : parent.children()) {
                if(t.getName().equals(d.getName())) {
                    existing = t;
                    break;
                }
            }
            if(existing == null) {
                parent.copy(d);
            } else {
                merge(d, existing);
            }
        }
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
    public InputStream stream() {
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
                final String name = file.getName();
                tasks.add(pool.submit(new Callable<Void>() {
                    @Override
                    public Void call() throws Exception {
                        if(name.endsWith("_dir.vpk")) {
                            VPK v = VPK.loadArchive(file);
                            archives.put(v.getRoot(), parent);
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
        for(Map.Entry<ExtendedVFile, ExtendedVFile> e : archives.entrySet()) {
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

    private interface FileVisitor {

        void visit(File f, Files parent);
    }
}
