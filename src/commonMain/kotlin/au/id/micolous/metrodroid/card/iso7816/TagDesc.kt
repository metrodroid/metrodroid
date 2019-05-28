package au.id.micolous.metrodroid.card.iso7816

import au.id.micolous.metrodroid.util.ImmutableByteArray
import au.id.micolous.metrodroid.util.NumberUtils
import au.id.micolous.metrodroid.util.countryCodeToName
import au.id.micolous.metrodroid.util.currencyNameByCode

data class TagDesc(val name: String, val contents: TagContents) {
    fun interpretTag(data: ImmutableByteArray) : String = when (contents) {
        TagContents.ASCII -> data.readASCII()
        TagContents.DUMP_SHORT -> data.toHexString()
        TagContents.DUMP_LONG -> data.toHexString()
        TagContents.CURRENCY -> {
            val n = NumberUtils.convertBCDtoInteger(data.byteArrayToInt())
            currencyNameByCode(n) ?: n.toString()
        }
        TagContents.COUNTRY -> countryCodeToName(NumberUtils.convertBCDtoInteger(data.byteArrayToInt()))
        else -> data.toHexString()
    }
}

enum class TagContents {
    DUMP_SHORT,
    DUMP_LONG,
    ASCII,
    DUMP_UNKNOWN,
    HIDE,
    CURRENCY,
    COUNTRY
}
