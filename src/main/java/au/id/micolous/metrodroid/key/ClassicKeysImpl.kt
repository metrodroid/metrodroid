package au.id.micolous.metrodroid.key

import android.nfc.tech.MifareClassic
import android.support.annotation.VisibleForTesting
import au.id.micolous.metrodroid.transit.smartrider.SmartRiderTransitData
import au.id.micolous.metrodroid.util.Utils
import au.id.micolous.metrodroid.util.ImmutableByteArray
import au.id.micolous.metrodroid.util.toImmutable
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject


private const val SECTOR_IDX = "sector"
private const val KEYS = "keys"

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

    private val keysJson: JSONArray
        @Throws(JSONException::class)
        get() {
            val res = JSONArray()
            for ((sector, keys) in mKeys.entries.sortedBy { it.key }) {
                keys.map { it.toJSON() }.forEach {
                    it.put(SECTOR_IDX, sector)
                    res.put(it)
                }
            }

            return res
        }

    protected val baseJson: JSONObject
        @Throws(JSONException::class)
        get() {
            val res = JSONObject()
            res.put(KEYS, keysJson)
            res.put(CardKeys.JSON_KEY_TYPE_KEY, type)
            return res
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

    @VisibleForTesting
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

    @VisibleForTesting
    internal fun getProperCandidates(sectorNumber: Int): List<ClassicSectorKey>? = mKeys[sectorNumber]

    companion object {
        private fun wellKnown(b: ByteArray, bundle: String): ClassicSectorKey = ClassicSectorKey.fromDump(
                b.toImmutable(), ClassicSectorKey.KeyType.A, bundle)

        private val PREAMBLE_KEY = ByteArray(6) { 0 }

        /**
         * Contains a list of widely used MIFARE Classic keys.
         *
         * None of the keys here are unique to a particular transit card, or to a vendor of transit
         * ticketing systems.
         *
         * Even if a transit operator uses (some) fixed keys, please do not add them here.
         *
         * If you are unable to identify a card by some data on it (such as a "magic string"), then
         * you should use [Utils.checkKeyHash], and include a hashed
         * version of the key in Metrodroid.
         *
         * See [&lt;][SmartRiderTransitData.detectKeyType] for an example of how to do
         * this.
         */
        private val WELL_KNOWN_KEYS = listOf(
                wellKnown(MifareClassic.KEY_DEFAULT, "well-known-ff"),
                wellKnown(PREAMBLE_KEY, "well-known-zero"),
                wellKnown(MifareClassic.KEY_MIFARE_APPLICATION_DIRECTORY, "well-known-mad"),
                wellKnown(MifareClassic.KEY_NFC_FORUM, "well-known-ndef"))

        @Throws(JSONException::class)
        fun keysFromJSON(jsonRoot: JSONObject, allowMissingIdx: Boolean, defaultBundle: String):
                Map<Int, List<ClassicSectorKey>> {
            val keysJSON = jsonRoot.getJSONArray(KEYS)
            val keys = mutableMapOf<Int, MutableList<ClassicSectorKey>>()
            for (i in 0 until keysJSON.length()) {
                val json = keysJSON.getJSONObject(i)

                val w = ClassicSectorKey.fromJSON(json, defaultBundle)
                val sectorIndex = if (json.has(SECTOR_IDX) || !allowMissingIdx)
                // if allowMissingIdx is false, purposefully trip the exception
                    json.getInt(SECTOR_IDX)
                else
                    i

                if (!keys.containsKey(sectorIndex)) {
                    keys[sectorIndex] = mutableListOf()
                }

                keys[sectorIndex]?.add(w)
            }
            return keys
        }

        fun flattenKeys(lst: List<ClassicStaticKeys>): Map<Int, List<ClassicSectorKey>> {
            val keys = mutableMapOf<Int, MutableList<ClassicSectorKey>>()
            for (who in lst)
                for ((key, value) in who.mKeys) {
                    if (!keys.containsKey(key))
                        keys[key] = mutableListOf()
                    keys[key]?.addAll(value)
                }
            return keys
        }
    }
}
