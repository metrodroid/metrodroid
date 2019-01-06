package au.id.micolous.metrodroid.card.classic

import au.id.micolous.metrodroid.MetrodroidApplication
import au.id.micolous.metrodroid.transit.smartrider.SmartRiderTransitData
import org.jetbrains.annotations.NonNls

private val SMARTRIDER = listOf("myway", "smartrider")

internal class FallbackFactory : ClassicCardTransitFactory {
    val fallback
        @NonNls
        get () = MetrodroidApplication.getMfcFallbackReader()

    override fun check(classicCard: ClassicCard) =
            fallback in SMARTRIDER

    override fun parseTransitIdentity(classicCard: ClassicCard) = when (fallback) {
        // This has a proper check now, but is included for legacy reasons.
        //
        // Before the introduction of key-based detection for these cards, Metrodroid did
        // not record the key inside the ClassicCard XML structure.
        in SMARTRIDER -> SmartRiderTransitData.FACTORY.parseTransitIdentity(classicCard)
        else -> null
    }

    override fun parseTransitData(classicCard: ClassicCard) = when (fallback) {
        // This has a proper check now, but is included for legacy reasons.
        //
        // Before the introduction of key-based detection for these cards, Metrodroid did
        // not record the key inside the ClassicCard XML structure.
        in SMARTRIDER -> SmartRiderTransitData.FACTORY.parseTransitData(classicCard)
        else -> null
    }
}
