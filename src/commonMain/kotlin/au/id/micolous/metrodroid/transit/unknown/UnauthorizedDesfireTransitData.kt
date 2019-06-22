package au.id.micolous.metrodroid.transit.unknown

import au.id.micolous.metrodroid.card.desfire.DesfireCard
import au.id.micolous.metrodroid.card.desfire.DesfireCardTransitFactory
import au.id.micolous.metrodroid.card.desfire.files.UnauthorizedDesfireFile
import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.transit.CardInfo
import au.id.micolous.metrodroid.transit.TransitIdentity

/**
 * Handle MIFARE DESFire with no open sectors
 */
@Parcelize
class UnauthorizedDesfireTransitData (override val cardName: String): UnauthorizedTransitData() {
    companion object {
        val FACTORY: DesfireCardTransitFactory = object : DesfireCardTransitFactory {
            override fun earlyCheck(appIds: IntArray) = false

            /**
             * This should be the last executed MIFARE DESFire check, after all the other checks are done.
             *
             *
             * This is because it will catch others' cards.
             *
             * @param card Card to read.
             * @return true if all sectors on the card are locked.
             */
            override fun check(card: DesfireCard) = card.applications.values.all {
                app -> app.interpretedFiles.values.all { it is UnauthorizedDesfireFile } }

            override fun parseTransitData(card: DesfireCard) =
                    UnauthorizedDesfireTransitData(getName(card))

            override fun parseTransitIdentity(card: DesfireCard)
                    = TransitIdentity(getName(card), null)

            override val hiddenAppIds: List<Int>
                get() = List(32) { 0x425300 + it }

            override val allCards get() = emptyList<CardInfo>() // Not worth bragging about
        }

        private data class UnauthorizedType(
                val appId: Int,
                val name: String
        )

        private val TYPES = listOf(
                UnauthorizedType(0x31594f, "Oyster"),
                UnauthorizedType(0x425311, "Thailand BEM"),
                UnauthorizedType(0x425303, "Rabbit Card"),
                UnauthorizedType(0x5011f2, "Lítačka"),
                UnauthorizedType(0x5010f2, "Metrocard (Christchurch)")
        )

        private fun getName(card: DesfireCard): String {
            for ((appId, name) in TYPES) {
                if (card.getApplication(appId) != null)
                    return name
            }
            return Localizer.localizeString(R.string.locked_mfd_card)
        }
    }

}
