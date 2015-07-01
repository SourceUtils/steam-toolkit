package com.timepath.steam.io

import com.timepath.steam.io.storage.ACF
import org.junit.After
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.io.FileNotFoundException
import java.io.PrintStream
import java.nio.charset.Charset
import java.util.LinkedList

public class VDFTest {

    @Before
    public fun setUp() {
    }

    @After
    public fun tearDown() {
    }

    @Test
    public fun testLoad() {
        try {
            val max = 10
            val a = ACF.fromManifest(440)
            val original = System.err
            val warnings = LinkedList<String>()
            for (s in listOf(".res", ".vdf")) {
                for (f in a.find(s)) {
                    var flag = false
                    System.setErr(object : PrintStream(original) {
                        override fun write(b: ByteArray) {
                            super.write(b)
                            flag = true
                        }

                        override fun write(buf: ByteArray, off: Int, len: Int) {
                            super.write(buf, off, len)
                            flag = true
                        }

                        override fun write(b: Int) {
                            super.write(b)
                            flag = true
                        }
                    })
                    VDF.load(f.openStream()!!, Charset.defaultCharset())
                    if (flag) {
                        warnings.add(f.toString())
                        System.err.println("Warning from: ${f.path}${f.name}")
                    }
                    if (warnings.size() > max) {
                        fail("Too many warnings: $warnings")
                    }
                }
            }
        } catch(e: FileNotFoundException) {
        }
    }
}
