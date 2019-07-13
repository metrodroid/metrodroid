/*
 * XmlCardFormat.kt
 *
 * Copyright 2019 Google
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
import au.id.micolous.metrodroid.card.CardType
import au.id.micolous.metrodroid.card.calypso.CalypsoApplication
import au.id.micolous.metrodroid.card.cepas.CEPASApplication
import au.id.micolous.metrodroid.card.cepascompat.CEPASCard
import au.id.micolous.metrodroid.card.china.ChinaCard
import au.id.micolous.metrodroid.card.classic.ClassicCard
import au.id.micolous.metrodroid.card.classic.ClassicSectorRaw
import au.id.micolous.metrodroid.card.desfire.DesfireCard
import au.id.micolous.metrodroid.card.desfire.files.RawDesfireFile
import au.id.micolous.metrodroid.card.desfire.settings.DesfireFileSettings
import au.id.micolous.metrodroid.card.felica.FelicaCard
import au.id.micolous.metrodroid.card.iso7816.*
import au.id.micolous.metrodroid.card.ksx6924.KSX6924Application
import au.id.micolous.metrodroid.card.ultralight.UltralightCard
import au.id.micolous.metrodroid.multi.Log
import au.id.micolous.metrodroid.time.MetroTimeZone
import au.id.micolous.metrodroid.time.TimestampFull
import au.id.micolous.metrodroid.util.ImmutableByteArray
import kotlinx.io.InputStream
import kotlinx.serialization.*
import kotlinx.serialization.CompositeDecoder.Companion.READ_DONE
import kotlinx.serialization.CompositeDecoder.Companion.UNKNOWN_NAME
import kotlinx.serialization.internal.EnumDescriptor

private const val TAG = "XmlCardFormat"

private val aliases = mapOf(
        "partial_read" to "isPartialRead")

@SerialInfo
@Target(AnnotationTarget.PROPERTY)
annotation class XMLId(val id: String)

@SerialInfo
@Target(AnnotationTarget.PROPERTY)
annotation class XMLInline

@SerialInfo
@Target(AnnotationTarget.PROPERTY)
annotation class XMLHex

@SerialInfo
@Target(AnnotationTarget.PROPERTY)
annotation class XMLListIdx(val idxElem: String)

@SerialInfo
@Target(AnnotationTarget.PROPERTY)
annotation class XMLDesfireManufacturingData

@SerialInfo
@Target(AnnotationTarget.CLASS)
annotation class XMLIgnore(val ignore: String)

interface NodeWrapper {
    val childNodes: List<NodeWrapper>
    val nodeName: String
    val nodeValue: String?
    val textContent: String?
    val attributes: Map <String, String>
}

/**
 * There is some circumstance where the OLD CEPASTransaction could contain null bytes,
 * which would then be serialized as `&#0;`.
 *
 * From this Android commit, it is no longer possible to serialise a null byte:
 * https://android.googlesource.com/platform/libcore/+/ff42219e3ea3d712f931ae7f26af236339b5cf23%5E%21/#F2
 *
 * However, these entities may still be deserialised. Importing an old file that
 * contains a null byte in an attribute will trigger an error if we try to re-serialise
 * it with kxml2.
 *
 * This runs a filter to drop characters that fail these rules:
 * https://android.googlesource.com/platform/libcore/+/master/xml/src/main/java/com/android/org/kxml2/io/KXmlSerializer.java#155
 *
 * NOTE: This does not escape entities. This only removes things that can't be properly
 * encoded.
 *
 * @param input Input data to strip characters from
 * @return Data without characters that can't be encoded.
 */
fun filterBadXMLChars(input: String): String {
    val o = StringBuilder()
    var i = 0
    while (i < input.length) {
        val c = input[i]

        // Skip &#0;
        if (c == '&' && i < input.length - 3 && input[i+1] == '#' && input[i+2] == '0' && input[i+3] == ';') {
            i += 3
        } else if (c == '\n' || c == '\r' || c == '\t' ||
                c.toInt() in 0x20..0xd7ff ||
                c.toInt() in 0xe000..0xfffd) {
            o.append(c)
        } else if (isHighSurrogate(c) && i < input.length - 1) {
            o.append(c)
            o.append(input[i++])
        }
        i++

        // Other characters invalid.
    }
    return o.toString()
}

private fun isHighSurrogate(c: Char): Boolean = (c.toInt() and 0xffff) in 0xD800..0xDB7F

class XMLInput internal constructor(private val parent: NodeWrapper,
                                    private val strict: Boolean,
                                    private val skippable: Set<String>,
                                    private val ignore: Set<String> = emptySet(),
                                    private var state: State = State.ATTRIBUTES_AND_TAGS_KV_PHASE_1,
                                    private val listIdxElem: String? = null) :
        ElementValueDecoder() {
    private var curTagIndex: Int = -1
    private var curCounter = -1
    private val attributes = parent.attributes
    private val attributeIterator = attributes.iterator()
    private var elementAnnotations: List<Annotation>? = null
    private var currentValue: String? = null
    private var currentKey: String? = null

    private fun sortNodes(lst: List<NodeWrapper>, sortKey: String?): List<NodeWrapper> {
        if (sortKey == null || state == State.MAP_KEY || state == State.MAP_VALUE)
            return lst
        return lst.sortedBy { -(it.attributes[sortKey]?.toInt() ?: 0) }
    }

    private val children: List<NodeWrapper> = sortNodes(parent.childNodes, listIdxElem)

    internal enum class State {
        ATTRIBUTES_AND_TAGS_KV_PHASE_1,
        ATTRIBUTES_AND_TAGS_KV_PHASE_2,
        TAGS_LIST,
        INLINE_LIST,
        MAP_KEY,
        MAP_VALUE
    }

    /*private val currentNode: NodeWrapper
        get() = when (state) {
            State.ATTRIBUTES_AND_TAGS_KV_PHASE_1 -> currentAttribute
            State.INLINE_LIST -> parent
            else -> children[curTagIndex]
        }*/

    override fun beginStructure(desc: SerialDescriptor, vararg typeParams: KSerializer<*>): CompositeDecoder {
        if (curCounter == -1)
            return this
        val newState = when (desc.kind) {
            StructureKind.LIST -> State.TAGS_LIST
            StructureKind.MAP -> State.MAP_VALUE
            else -> State.ATTRIBUTES_AND_TAGS_KV_PHASE_1
        }
        val newNode = when (state) {
            State.ATTRIBUTES_AND_TAGS_KV_PHASE_1 -> throw Exception("Unexpected nesting of attributes")
            State.INLINE_LIST -> parent
            else -> children[curTagIndex]
        }
        return XMLInput(newNode, state = newState, strict = strict,
                listIdxElem = computeIdxElem(),
                skippable = listOfNotNull(listIdxElem).toSet(),
                ignore = desc.getEntityAnnotations().filterIsInstance<XMLIgnore>()
                        .map { it.ignore }.toSet())
    }

    private fun computeIdxElem(): String? {
        return elementAnnotations?.filterIsInstance<XMLListIdx>()?.singleOrNull()?.idxElem
    }

    override fun decodeElementIndex(desc: SerialDescriptor): Int {
        while (true) {
            if (state != State.MAP_KEY && !nextNode())
                return READ_DONE
            elementAnnotations = null
            val id = when (state) {
                State.TAGS_LIST -> return 0
                State.MAP_KEY -> {
                    state = State.MAP_VALUE; return curCounter * 2 + 1
                }
                State.MAP_VALUE -> {
                    state = State.MAP_KEY; return curCounter * 2
                }
                State.INLINE_LIST -> descInline(desc)
                else -> descIndex(desc, currentKey!!)
            }

            if (id != UNKNOWN_NAME) {
                elementAnnotations = desc.getElementAnnotations(id)
                return id
            }

            if (state == State.INLINE_LIST)
                continue

            if (currentKey in skippable)
                continue

            if (!strict)
                continue
            throw Exception("Unknown node $currentKey")
        }
    }

    private fun descIndex(desc: SerialDescriptor, key: String): Int {
        val ind = desc.getElementIndex(key)
        if (ind != UNKNOWN_NAME)
            return ind
        for (i in 0 until desc.elementsCount)
            if (desc.getElementAnnotations(i).filterIsInstance<XMLId>().singleOrNull()?.id == key) {
                return i
            }
        val indAlias = aliases[key]?.let { desc.getElementIndex(it) } ?: UNKNOWN_NAME
        if (indAlias != UNKNOWN_NAME)
            return indAlias
        return UNKNOWN_NAME
    }

    private fun descInline(desc: SerialDescriptor): Int {
        Log.d(TAG, "inline")
        for (i in 0 until desc.elementsCount)
            if (desc.getElementAnnotations(i).filterIsInstance<XMLInline>().isNotEmpty()) {
                return i
            }
        return UNKNOWN_NAME
    }

    private fun nextNode(): Boolean {
        if (state == State.ATTRIBUTES_AND_TAGS_KV_PHASE_1) {
            while (true) {
                if (!attributeIterator.hasNext()) {
                    break
                }
                curCounter++
                val currentAttribute = attributeIterator.next()
                currentValue = currentAttribute.value
                currentKey = currentAttribute.key
                currentKey.let {
                    if (it != null && it !in ignore)
                        return true
                }
            }
            state = State.ATTRIBUTES_AND_TAGS_KV_PHASE_2
            curTagIndex = -1
        }
        if (state != State.INLINE_LIST) {
            while (true) {
                curTagIndex++
                if (curTagIndex !in children.indices) {
                    break
                }
                val currentNode = children[curTagIndex]
                if (currentNode.nodeName !in listOf("#text", "#comment")
                        && currentNode.nodeName !in ignore) {
                    curCounter++
                    currentKey = currentNode.nodeName
                    currentValue = currentNode.nodeValue ?: currentNode.textContent
                    return true
                }
            }

            state = State.INLINE_LIST
            curTagIndex = -1
        }
        if (state == State.INLINE_LIST) {
            curTagIndex++
            if (curTagIndex < 1) {
                currentValue = parent.nodeValue ?: parent.textContent
                currentKey = parent.nodeName
                return true
            }
        }

        return false
    }

    private fun takeStr(): String {
        if (state == State.MAP_KEY) {
            val ret = children[curTagIndex].attributes[listIdxElem!!]
            return ret!!
        }
        return currentValue!!
    }

    override fun decodeBoolean(): Boolean = takeStr().toBoolean()
    override fun decodeByte(): Byte = takeStr().toByte()
    override fun decodeShort(): Short = takeStr().toShort()
    override fun decodeInt(): Int = takeStr().toInt()
    override fun decodeLong(): Long = takeStr().toLong()
    override fun decodeFloat(): Float = takeStr().toFloat()
    override fun decodeDouble(): Double = takeStr().toDouble()
    override fun decodeChar(): Char = takeStr().single()
    override fun decodeString(): String = takeStr()

    override fun decodeEnum(enumDescription: EnumDescriptor): Int = enumDescription.getElementIndex(takeStr())

    @Suppress("UNCHECKED_CAST")
    override fun <T> decodeSerializableValue(deserializer: DeserializationStrategy<T>): T {
        when (deserializer.descriptor.name) {
            TimestampFull.serializer().descriptor.name ->
                return TimestampFull(timeInMillis = decodeLong(), tz = MetroTimeZone.LOCAL) as T
            ImmutableByteArray.Companion.descriptor.name -> {
                return when {
                    elementAnnotations.orEmpty().filterIsInstance<XMLDesfireManufacturingData>().isNotEmpty() -> super.decodeSerializableValue(DesfireManufacturingDataXmlAdapter.serializer()).makeRaw()
                    elementAnnotations.orEmpty().filterIsInstance<XMLHex>().isNotEmpty() -> ImmutableByteArray.fromHex(decodeString())
                    else -> ImmutableByteArray.fromBase64(decodeString())
                } as T
            }
            ClassicSectorRaw.serializer().descriptor.name -> {
                val a = super.decodeSerializableValue(ClassicSectorRawXmlAdapter.serializer())
                return ClassicSectorRaw(blocks = a.blocks.map { it.data },
                        isUnauthorized = a.isUnauthorized,
                        error = if (a.invalid && a.error != null) "invalid" else a.error,
                        keyA = if (a.keyType == "KeyB") null else a.key,
                        keyB = if (a.keyType == "KeyB") a.key else null) as T
            }
            RawDesfireFile.serializer().descriptor.name -> {
                val a = super.decodeSerializableValue(DesfireFileXmlAdapter.serializer())
                return RawDesfireFile(data = a.data, settings = a.settings?.toRaw(),
                        error = a.error, isUnauthorized = a.unauthorized) as T
            }
            ISO7816AppSerializer.descriptor.name -> {
                val a = super.decodeSerializableValue(ISO7816ApplicationXmlAdapter.serializer())
                return a.convert() as T
            }
        }
        return super.decodeSerializableValue(deserializer)
    }
}

@Serializable
class DesfireManufacturingDataXmlAdapter(
        @Optional
        val raw: ImmutableByteArray? = null,
        @XMLId("batch-no")
        val batchno: Long,
        @XMLId("hw-major-version")
        val hwmajorversion: Int,
        @XMLId("hw-minor-version")
        val hwminorversion: Int,
        @XMLId("hw-protocol")
        val hwprotocol: Int,
        @XMLId("hw-storage-size")
        val hwstoragesize: Int,
        @XMLId("hw-sub-type")
        val hwsubtype: Int,
        @XMLId("hw-type")
        val hwtype: Int,
        @XMLId("hw-vendor-id")
        val hwvendorid: Int,
        @XMLId("sw-major-version")
        val swmajorversion: Int,
        @XMLId("sw-minor-version")
        val swminorversion: Int,
        @XMLId("sw-protocol")
        val swprotocol: Int,
        @XMLId("sw-storage-size")
        val swstoragesize: Int,
        @XMLId("sw-sub-type")
        val swsubtype: Int,
        @XMLId("sw-type")
        val swtype: Int,
        @XMLId("sw-vendor-id")
        val swvendorid: Int,
        @XMLId("uid")
        val uid: Long,
        @XMLId("week-prod")
        val weekprod: Int,
        @XMLId("year-prod")
        val yearprod: Int
) {
    fun makeRaw(): ImmutableByteArray {
        if (raw != null)
            return raw
        // TODO: reassemble components
        return ImmutableByteArray.ofB(hwvendorid,
                hwtype, hwsubtype,
                hwmajorversion, hwminorversion,
                hwstoragesize, hwprotocol,
                swvendorid, swtype, swsubtype,
                swmajorversion, swminorversion,
                swstoragesize, swprotocol,
                (uid shr 48), (uid shr 40),
                (uid shr 32), (uid shr 24),
                (uid shr 16), (uid shr 8),
                uid,
                (batchno shr 32), (batchno shr 24),
                (batchno shr 16), (batchno shr 8),
                batchno, weekprod, yearprod)
    }
}

@XMLIgnore("commsetting")
@Serializable
class DesfireFileSettingsXmlAdapter(
        @XMLHex
        private val accessrights: ImmutableByteArray,
        private val filetype: Byte,
        // Old Farebot (before 2014-09-01) called this "commsetting" -- but never used this field.
        @Optional
        private val commsettings: Byte = 0,
        @Optional
        private val filesize: Int = 0,
        @Optional
        private val recordsize: Int = 0,
        @Optional
        private val maxrecords: Int = 0,
        @Optional
        private val currecords: Int = 0,
        @Optional
        private val min: Int = 0,
        @Optional
        private val max: Int = 0,
        @Optional
        private val limitcredit: Int = 0,
        @Optional
        private val limitcreditenabled: Boolean = false
) {
    private fun encodeInt3(v: Int) = ImmutableByteArray.ofB(v,
            (v shr 8), (v shr 16))

    private fun encodeInt4(v: Int) = ImmutableByteArray.ofB(v,
            (v shr 8), (v shr 16), (v shr 24))

    fun toRaw(): ImmutableByteArray {
        val head = ImmutableByteArray.of(filetype, commsettings) + accessrights
        return when (filetype) {
            DesfireFileSettings.STANDARD_DATA_FILE, DesfireFileSettings.BACKUP_DATA_FILE ->
                head + encodeInt3(filesize)
            DesfireFileSettings.LINEAR_RECORD_FILE, DesfireFileSettings.CYCLIC_RECORD_FILE ->
                head + encodeInt3(recordsize) + encodeInt3(maxrecords) + encodeInt3(currecords)
            DesfireFileSettings.VALUE_FILE -> head + encodeInt4(min) + encodeInt4(max) +
                    encodeInt4(limitcredit) + ImmutableByteArray.of(if (limitcreditenabled) 1 else 0)
            else -> throw Exception("Unknown file type: $filetype")
        }
    }
}

@Serializable
class DesfireFileXmlAdapter(
        @Optional
        val settings: DesfireFileSettingsXmlAdapter? = null,
        // Old Farebot (before 2014-09-01) was missing this tag when it couldn't read a file
        @Optional
        val data: ImmutableByteArray? = null,
        @Optional
        val error: String? = null,
        @Optional
        val unauthorized: Boolean = false)

@Serializable
class ClassicBlockRawXmlAdapter(
        val data: ImmutableByteArray,
        val type: String)

@Serializable
class ClassicSectorRawXmlAdapter(
        @XMLListIdx("index")
        @Optional
        val blocks: List<ClassicBlockRawXmlAdapter> = emptyList(),
        @Optional
        val key: ImmutableByteArray? = null,
        @Optional
        @XMLId("keytype")
        val keyType: String? = null,
        @XMLId("unauthorized")
        @Optional
        val isUnauthorized: Boolean = false,
        @Optional
        val error: String? = null,
        @Optional
        val invalid: Boolean = false)

@Serializable
class ISO7816FileXMLAdapterSfi(val file: ISO7816File)

@Serializable
@XMLIgnore("entry")
class ISO7816ApplicationXmlAdapter(
        @XMLId("application-data")
        @Optional
        val appFci: ImmutableByteArray? = null,
        @XMLId("application-name")
        @Optional
        val appName: ImmutableByteArray? = null,
        val type: String,
        @XMLListIdx("name")
        val records: Map<ISO7816Selector, ISO7816File>,
        @XMLId("sfi-files")
        @XMLListIdx("sfi")
        @Optional
        val sfiFiles: Map<Int, ISO7816FileXMLAdapterSfi> = emptyMap(),
        val tagid: String,
        @Optional
        val balance: Int = 0,
        @Optional
        @XMLId("extra-records")
        val extraRecords: List<ImmutableByteArray> = emptyList(),
        @Optional
        @XMLListIdx("idx")
        @XMLHex
        val balances: Map<Int, String> = emptyMap(),
        @XMLListIdx("idx")
        @Optional
        private val purses: Map<Int, ImmutableByteArray> = emptyMap(),
        @XMLListIdx("idx")
        @Optional
        private val histories: Map<Int, ImmutableByteArray> = emptyMap(),
        @Optional
        @XMLHex
        @XMLId("gpo-response")
        private val gpoResponse: ImmutableByteArray? = null
) {
    private fun makeCapsule() = ISO7816ApplicationCapsule(
            files = records,
            appFci = appFci,
            appName = appName,
            sfiFiles = sfiFiles.mapValues {
                it.value.file
            }
    )

    fun convert(): ISO7816Application =
        when (type) {
            "calypso" -> CalypsoApplication(generic = makeCapsule())
            "cepas" -> CEPASApplication(generic = makeCapsule(),
                    histories = histories, purses = purses)
            "ksx6924", "tmoney" -> KSX6924Application(generic = makeCapsule(),
                    balance = ImmutableByteArray.ofB(
                            (balance shr 24),
                            (balance shr 16),
                            (balance shr 8),
                            balance),
                    extraRecords = extraRecords
                    )
            "china", "shenzhentong" -> ChinaCard(generic = makeCapsule(),
                    balances = balances.mapValues { ImmutableByteArray.fromHex(it.value.trim()) })
            else -> throw Exception("Unknown type $type")
        }
}

fun readCardXML(root: NodeWrapper): Card {
    if (root.nodeName != "card")
        throw Exception("Invalid root ${root.nodeName}")
    val cardType = root.attributes["type"] ?: throw Exception("type attribute not found")
    val xi = XMLInput(root, strict = cardType.toInt() != CardType.CEPAS.toInteger(),
            ignore = setOf("type", "id", "scanned_at", "label"),
            skippable = setOf("ultralightType", "idm"))
    val tagId = ImmutableByteArray.fromHex(root.attributes["id"]!!)
    val scannedAt = TimestampFull(timeInMillis = root.attributes["scanned_at"]!!.toLong(),
            tz = MetroTimeZone.LOCAL)
    val label = root.attributes["label"]
    when (cardType.toInt()) {
        CardType.MifareClassic.toInteger() -> return Card(
                tagId = tagId, scannedAt = scannedAt, label = label,
                mifareClassic = xi.decode(ClassicCard.serializer()))
        CardType.MifareUltralight.toInteger() -> return Card(
                tagId = tagId, scannedAt = scannedAt, label = label,
                mifareUltralight = xi.decode(UltralightCard.serializer()))
        CardType.MifareDesfire.toInteger() -> return Card(
                tagId = tagId, scannedAt = scannedAt, label = label,
                mifareDesfire = xi.decode(DesfireCard.serializer()))
        CardType.CEPAS.toInteger() -> return Card(
                tagId = tagId, scannedAt = scannedAt, label = label,
                cepasCompat = xi.decode(CEPASCard.serializer()))
        CardType.FeliCa.toInteger() -> return Card(
                tagId = tagId, scannedAt = scannedAt, label = label,
                felica = xi.decode(FelicaCard.serializer()))
        CardType.ISO7816.toInteger() -> return Card(
                tagId = tagId, scannedAt = scannedAt, label = label,
                iso7816 = xi.decode(ISO7816Card.serializer()))
        else -> throw Exception("Unknown card type $cardType")
    }
}
