package au.id.micolous.metrodroid.card.iso7816

import au.id.micolous.metrodroid.util.ImmutableByteArray
import au.id.micolous.metrodroid.util.NumberUtils
import au.id.micolous.metrodroid.util.countryCodeToName
import au.id.micolous.metrodroid.util.currencyNameByCode

data class TagDesc(val name: String, val contents: TagContents) {
    companion object {
        enum class TagContents {
            DUMP_SHORT,
            DUMP_LONG,
            ASCII,
            DUMP_UNKNOWN,
            HIDE,
            CURRENCY,
            COUNTRY
        }

        fun interpretTag(contents: TagContents, data: ImmutableByteArray) = when (contents) {
            Companion.TagContents.ASCII -> data.readASCII()
            Companion.TagContents.DUMP_SHORT -> data.toHexString()
            Companion.TagContents.DUMP_LONG -> data.toHexString()
            Companion.TagContents.CURRENCY -> currencyNameByCode(NumberUtils.convertBCDtoInteger(data.byteArrayToInt()))
            Companion.TagContents.COUNTRY -> countryCodeToName(NumberUtils.convertBCDtoInteger(data.byteArrayToInt()))
            else -> data.toHexString()
        }

    }
}
