package com.timepath.steam.io.storage;

import com.timepath.steam.io.util.ExtendedVFile;
import com.timepath.util.concurrent.DaemonThreadFactory;
import com.timepath.vfs.SimpleVFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
    @NotNull
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
    @NotNull
    private final File file;

    public Files(@NotNull File f) {
        this(f, f.isDirectory());
    }

    /**
     * Reserved for internal use to prevent starting more searches
     *
     * @param f
     * @param recursive
     */
    private Files(@NotNull File f, boolean recursive) {
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
    private static void merge(@NotNull SimpleVFile src, @NotNull SimpleVFile parent) {
        @Nullable SimpleVFile existing = parent.get(src.getName());
        if (existing == null) {
            // Parent does not have this file, simple case
            parent.add(src);
        } else {
            // Add all child files, silently ignore duplicates
            @Nullable Collection<? extends SimpleVFile> children = src.list();
            for (@NotNull SimpleVFile f : children.toArray(new SimpleVFile[children.size()])) { // Defensive copy
                merge(f, existing);
            }
        }
    }

    public static void registerHandler(FileHandler h) {
        handlers.add(h);
    }

    @Nullable
    @Override
    public Object getAttributes() {
        return null;
    }

    @NotNull
    @Override
    public ExtendedVFile getRoot() {
        return this;
    }

    @Override
    public boolean isComplete() {
        return true;
    }

    @NotNull
    @Override
    public String getName() {
        return file.getName();
    }

    @Nullable
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
    private void insert(@NotNull File f) {
        long start = System.currentTimeMillis();
        @NotNull final Collection<Future> tasks = new LinkedList<>();
        visit(f, new FileVisitor() {
            @Override
            public void visit(@NotNull final File file, @NotNull final Files parent) {
                @NotNull Files entry = new Files(file, false);
                parent.add(entry);
                if (file.isDirectory()) {
                    entry.visit(file, this); // Depth first search
                    return;
                }
                // File identification
                tasks.add(pool.submit(new Callable<Void>() {
                    @Nullable
                    @Override
                    public Void call() throws Exception {
                        for (@NotNull FileHandler h : handlers) {
                            @Nullable Collection<? extends SimpleVFile> root = h.handle(file);
                            if (root == null) continue;
                            archives.put(root, parent);
                        }
                        return null;
                    }
                }));
            }
        });
        for (@NotNull Future fut : tasks) {
            try {
                fut.get();
            } catch (@NotNull InterruptedException | ExecutionException ex) {
                LOG.log(Level.SEVERE, null, ex);
            }
        }
        LOG.log(Level.INFO, "Recursive file load took {0}ms", System.currentTimeMillis() - start);
        for (@NotNull Map.Entry<Collection<? extends SimpleVFile>, SimpleVFile> e : archives.entrySet()) {
            Collection<? extends SimpleVFile> files = e.getKey();
            SimpleVFile directory = e.getValue();
            for (@NotNull SimpleVFile file : files.toArray(new SimpleVFile[files.size()])) { // Defensive copy
                merge(file, directory);
            }
        }
    }

    private void visit(@NotNull File dir, @NotNull FileVisitor v) {
        @Nullable File[] ls = dir.listFiles();
        if (ls == null) {
            return;
        }
        for (File f : ls) {
            v.visit(f, this);
        }
    }

    public static interface FileHandler {

        @Nullable
        Collection<? extends SimpleVFile> handle(File file) throws IOException;
    }

    private interface FileVisitor {

        void visit(File f, Files parent);
    }
}
