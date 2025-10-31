/*
 * XmlFormat.kt
 *
 * Copyright 2019, 2021 Google
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package au.id.micolous.metrodroid.serializers

import au.id.micolous.metrodroid.card.Card
import au.id.micolous.metrodroid.multi.logAndSwiftWrap
import au.id.micolous.metrodroid.util.AtomicRef
import au.id.micolous.metrodroid.util.ImmutableByteArray
import au.id.micolous.metrodroid.util.toNSData
import kotlinx.cinterop.*
import platform.Foundation.*
import platform.darwin.NSInteger
import platform.darwin.NSObject
import platform.darwin.NSUInteger
import platform.darwin.NSUIntegerVar
import platform.posix.memcpy
import platform.posix.uint8_tVar

object XmlFormat {
    class XMLAST(override val nodeName: String,
                 override val attributes: Map<String, String>,
                 val parent: XMLAST?) : NodeWrapper {
        override var childNodes = mutableListOf<XMLAST>()
        override var inner: String? = ""

        fun addChild(child: XMLAST) {
            childNodes.add(child)
        }

        fun addInner(s: String) {
            inner = (inner ?: "") + s
        }
    }

    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
    class XMLDelegate(val callback: (card: Card) -> Unit) :
            NSObject(), NSXMLParserDelegateProtocol {
        private var isFirst = true
        private var cardDepth = 0
        private var depth = 0
        private var valid = true
        private var astRoot: XMLAST? = null
        private var curElement: XMLAST? = null
        override fun parser(parser: NSXMLParser,
                            didStartElement: String, namespaceURI: String?,
                            qualifiedName: String?, attributes: Map<Any?, *>) {
            if (isFirst) {
                when (didStartElement) {
                    "cards" -> {
                        cardDepth = 2
                    }
                    "card" -> {
                        cardDepth = 1
                    }
                    else -> {
                        valid = false
                        parser.abortParsing()
                    }
                }
            }
            isFirst = false
            depth += 1
            val attrDict: Map<String, String> by lazy {
                attributes.map { (k, v) -> k as String to v as String }.toMap()
            }
            if (depth == cardDepth) {
                astRoot = XMLAST(nodeName = didStartElement,
                        attributes = attrDict,
                        parent = null)
                curElement = astRoot
            }
            if (depth > cardDepth) {
                val newElement = XMLAST(
                        nodeName = didStartElement, attributes = attrDict,
                        parent = curElement)
                curElement?.addChild(child = newElement)
                curElement = newElement
            }
        }

        override fun parser(parser: NSXMLParser, foundCharacters: String) {
            curElement?.addInner(foundCharacters)
        }

        private fun parseCard() {
            val card = try {
                readCardXML(astRoot ?: return)
            } catch (e: Exception) {
                return
            }
            callback(card)
        }

        override fun parser(parser: NSXMLParser,
                            didEndElement: String,
                            namespaceURI: String?,
                            qualifiedName: String?) {
            if (depth == cardDepth) {
                parseCard()
                astRoot = null
            }
            depth -= 1
            curElement = curElement?.parent
        }

        override fun parserDidEndDocument(parser: NSXMLParser) {
            parseCard()
            astRoot = null
        }

        override fun parserDidStartDocument(parser: NSXMLParser) {
        }

        override fun parser(parser: NSXMLParser, parseErrorOccurred: NSError) {
            println("parse error=$parseErrorOccurred")
        }
    }

    @Throws(Throwable::class)
    fun readXmlFromInputStream(inputStream: NSInputStream, callback: (card: Card) -> Unit) = logAndSwiftWrap(TAG, "Xml reading failed") {
        val wrappedStream = StreamWrapper(inputStream)
        val parser = try {
            NSXMLParser(stream = wrappedStream)
        } catch (e: Exception) {
            null
        }
        val delegate = XMLDelegate(callback)
        parser?.delegate = delegate
        val err = parser?.parse()
        println("parser result=$err")
    }

    @Throws(Throwable::class)
    fun readXmlFromUrl(xmlUrl: NSURL, callback: (card: Card) -> Unit) {
        val inputStream = NSInputStream.inputStreamWithURL(xmlUrl) ?: return
        readXmlFromInputStream(inputStream, callback)
    }

    class StreamWrapper(
            private val inputStream: NSInputStream,
            private var lookAhead: AtomicRef<ImmutableByteArray?> = AtomicRef(null)
    ) : NSInputStream(NSData()) {
        override fun open() {
            inputStream.open()
        }

        override fun close() {
            inputStream.close()
        }

        private fun baseGetChar(): Byte? {
            if (!inputStream.hasBytesAvailable) {
                return null
            }
            val b = ubyteArrayOf(0.toUByte())
            var bl = 0.toLong()
            b.usePinned {
                bl = inputStream.read(it.addressOf(0), 1.toULong())
            }

            if (bl != 0.toLong()) {
                return b[0].toByte()
            } else {
                return null
            }
        }

        private fun extGet(buffer: CPointer<uint8_tVar>?, buflen: Long, ptr: Int): Byte? {
            if (ptr < buflen && buffer != null)
                return buffer[ptr].toByte()
            while ((lookAhead.value?.size ?: 0) <= ptr - buflen) {
                val b = baseGetChar() ?: return null
                lookAhead.value = (lookAhead.value
                        ?: ImmutableByteArray.empty()) + ImmutableByteArray.of(b)
            }

            return lookAhead.value?.get((ptr - buflen).toInt())
        }

        private fun nextValidChar(buffer: CPointer<uint8_tVar>?,
                                  buflen: Long, ptrStart: Int, fetch: Boolean): Pair<Byte, Int>? {
            var ptr = ptrStart
            while (fetch || ptr < buflen) {
                val c = extGet(buffer, buflen, ptr) ?: return null
                if (c == '&'.code.toByte()
                        && extGet(buffer, buflen, ptr + 1) == '#'.code.toByte()
                        && extGet(buffer, buflen, ptr + 2) == '0'.code.toByte()
                        && extGet(buffer, buflen, ptr + 3) == ';'.code.toByte()) {
                    ptr += 4
                    continue
                }

                if (c == '\n'.code.toByte() || c == '\r'.code.toByte()
                        || c == '\t'.code.toByte() ||
                        (c.toInt() and 0xff) in 0x20..0xff) {
                    ptr++
                    return Pair(c, ptr - ptrStart)
                }

                // TODO: skip invalid UTF-8 sequences?

                ptr++
            }

            return null
        }

        override fun read(buffer: CPointer<uint8_tVar>?, maxLength: NSUInteger): NSInteger {
            var ptr = 0
            if (buffer == null)
                return 0
            lookAhead.value?.let {
                val len = minOf(maxLength.toInt(), it.size)
                if (len != 0)
                    memcpy(buffer, it.toNSData().bytes, len.toULong())
                ptr += len
                if (ptr < len)
                    lookAhead.value = lookAhead.value?.drop(len - ptr)
                else
                    lookAhead.value = null
            }
            if (ptr == maxLength.toInt())
                return ptr.toLong()
            val end = ptr + inputStream.read(buffer + ptr, maxLength - ptr.toUInt())
            var optr = ptr
            while (optr < end) {
                val cc = nextValidChar(buffer, end, ptr, false) ?: break
                buffer[optr] = cc.first.toUByte()
                ptr += cc.second
                optr++
            }
            normalizeLookAhead((ptr - end).toInt())
            return optr.toLong()
        }

        private fun normalizeLookAhead(bytesToDrop: Int) {
            var toDrop = bytesToDrop
            while (true) {
                lookAhead.value = lookAhead.value?.drop(toDrop)
                if (lookAhead.value?.isEmpty() != false) {
                    lookAhead.value = null
                    return
                }

                val cc = nextValidChar(null, 0, 0, true)
                if (cc == null || cc.second == 0) {
                    lookAhead.value = null
                    return
                }
                if (cc.second == 1)
                    return
                toDrop = cc.second - 1
            }
        }

        override fun hasBytesAvailable(): Boolean {
            if (lookAhead.value != null)
                return true
            val cc = nextValidChar(null, 0, 0, true)
            if (cc == null || cc.second == 0) {
                lookAhead.value = null
                return false
            }
            normalizeLookAhead(cc.second - 1)
            return true
        }

        override fun getBuffer(buffer: CPointer<CPointerVar<uint8_tVar>>?, length: CPointer<NSUIntegerVar>?): Boolean = false
    }

    const val TAG = "XmlFormat"
}
