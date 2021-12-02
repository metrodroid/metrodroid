/*
 * ISO7816Card.kt
 *
 * Copyright 2018 Michael Farrell <micolous+git@gmail.com>
 * Copyright 2018-2019 Google
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package au.id.micolous.metrodroid.card.iso7816

/**
 * Generic card implementation for ISO7816. This doesn't have many smarts, but dispatches to other
 * readers.
 */

import au.id.micolous.metrodroid.card.CardLostException
import au.id.micolous.metrodroid.card.CardProtocol
import au.id.micolous.metrodroid.card.CardTransceiver
import au.id.micolous.metrodroid.card.TagReaderFeedbackInterface
import au.id.micolous.metrodroid.card.calypso.CalypsoApplication
import au.id.micolous.metrodroid.card.cepas.CEPASApplication
import au.id.micolous.metrodroid.card.china.ChinaCard
import au.id.micolous.metrodroid.card.emv.EmvFactory
import au.id.micolous.metrodroid.card.ksx6924.KROCAPConfigDFApplication
import au.id.micolous.metrodroid.card.ksx6924.KSX6924Application
import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.multi.Log
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.serializers.MultiTypeSerializer
import au.id.micolous.metrodroid.transit.TransitData
import au.id.micolous.metrodroid.transit.TransitIdentity
import au.id.micolous.metrodroid.transit.china.BeijingTransitData
import au.id.micolous.metrodroid.ui.ListItem
import au.id.micolous.metrodroid.ui.ListItemRecursive

/**
 * Generic card implementation for ISO7816. This doesn't have many smarts, but dispatches to other
 * readers.
 */

import kotlinx.serialization.Serializable

object ISO7816AppSerializer : MultiTypeSerializer<ISO7816Application>() {
    private val klasses =
            (ISO7816Card.factories.flatMap {
                it.typeMap.entries.map { (k,v) -> k to v} }
                    + (CEPASApplication.TYPE to CEPASApplication.serializer())).toMap()

    override val name: String
        get() = "au.id.micolous.metrodroid.card.iso7816.ISO7816Application"

    override fun obj2serializer(obj: ISO7816Application) =
            Pair(obj.type, klasses.getValue(obj.type))

    override fun str2serializer(name: String) = klasses.getValue(name)
}

@Serializable
data class ISO7816Card (
        val applications: List<ISO7816Application>,
        override val isPartialRead: Boolean = false) :
        CardProtocol() {

    override fun parseTransitIdentity(): TransitIdentity? {
        for (app in applications) {
            val id = app.parseTransitIdentity(this)
            if (id != null)
                return id
        }
        return null
    }

    override fun parseTransitData(): TransitData? {
        for (app in applications) {
            val d = app.parseTransitData(this)
            if (d != null)
                return d
        }
        return null
    }

    override val manufacturingInfo get(): List<ListItem>? {
        val manufacturingInfo = applications.mapNotNull { it.manufacturingInfo }.flatten()
        return manufacturingInfo.ifEmpty { null }
    }

    override val rawData get(): List<ListItem> {
        val rawData = mutableListOf<ListItem>()
        for (app in applications) {
            val appName = app.appName
            val appTitle = when {
                appName == null -> "unspecified"
                appName.isASCII() -> appName.readASCII()
                else -> appName.toHexString()
            }
            val rawAppData = mutableListOf<ListItem>()
            val appData = app.appFci
            if (appData != null)
                rawAppData.add(ListItemRecursive(
                        R.string.app_fci, null, ISO7816TLV.infoWithRaw(
                        appData)))
            rawAppData.addAll(app.rawFiles)
            val extra = app.rawData
            if (extra != null)
                rawAppData.addAll(extra)
            rawData.add(ListItemRecursive(Localizer.localizeString(R.string.application_title_format,
                    appTitle), null, rawAppData))
        }
        return rawData
    }

    val size get(): Int = applications.size

    companion object {
        private const val TAG = "ISO7816Card"
        val factories = listOf(
                CalypsoApplication.FACTORY,
                KROCAPConfigDFApplication.FACTORY,
                KSX6924Application.FACTORY,
                ChinaCard.FACTORY,
		EmvFactory())

        /**
         * Dumps a ISO7816 tag in the field.
         *
         * @param tech Tag to dump.
         * @return ISO7816Card of the card contents. Returns null if an unsupported card is in the
         * field.
         * @throws Exception On communication errors.
         */
        fun dumpTag(tech: CardTransceiver,
                            feedbackInterface: TagReaderFeedbackInterface,
                            coreNFC: Boolean = false): ISO7816Card {
            var partialRead = false
            val apps = mutableListOf<ISO7816Application>()

            try {
                val iso7816Tag = ISO7816Protocol(tech)

                feedbackInterface.updateStatusText(Localizer.localizeString(R.string.iso7816_probing))
                feedbackInterface.updateProgressBar(0, 1)

                /*
             * It's tempting to try to iterate over the apps on the card.
             * Unfortunately many cards don't reply to iterating requests
             *
             */

                // CEPAS specification makes selection by AID optional. I couldn't find an AID that
                // works on my cards. But CEPAS needs to have CEPAS app implicitly selected,
                // so try selecting its main file
                // So this needs to be before selecting any real application as selecting APP by AID
                // may deselect default app
                if (!coreNFC) {
                    val cepas = CEPASApplication.dumpTag(iso7816Tag,
                    ISO7816ApplicationMutableCapsule(
                        appName = null, appFci = null),
                        feedbackInterface)
                     if (cepas != null)
                        apps.add(cepas)
                }

                for (factory in factories) {
                    // CoreNFC has some bug which prevents
                    // entitlement for Beijing cards to work
                    if (coreNFC && !factory.fixedAppIds)
                        continue
                    for (appId in factory.applicationNames) {
                        if (coreNFC && appId in BeijingTransitData.FACTORY.appNames)
                          continue
                        val appFci = iso7816Tag.selectByNameOrNull(appId) ?: continue

                        val app = factory.dumpTag(
                                iso7816Tag, ISO7816ApplicationMutableCapsule(
                                appFci = appFci, appName = appId),
                                feedbackInterface, apps.map { it.appName }) ?: continue

                        apps.addAll(app)

                        if (factory.stopAfterFirstApp) {
                            break
                        }
                    }
                }
            } catch (ex: CardLostException) {
                Log.w(TAG, "tag lost", ex)
                partialRead = true
            }

            return ISO7816Card(apps, partialRead)
        }
    }
}
