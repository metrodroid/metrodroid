package au.id.micolous.metrodroid.transit.ndef

import au.id.micolous.metrodroid.card.UnauthorizedException
import au.id.micolous.metrodroid.card.classic.ClassicCard
import au.id.micolous.metrodroid.card.classic.ClassicSector
import au.id.micolous.metrodroid.util.HashUtils
import au.id.micolous.metrodroid.util.ImmutableByteArray

class MifareClassicAccessDirectory(val aids: List<SectorIndex>) {
    fun contains(aid: Int): Boolean = aids.firstOrNull { it.aid == aid } != null
    fun getContiguous(aid: Int): List<Int> {
        val all = getAll(aid)
        val res = mutableListOf<Int>()
        for (el in all) {
            if (res.isNotEmpty() && res.last() != el - 1
                && (res.last() != 0xf || el != 0x11))
                    break
            res += el
        }
        return res
    }

    fun getAll(aid: Int): List<Int> =
        aids.filter { it.aid == aid }.map { it.sector }

    data class SectorIndex(val sector: Int, val aid: Int)

    companion object {
        private fun parseAids(block: ImmutableByteArray, start: Int, skip: Int): List<SectorIndex> =
            (skip..7).map {
                SectorIndex(it + start, block.byteArrayToInt(it * 2, 2))
            }
        
        fun getMadVersion(sector0: ClassicSector): Int? {
            try {
                val gpb = sector0[3].data[9].toInt() and 0xff

                // We don't check keyA as it might be unknown if we read using keyB

                if ((gpb and 0x80 == 0) // DA == 0
                    || (gpb and 0x3c != 0) // RFU != 0
                )
                    return null

                val madVersion = gpb and 0x3
                if (madVersion != 1 && madVersion != 2)
                    return null

                val infoByte = sector0[1].data[1]

                if (infoByte == 0x10.toByte() || infoByte >= 0x28)
                    return null

                val storedCrc = sector0[1].data[0].toInt() and 0xff

                val crc = HashUtils.calculateCRC8NXP(
                    sector0[1].data.sliceOffLen(1, 15),
                    sector0[2].data
                )

                if (storedCrc != crc)
                    return null

                return madVersion
            } catch (e: UnauthorizedException) {
                return null
            }
        }

        fun sector0Aids(sector0: ClassicSector) = parseAids(sector0[1].data, 0, 1) +
                parseAids(sector0[2].data, 8, 0)

        fun parse(card: ClassicCard): MifareClassicAccessDirectory? {
            try {
                val madVersion = getMadVersion(card[0]) ?: return null

                if (madVersion == 2 && card.sectors.size <= 0x10)
                    return null

                val infoByte = card[0, 1].data[1]

                if (infoByte >= card.sectors.size)
                    return null

                val aids = sector0Aids(card[0])

                if (madVersion == 1)
                    return MifareClassicAccessDirectory(aids)

                val gpb2 = card[0x10, 3].data[9].toInt() and 0xff

                if (gpb2 != 0)
                    return null

                val infoByte2 = card[0x10, 0].data[1]

                if (infoByte2 == 0x10.toByte() || infoByte2 >= card.sectors.size)
                    return null

                val crc2 = HashUtils.calculateCRC8NXP(
                    card[0x10,0].data.sliceOffLen(1, 15),
                    card[0x10,1].data, card[0x10,2].data)

                val storedCrc2 = card[0x10,0].data[0].toInt() and 0xff

                if (storedCrc2 != crc2)
                    return null

                val aids2 = parseAids(card[0x10,0].data, 16, 1) +
                        parseAids(card[0x10,1].data, 24, 0) +
                        parseAids(card[0x10,2].data, 32, 0)

                return MifareClassicAccessDirectory(aids + aids2)
            } catch (e: UnauthorizedException) {
                return null
            }
        }

        fun sector0Contains(sector0: ClassicSector, aid: Int): Boolean {
            getMadVersion(sector0) ?: return false
            return sector0Aids(sector0).firstOrNull { it.aid == aid } != null
        }

        const val NFC_AID = 0x3e1
    }
}