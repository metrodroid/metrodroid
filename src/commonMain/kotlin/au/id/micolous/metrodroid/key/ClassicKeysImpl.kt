package au.id.micolous.metrodroid.key

import au.id.micolous.metrodroid.util.ImmutableByteArray
import kotlinx.serialization.json.*

abstract class ClassicKeysImpl(
        protected val mKeys: Map<Int, List<ClassicSectorKey>>) : ClassicKeys {

    private val keySet: Set<String>
        get() = dedupKeys(mKeys.values + listOf(WELL_KNOWN_KEYS))

    private val properKeySet: Set<String>
        get() = dedupKeys(mKeys.values)

    private fun dedupKeys(input: Collection<List<ClassicSectorKey>>) = input.flatten()
            .map {it -> it.key.toHexString() }.toSet()

    internal val keyCount: Int
        get () = properKeySet.size

    private val keysJson: JsonArray
        get() = jsonArray {
                for ((sector, keys) in mKeys.entries.sortedBy { it.key }) {
                    keys.map { it.toJSON(sector) }.forEach {
                        +it
                    }
                }
            }

    protected val baseJson: JsonObject
        get() = json {
            KEYS to keysJson
            CardKeys.JSON_KEY_TYPE_KEY to type
        }


    /**
     * Gets all keys for the card.
     *
     * @return All [ClassicSectorKey] for the card.
     */
    override val allKeys: List<ClassicSectorKey>
        get() = keySet.toList().map {
            ClassicSectorKey.fromDump(
                    ImmutableByteArray.fromHex(it), ClassicSectorKey.KeyType.UNKNOWN, "all-keys")
        }

    internal val allProperKeys: List<ClassicSectorKey>
        get() = properKeySet.toList().map {
            ClassicSectorKey.fromDump(
                    ImmutableByteArray.fromHex(it), ClassicSectorKey.KeyType.UNKNOWN, "all-keys")
        }

    /**
     * Gets the keys for a particular sector on the card.
     *
     * @param sectorNumber The sector number to retrieve the key for
     * @return All candidate [ClassicSectorKey] for that sector, or an empty list if there is
     * no known key, or the sector is out of range.
     */
    override fun getCandidates(sectorNumber: Int, preferences: List<String>): List<ClassicSectorKey> =
            ((mKeys[sectorNumber] ?: emptyList()) + WELL_KNOWN_KEYS).sortedBy { key ->
                preferences.indexOf(key.bundle).let { if (it == -1) preferences.size else it }
            }

    internal fun getProperCandidates(sectorNumber: Int): List<ClassicSectorKey>? = mKeys[sectorNumber]

    companion object {
        private const val KEYS = "keys"
        private fun wellKnown(b: String, bundle: String): ClassicSectorKey = ClassicSectorKey.fromDump(
                ImmutableByteArray.fromHex(b), ClassicSectorKey.KeyType.A, bundle)

        /**
         * Contains a list of widely used MIFARE Classic keys.
         *
         * None of the keys here are unique to a particular transit card, or to a vendor of transit
         * ticketing systems.
         *
         * Even if a transit operator uses (some) fixed keys, please do not add them here.
         *
         * If you are unable to identify a card by some data on it (such as a "magic string"), then
         * you should use [HashUtils.checkKeyHash], and include a hashed
         * version of the key in Metrodroid.
         *
         * See [&lt;][SmartRiderTransitData.detectKeyType] for an example of how to do
         * this.
         */
        private val WELL_KNOWN_KEYS = listOf(
                wellKnown("ffffffffffff", "well-known-ff"),
                wellKnown("000000000000", "well-known-zero"),
                wellKnown("a0a1a2a3a4a5", "well-known-mad"),
                wellKnown("d3f7d3f7d3f7", "well-known-ndef"))

        fun keysFromJSON(jsonRoot: JsonObject, allowMissingIdx: Boolean, defaultBundle: String):
                Map<Int, List<ClassicSectorKey>> {
            val keysJSON = jsonRoot[KEYS].jsonArray
            val keys = mutableMapOf<Int, MutableList<ClassicSectorKey>>()
            for ((i, jsonElement) in keysJSON.withIndex()) {
                val json = jsonElement.jsonObject
                val w = classicFromJSON(json, defaultBundle)
                var sectorIndex = json.getPrimitiveOrNull(ClassicSectorKey.SECTOR_IDX)?.intOrNull
                if (sectorIndex == null && allowMissingIdx)
                    sectorIndex = i
                // if allowMissingIdx is false, purposefully trip the exception
                sectorIndex!!

                if (!keys.containsKey(sectorIndex)) {
                    keys[sectorIndex] = mutableListOf()
                }

                keys[sectorIndex]?.add(w)
            }
            return keys
        }

        private const val KEY_BUNDLE = "bundle"
        private const val KEY_LEN = 6

        private fun classicFromJSON(json: JsonObject, defaultBundle: String): ClassicSectorKey {
            val t = json.getPrimitiveOrNull(ClassicSectorKey.KEY_TYPE)?.contentOrNull
            val kt = when (t) {
                "", null -> ClassicSectorKey.KeyType.UNKNOWN
                ClassicSectorKey.TYPE_KEYA -> ClassicSectorKey.KeyType.A
                ClassicSectorKey.TYPE_KEYB -> ClassicSectorKey.KeyType.B
                else -> ClassicSectorKey.KeyType.UNKNOWN
            }

            val keyData = ImmutableByteArray.fromHex(json[ClassicSectorKey.KEY_VALUE].content)

            // Check that the key is the correct length
            if (keyData.size != KEY_LEN) {
                throw Exception("Expected $KEY_LEN bytes in key, got ${keyData.size}")
            }

            // Checks completed, pass the data back.
            return ClassicSectorKey(type = kt, key = keyData,
                    bundle = json.getPrimitiveOrNull(KEY_BUNDLE)?.contentOrNull ?: defaultBundle)
        }

        fun classicFromJSON(json: String, defaultBundle: String): ClassicSectorKey =
                classicFromJSON(Json.plain.parseJson(json).jsonObject, defaultBundle)
    }
}
