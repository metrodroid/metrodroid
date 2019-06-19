package au.id.micolous.metrodroid.transit.intercode

import au.id.micolous.metrodroid.time.MetroTimeZone
import au.id.micolous.metrodroid.transit.CardInfo
import au.id.micolous.metrodroid.transit.TransitCurrency
import au.id.micolous.metrodroid.transit.en1545.En1545Lookup

interface IntercodeLookup : En1545Lookup {
    val cardInfo: CardInfo?

    override val timeZone: MetroTimeZone
        get() = MetroTimeZone.PARIS

    override fun parseCurrency(price: Int) = TransitCurrency.EUR(price)
}