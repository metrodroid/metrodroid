package au.id.micolous.metrodroid.card.ksx6924

import au.id.micolous.metrodroid.card.iso7816.TagDesc
import au.id.micolous.metrodroid.card.iso7816.TagDesc.Companion.TagContents.*

object KROCAPData {
    const val PROPRIETARY_TEMPLATE = "a5"

    // TODO: i18n
    val TAGMAP = mapOf(
            "50" to TagDesc("Payment terms", DUMP_SHORT),
            "47" to TagDesc("Supported protocols", DUMP_SHORT),
            "43" to TagDesc("ID center", DUMP_SHORT),
            "11" to TagDesc("Balance command", DUMP_SHORT),
            "4f" to TagDesc("ADF AID", DUMP_SHORT),
            "9f10" to TagDesc("Additional file references", DUMP_SHORT),
            "45" to TagDesc("Usercode", DUMP_SHORT),
            "5f24" to TagDesc("Expiry", DUMP_SHORT),
            "12" to TagDesc("Card serial number", HIDE),
            "13" to TagDesc("Operator serial number", DUMP_SHORT),
            "bf0c" to TagDesc("Discretionary data", DUMP_SHORT)
    )
}