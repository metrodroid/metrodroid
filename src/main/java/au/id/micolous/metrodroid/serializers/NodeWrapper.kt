package au.id.micolous.metrodroid.serializers

import kotlinx.io.ByteArrayInputStream
import kotlinx.io.InputStream
import kotlinx.io.charsets.Charsets
import kotlinx.serialization.toUtf8Bytes
import org.w3c.dom.Node
import javax.xml.parsers.DocumentBuilderFactory
import org.apache.commons.io.IOUtils

actual class NodeWrapper(val node: Node) {
    actual val childNodes: List<NodeWrapper>
        get() {
            val children = node.childNodes ?: return emptyList()
            return (0 until children.length).map { NodeWrapper(children.item(it)) }
        }
    actual val nodeName: String
        get() = node.nodeName
    actual val attributes: Map<String, String>
        get() {
            val attr = node.attributes ?: return emptyMap()
            return (0 until attr.length).map { attr.item(it).nodeName to attr.item(it).nodeValue }.toMap()
        }
    actual val nodeValue: String?
        get() = node.nodeValue
    actual val textContent: String?
        get() = node.textContent

    actual companion object {
        actual fun read(stream: InputStream): NodeWrapper {
            val dbFactory = DocumentBuilderFactory.newInstance()
            val dBuilder = dbFactory.newDocumentBuilder()
            val doc = dBuilder.parse(ByteArrayInputStream(
                    filterBadXMLChars(IOUtils.toString(stream, Charsets.UTF_8)).toUtf8Bytes()))
            return NodeWrapper(doc.documentElement)
        }
    }
}