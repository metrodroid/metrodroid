package au.id.micolous.metrodroid.serializers

import kotlinx.io.ByteArrayInputStream
import kotlinx.io.InputStream
import kotlinx.io.charsets.Charsets
import kotlinx.serialization.toUtf8Bytes
import org.w3c.dom.Node
import javax.xml.parsers.DocumentBuilderFactory
import au.id.micolous.metrodroid.card.Card

class NodeWrapperImpl(val node: Node): NodeWrapper {
    override val childNodes: List<NodeWrapper>
        get() {
            val children = node.childNodes ?: return emptyList()
            return (0 until children.length).map { NodeWrapperImpl(children.item(it)) }
        }
    override val nodeName: String
        get() = node.nodeName
    override val attributes: Map<String, String>
        get() {
            val attr = node.attributes ?: return emptyMap()
            return (0 until attr.length).map { attr.item(it).nodeName to attr.item(it).nodeValue }.toMap()
        }
    override val nodeValue: String?
        get() = node.nodeValue
    override val textContent: String?
        get() = node.textContent

    companion object {
        fun read(stream: InputStream): NodeWrapper {
            val dbFactory = DocumentBuilderFactory.newInstance()
            val dBuilder = dbFactory.newDocumentBuilder()
            val doc = dBuilder.parse(ByteArrayInputStream(
                    filterBadXMLChars(stream.bufferedReader().readText()).toUtf8Bytes()))
            return NodeWrapperImpl(doc.documentElement)
        }
    }
}

fun readCardXML(reader: InputStream): Card = readCardXML(NodeWrapperImpl.read(reader))
