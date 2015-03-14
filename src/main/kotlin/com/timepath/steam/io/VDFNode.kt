package com.timepath.steam.io

import com.timepath.Diff
import com.timepath.Node
import com.timepath.Pair
import com.timepath.steam.io.VDFNode.VDFProperty
import com.timepath.steam.io.VDFParser.NodeContext
import com.timepath.steam.io.VDFParser.PairContext
import org.antlr.v4.runtime.ANTLRInputStream
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.tree.ParseTreeWalker

import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.Charset
import java.util.Comparator
import java.util.LinkedList
import java.util.logging.Logger
import java.util.Arrays

/**
 * Standard KeyValues format loader
 *
 * @author TimePath
 */

throws(javaClass<IOException>())
public fun VDFNode(`is`: InputStream, c: Charset, self: VDFNode = VDFNode()): VDFNode = with(self) {
    val lexer = VDFLexer(ANTLRInputStream(InputStreamReader(`is`, c)))
    val tokens = CommonTokenStream(lexer)
    val parser = VDFParser(tokens)
    val stack = LinkedList<VDFNode>()
    stack.push(this)
    ParseTreeWalker.DEFAULT.walk(object : VDFBaseListener() {
        override fun enterNode(ctx: NodeContext) {
            val conditional = if (ctx.conditional != null) ctx.conditional.getText() else null
            stack.push(VDFNode(u(ctx.name.getText())))
            stack.peek().conditional = conditional
        }

        override fun exitNode(ctx: NodeContext) {
            val current = stack.pop()
            stack.peek().addNode(current)
        }

        override fun exitPair(ctx: PairContext) {
            val conditional = when {
                ctx.conditional != null -> ctx.conditional.getText()
                else -> null
            }
            stack.peek().addProperty(VDFProperty(u(ctx.key.getText()), u(ctx.value.getText()), conditional))
        }

        private fun u(s: String) = when {
            s.startsWith("\"") -> s.substring(1, s.length() - 1).replace("\\\"", "\"")
            else -> s
        }
    }, parser.parse())
    this
}

public open class VDFNode(name: Any = "VDF") : Node<VDFProperty, VDFNode>(name) {
    public var conditional: String? = null

    override fun toString(): String {
        return super.toString() + (if (conditional == null) "" else "    " + conditional)
    }

    public fun rdiff2(other: VDFNode): Diff<VDFNode> {
        val d = Diff<VDFNode>()
        d.`in` = this
        d.out = other
        val removed = VDFNode("Removed")
        val added = VDFNode("Added")
        val potential = VDFNode("Potential")
        val potential2 = VDFNode("Potential2")
        val same = VDFNode("Same")
        for (v in getNodes()) {
            val match = other[v.getCustom()]
            if (match == null) {
                // Not in new copy
                removed.addNode(v)
            } else {
                potential.addNode(match)
            }
        }
        for (v in other.getNodes()) {
            val match = get(v.getCustom())
            if (match == null) {
                // Not in this copy
                added.addNode(v)
            } else {
                potential2.addNode(match)
            }
        }
        for (v in potential.getNodes()) {
            val v2 = potential2[v.getCustom()]
            val diff = v2!!.diff(v) // FIXME: backwards for some reason
            if ((diff.added.size() + diff.removed.size() + diff.modified.size()) > 0) {
                // Something was changed
                val na = VDFNode(v.getCustom())
                val nr = VDFNode(v.getCustom())
                for (a in diff.added) {
                    na.addProperty(a)
                }
                added.addNode(na)
                for (r in diff.removed) {
                    nr.addProperty(r)
                }
                removed.addNode(nr)
                for (p in diff.modified) {
                    // TODO: push to modified
                    nr.addProperty(p.getKey())
                    na.addProperty(p.getValue())
                }
                // This could be a mixture of additions, removals or modifications, as well as unchanged values
            } else {
                same.addNode(v)
            }
        }
        Node.debug<VDFProperty, VDFNode>(d.`in`, d.out, removed, added)
        return d
    }

    /**
     * Diffs the properties of both VDF nodes
     *
     * @param other The other node
     * @return
     */
    fun diff(other: VDFNode): Diff<VDFProperty> {
        return Diff.diff<VDFProperty>(getProperties(), other.getProperties(), COMPARATOR_KEY, COMPARATOR_VALUE)
    }

    public fun save(): String {
        val sb = StringBuilder()
        // preceding header
        for (p in properties) {
            if (p.getValue().toString().isEmpty()) {
                if ("\\n" == p.getKey()) {
                    sb.append('\n')
                }
                if ("//" == p.getKey()) {
                    sb.append("//").append(p.getInfo()).append('\n')
                }
            }
        }
        sb.append(custom).append('\n')
        sb.append("{\n")
        for (p in properties) {
            if (!p.getValue().toString().isEmpty()) {
                if ("\\n" == p.getKey()) {
                    sb.append("\t    \n")
                } else {
                    sb.append("\t    ").append(p.getKey()).append("\t    ").append(p.getValue())
                    if (p.getInfo() != null) sb.append(' ').append(p.getInfo())
                    sb.append('\n')
                }
            }
        }
        sb.append("}\n")
        return sb.toString()
    }

    /**
     * Diffs the child nodes of both VDF nodes. TODO: breadth first would be more efficient with large differences
     *
     * @param other The other node
     * @return
     */
    override fun rdiff(other: VDFNode): Diff<VDFNode> {
        val d = Diff<VDFNode>()
        d.`in` = this
        d.out = other
        val removed = VDFNode("Removed")
        val added = VDFNode("Added")
        val same = VDFNode("Same")
        val modified = VDFNode("Modified")
        for (v in getNodes()) {
            val match = other[v.getCustom()]
            if (match == null) {
                // Not in new copy
                removed.addNode(v)
            } else {
                same.addNode(match) // TODO: check for differences
            }
        }
        for (v in other.getNodes()) {
            val match = get(v.getCustom())
            if (match == null) {
                // Not in this copy
                added.addNode(v)
            }
            //else {
            //    Diff<VDFProperty> diff = v.diff(same.get(v.custom));
            //    if(diff.added.size() + diff.removed.size() + diff.modified.size() >= 0) { // Something was changed
            //        // This could be a mixture of additions, removals or modifications, as well as unchanged values
            //    }
            //}
        }
        d.removed = Arrays.asList<VDFNode>(removed)
        d.same = Arrays.asList<VDFNode>(same)
        d.added = Arrays.asList<VDFNode>(added)
//        d.modified = Arrays.asList(modified.getChildren())
        return d
    }

    public class VDFProperty(key: String, `val`: Any, public var conditional: String? = null) : Pair<String, Any>(key, `val`) {

        override fun toString() = "'${getKey()}'${TAB}'${getValue()}'${if (conditional == null) "" else TAB + conditional}"

        public fun getInfo(): String = ""

        class object {

            private val LOG = Logger.getLogger(javaClass<VDFProperty>().getName())
            private val TAB = "    "
        }
    }

    class object {

        private val COMPARATOR_KEY = object : Comparator<VDFProperty> {
            override fun compare(o1: VDFProperty, o2: VDFProperty): Int {
                return o1.getKey().compareTo(o2.getKey())
            }
        }
        private val COMPARATOR_VALUE = object : Comparator<VDFProperty> {
            override fun compare(o1: VDFProperty, o2: VDFProperty): Int {
                return o1.getValue().hashCode() - o2.getValue().hashCode()
            }
        }
        private val LOG = Logger.getLogger(javaClass<VDFNode>().getName())
    }
}
