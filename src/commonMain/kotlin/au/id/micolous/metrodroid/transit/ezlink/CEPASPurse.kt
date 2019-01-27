package au.id.micolous.metrodroid.transit.ezlink

import au.id.micolous.metrodroid.card.cepas.CEPASApplication
import au.id.micolous.metrodroid.transit.CardInfo
import au.id.micolous.metrodroid.transit.TransitData
import au.id.micolous.metrodroid.transit.TransitIdentity
import au.id.micolous.metrodroid.util.ImmutableByteArray

expect class CEPASPurse(purseData: ImmutableByteArray?) {
    val autoLoadAmount: Int
    val can: ImmutableByteArray
    val cepasVersion: Byte
    val csn: ImmutableByteArray
    val issuerDataLength: Int
    val issuerSpecificData: ImmutableByteArray
    val lastCreditTransactionHeader: ImmutableByteArray
    val lastCreditTransactionTRP: Int
    val lastTransactionDebitOptionsByte: Byte
    val lastTransactionTRP: Int
    val logfileRecordCount: Byte
    val purseBalance: Int
    val purseStatus: Byte
    val isValid: Boolean
}

expect class EZLinkTransitData : TransitData {
    constructor(cepasCard: CEPASApplication)

    companion object {
        fun parseTransitIdentity(card: CEPASApplication): TransitIdentity
        fun check(cepasCard: CEPASApplication): Boolean
        val EZ_LINK_CARD_INFO: CardInfo
    }
}

