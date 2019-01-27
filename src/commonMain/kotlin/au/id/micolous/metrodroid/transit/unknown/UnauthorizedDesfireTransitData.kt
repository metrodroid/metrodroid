package au.id.micolous.metrodroid.transit.unknown

import au.id.micolous.metrodroid.card.desfire.DesfireCard
import au.id.micolous.metrodroid.card.desfire.DesfireCardTransitFactory
import au.id.micolous.metrodroid.card.desfire.files.UnauthorizedDesfireFile
import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.transit.TransitIdentity

/**
 * Handle MIFARE DESFire with no open sectors
 */
@Parcelize
class UnauthorizedDesfireTransitData (override val cardName: String): UnauthorizedTransitData() {
    companion object {
        val FACTORY: DesfireCardTransitFactory = object : DesfireCardTransitFactory() {
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
        }

        private val TYPES = mapOf(
                0x31594f to "Oyster",
                0x425301 to "Thailand BEM",
                0x5011f2 to "Lítačka",
                0x5010f2 to "Metrocard (Christchurch)")

        private fun getName(card: DesfireCard): String {
            for ((k, v) in TYPES) {
                if (card.getApplication(k) != null)
                    return v
            }
            return Localizer.localizeString(R.string.locked_mfd_card)
        }
    }

}
