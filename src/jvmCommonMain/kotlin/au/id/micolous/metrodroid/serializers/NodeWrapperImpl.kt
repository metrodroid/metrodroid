package au.id.micolous.metrodroid.serializers

import au.id.micolous.metrodroid.card.Card
import org.w3c.dom.Node
import java.io.ByteArrayInputStream
import javax.xml.parsers.DocumentBuilderFactory

class NodeWrapperImpl(private val node: Node): NodeWrapper {
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
    override val inner: String?
        get() = node.nodeValue ?: node.textContent

    companion object {
        @OptIn(ExperimentalStdlibApi::class)
        fun read(contents: String): NodeWrapper {
            val dbFactory = DocumentBuilderFactory.newInstance()
            val dBuilder = dbFactory.newDocumentBuilder()
            val doc = dBuilder.parse(
                ByteArrayInputStream(
                    filterBadXMLChars(contents).encodeToByteArray())
            )
            return NodeWrapperImpl(doc.documentElement)
        }
    }
}

fun readCardXML(contents: String): Card = readCardXML(NodeWrapperImpl.read(contents))
