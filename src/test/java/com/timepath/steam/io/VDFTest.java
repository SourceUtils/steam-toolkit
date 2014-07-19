package com.timepath.steam.io;

import com.timepath.steam.io.storage.ACF;
import com.timepath.vfs.VFile;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.PrintStream;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.fail;

public class VDFTest {

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testLoad() throws Exception {
        int max = 10;
        try {
            ACF a = ACF.fromManifest(440);
            PrintStream original = System.err;
            List<String> warnings = new LinkedList<>();
            for(String s : new String[] { ".res", ".vdf" }) {
                for(VFile f : a.find(s)) {
                    final boolean[] flag = new boolean[1];
                    System.setErr(new PrintStream(original) {
                        @Override
                        public void write(final byte[] b) throws IOException {
                            super.write(b);
                            flag[0] = true;
                        }

                        @Override
                        public void write(final byte[] buf, final int off, final int len) {
                            super.write(buf, off, len);
                            flag[0] = true;
                        }

                        @Override
                        public void write(final int b) {
                            super.write(b);
                            flag[0] = true;
                        }
                    });
                    VDF.load(f.openStream());
                    if(flag[0]) {
                        warnings.add(f.toString());
                        System.err.println("Warning from: " + f.getPath() + f.getName());
                    }
                    if(warnings.size() > max) {
                        fail("Too many warnings: " + warnings);
                    }
                }
            }
        } catch(IOException ignored) {
        }
    }
}