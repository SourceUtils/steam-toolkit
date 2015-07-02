package com.timepath.steam.io.bvdf

import com.timepath.io.ByteBufferInputStream
import com.timepath.io.utils.Savable
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.text.MessageFormat
import java.util.Arrays
import java.util.logging.Level
import java.util.logging.Logger
import javax.swing.tree.DefaultMutableTreeNode

/**
 * Creates a swing tree from a binary VDF file
 */
public class BVDF : Savable {
    public val root: DataNode

    init {
        root = DataNode("BVDF")
    }

    override fun readExternal(buf: ByteBuffer) {
        readExternal(ByteBufferInputStream(buf))
    }

    override fun readExternal(`in`: InputStream) {
        try {
            BVDFStream(`in`, object : BVDFListener {
                private var last = root

                override fun emit(key: String, value: Any) {
                    last.add(DataNode(key, value))
                }

                override fun pop() {
                    last = last.getParent() as DataNode
                }

                override fun push(section: Any) {
                    val node = DataNode(section)
                    last.add(node)
                    last = node
                }
            }).read()
        } catch (ex: IOException) {
            LOG.log(Level.SEVERE, null, ex)
        }

    }

    /**
     * Not supported yet
     */
    override fun writeExternal(out: OutputStream) {
        throw UnsupportedOperationException("Not supported yet.")
    }

    public class DataNode : DefaultMutableTreeNode {

        public var name: String? = null
        public var value: Any? = null

        constructor(obj: Any) {
            name = obj.toString()
        }

        constructor(name: String, obj: Any) {
            this.name = name
            value = obj
        }

        public fun get(key: String): DataNode? {
            for (o in children) {
                if (o !is DataNode) {
                    continue
                }
                if (o.name == key) {
                    return o
                }
            }
            return null
        }

        override fun toString(): String {
            var nameStr = ""
            name?.let {
                nameStr = it
            }
            var splitStr = ""
            if ((name != null) && (value != null)) {
                splitStr = ": "
            }
            var valStr = ""
            if (value != null) {
                valStr = value!!.toString()
                if (value is ByteArray) {
                    valStr = Arrays.toString(value as ByteArray?)
                }
                valStr += " [" + value!!.javaClass.getSimpleName() + ']'
            }
            return MessageFormat.format("{0}{1}{2}", nameStr, splitStr, valStr)
        }
    }

    companion object {

        private val LOG = Logger.getLogger(javaClass<BVDF>().getName())
    }
}
