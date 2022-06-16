package au.id.micolous.metrodroid.test

import au.id.micolous.metrodroid.card.CardType
import au.id.micolous.metrodroid.card.calypso.CalypsoApplication
import au.id.micolous.metrodroid.card.calypso.CalypsoCardTransitFactory
import au.id.micolous.metrodroid.card.classic.ClassicCardTransitFactory
import au.id.micolous.metrodroid.card.desfire.DesfireCardTransitFactory
import au.id.micolous.metrodroid.card.felica.FelicaCardTransitFactory
import au.id.micolous.metrodroid.card.felica.FelicaConsts
import au.id.micolous.metrodroid.card.ksx6924.KSX6924Application
import au.id.micolous.metrodroid.key.ClassicSectorKey
import au.id.micolous.metrodroid.serializers.JsonKotlinFormat
import au.id.micolous.metrodroid.serializers.classic.MfcCardImporter
import au.id.micolous.metrodroid.transit.*
import au.id.micolous.metrodroid.transit.bilhete_unico.BilheteUnicoSPTransitData
import au.id.micolous.metrodroid.transit.easycard.EasyCardTransitData
import au.id.micolous.metrodroid.transit.ezlinkcompat.EZLinkCompatTransitData
import au.id.micolous.metrodroid.transit.hsl.HSLTransitData
import au.id.micolous.metrodroid.transit.hsl.HSLUltralightTransitData
import au.id.micolous.metrodroid.transit.hsl.HSLUltralightTransitFactory
import au.id.micolous.metrodroid.transit.mobib.MobibTransitData
import au.id.micolous.metrodroid.transit.ndef.NdefClassicTransitFactory
import au.id.micolous.metrodroid.transit.ndef.NdefData
import au.id.micolous.metrodroid.transit.ndef.NdefFelicaTransitFactory
import au.id.micolous.metrodroid.transit.ndef.NdefUltralightTransitFactory
import au.id.micolous.metrodroid.transit.opal.OpalTransitData
import au.id.micolous.metrodroid.transit.rkf.RkfLookup
import au.id.micolous.metrodroid.transit.rkf.RkfTransitData
import au.id.micolous.metrodroid.transit.selecta.SelectaFranceTransitData
import au.id.micolous.metrodroid.transit.tmoney.TMoneyTransitData
import au.id.micolous.metrodroid.transit.troika.TroikaTransitData
import au.id.micolous.metrodroid.transit.troika.TroikaUltralightTransitData
import au.id.micolous.metrodroid.ui.ListItemInterface
import au.id.micolous.metrodroid.util.Preferences
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.modules.SerializersModule
import kotlin.reflect.KClass
import kotlin.test.*

class TransitDataSerializedTest : BaseInstrumentedTest() {
    enum class InputType {
        JSON,
        MFC,
        XML
    }

    data class TestCase(
        val inputFile: String,
        val parsedFile: String?,
        val transitType: KClass<out TransitData>?,
        val cardType: CardType,
        val factory: CardTransitFactory<*>?,
        val cardInfo: CardInfo?,
        val inputType: InputType = InputType.JSON,
        val canonical: String? = null,
        val rawFile: String? = null,
        val manufFile: String? = null,
        val dynamicKeys: Set<Pair<Int, ClassicSectorKey.KeyType>> = emptySet()
    )

    @Test
    fun testAllCards(){
        setLocale("en-US")
        Preferences.showBothLocalAndEnglish = true
        Preferences.useIsoDateTimeStamps = true
        Preferences.showRawStationIds = false
        Preferences.obfuscateTripDates = false
        Preferences.obfuscateTripTimes = false
        Preferences.obfuscateTripFares = false
        for (testcase in testcases) {
            val card = when (testcase.inputType) {
                InputType.JSON -> JsonKotlinFormat.readCard(loadAsset(testcase.inputFile))
                InputType.MFC -> MfcCardImporter().readCard(loadAsset(testcase.inputFile))
                InputType.XML -> loadCardXml(testcase.inputFile)
            }
            assertNotNull(card)
            assertEquals(expected=testcase.cardType, actual=card.cardType)
            if (testcase.canonical != null)
                assertEquals(expected=jsonNoDefault.parseToJsonElement(loadSmallAssetBytes(testcase.canonical).decodeToString()), actual=JsonKotlinFormat.makeCardElement(card),
                    message="Wrong card parsing for ${testcase.inputFile}->${testcase.canonical}")
            val parsed = card.parseTransitData()
            val ti = card.parseTransitIdentity()
            if (testcase.transitType != null && testcase.parsedFile != null) {
                assertNotNull(parsed)
                val stored = TransitDataStored.Storer.store(parsed)
                assertTrue(
                    actual = testcase.transitType.isInstance(parsed),
                    message = "Transit data is of unexpected type: ${parsed::class.simpleName} instead of ${testcase.transitType.simpleName}"
                )
                val actual =
                    jsonNoDefault.encodeToJsonElement(TransitDataStored.serializer(), stored)
                val expected = jsonNoDefault.parseToJsonElement(loadSmallAssetBytes(testcase.parsedFile).decodeToString())
                assertEquals(
                    expected = expected,
                    actual = actual,
                    "Wrong transit data for ${testcase.inputFile}->${testcase.parsedFile}"
                )
                assertNotNull(ti)
                assertEquals(expected= expected.jsonObject["cardName"]?.jsonPrimitive?.content, actual = ti.name)
                assertEquals(expected= expected.jsonObject["serialNumber"]?.jsonPrimitive?.content, actual = ti.serialNumber)
            } else {
                assertNull(testcase.transitType)
                assertNull(testcase.parsedFile)
                assertNull(parsed)
                assertNull(ti)
            }
            when (testcase.cardType) {
                CardType.MifareClassic -> {
                    assertIs<ClassicCardTransitFactory>(testcase.factory)
                    val sec = testcase.factory.earlySectors
                    if (sec != -1) {
                        assertTrue(
                            testcase.factory.earlyCheck(
                                card.mifareClassic!!.sectors.subList(
                                    0,
                                    sec
                                )
                            )
                        )
                        assertEquals(testcase.factory.earlyCardInfo(
                            card.mifareClassic!!.sectors.subList(
                                0,
                                sec
                            )
                        ), testcase.cardInfo)
                    }
                    for (curSecIdx in card.mifareClassic!!.sectors.indices)
                        for (keyType in listOf(ClassicSectorKey.KeyType.UNKNOWN,
                            ClassicSectorKey.KeyType.A, ClassicSectorKey.KeyType.B)) {
                            assertEquals(
                                Pair(curSecIdx, keyType) in testcase.dynamicKeys,
                                testcase.factory.isDynamicKeys(
                                    card.mifareClassic!!.sectors.subList(0, curSecIdx),
                                    curSecIdx, keyType)
                            )
                        }
                }
                CardType.MifareUltralight -> {} // No early check for ultralight
                CardType.MifareDesfire -> {
                    assertIs<DesfireCardTransitFactory>(testcase.factory)
                    assertTrue(testcase.factory.earlyCheck(card.mifareDesfire!!.applications.keys.toIntArray()))
                    assertEquals(
                        testcase.factory.getCardInfo(card.mifareDesfire!!.applications.keys.toIntArray()),
                        testcase.cardInfo
                    )
                }
                CardType.CEPAS -> {} // Used only for old dumps, now read as ISO7816
                CardType.FeliCa -> {
                    assertIs<FelicaCardTransitFactory>(testcase.factory)
                    if (FelicaConsts.SYSTEMCODE_FELICA_LITE !in card.felica!!.systems) {
                        assertTrue(
                            testcase.factory.earlyCheck(card.felica!!.systems.keys.toList()),
                            "Failed early check for ${testcase.inputFile}"
                        )
                        assertEquals(
                            testcase.factory.getCardInfo(card.felica!!.systems.keys.toList()),
                            testcase.cardInfo)
                    }
                }
                CardType.ISO7816 -> {
                    val tmoney = card.iso7816?.applications?.firstOrNull { it is KSX6924Application }
                    val calypso = card.iso7816?.applications?.firstOrNull { it is CalypsoApplication }
                    when {
                        tmoney != null -> {}
                        calypso != null -> {
                            val factory = testcase.factory as CalypsoCardTransitFactory
                            val tenv = calypso.sfiFiles[CalypsoApplication.File.TICKETING_ENVIRONMENT.sfi]?.getRecord(1)
                            assertNotNull(tenv)
                            assertTrue(factory.check(tenv))
                            assertEquals(testcase.cardInfo, factory.getCardInfo(tenv))
                        }
                        else -> TODO()
                    }
                }
                CardType.MultiProtocol -> TODO()
                CardType.Vicinity -> TODO()
                CardType.MifarePlus -> TODO()
                CardType.Unknown -> TODO()
            }

            if (testcase.rawFile != null) {
                val actualRd = card.rawData
                assertNotNull(actualRd)
                assertEquals(expected=jsonNoDefault.parseToJsonElement(loadSmallAssetBytes(testcase.rawFile).decodeToString()),
                    actual= jsonNoDefault.encodeToJsonElement(ListSerializer(ListItemInterface.serializer()), actualRd),
                    message="Wrong raw presentation for ${testcase.inputFile}->${testcase.rawFile}")
            }

            if (testcase.manufFile != null) {
                val actualMi = card.manufacturingInfo
                assertNotNull(actualMi)
                assertEquals(expected=jsonNoDefault.parseToJsonElement(loadSmallAssetBytes(testcase.manufFile).decodeToString()),
                    actual= jsonNoDefault.encodeToJsonElement(ListSerializer(ListItemInterface.serializer()), actualMi),
                    message="Wrong manufacturing data parsing for ${testcase.inputFile}->${testcase.manufFile}")
            }
        }
    }

    companion object {
        val jsonNoDefault = Json {
            encodeDefaults = false
            serializersModule = SerializersModule {
                polymorphic(TransitBalance::class, TransitBalanceStored::class,
                    TransitBalanceStored.serializer())
            }
        }
        private val testcases = listOf(
            TestCase("7eb2258a.mfd", "parsed/7eb2258a.json", BilheteUnicoSPTransitData::class, CardType.MifareClassic, BilheteUnicoSPTransitData.FACTORY, BilheteUnicoSPTransitData.CARD_INFO, InputType.MFC, manufFile = "parsed/7eb2258a_manuf.json"),
            TestCase("7eb2258a/201111242210.dump", "parsed/7eb2258a/201111242210.json", BilheteUnicoSPTransitData::class, CardType.MifareClassic, BilheteUnicoSPTransitData.FACTORY, BilheteUnicoSPTransitData.CARD_INFO, InputType.MFC),
            TestCase("7eb2258a/201111272000.dump", "parsed/7eb2258a/201111272000.json", BilheteUnicoSPTransitData::class, CardType.MifareClassic, BilheteUnicoSPTransitData.FACTORY, BilheteUnicoSPTransitData.CARD_INFO, InputType.MFC),
            TestCase("7eb2258a/201111282115.dump", "parsed/7eb2258a/201111282115.json", BilheteUnicoSPTransitData::class, CardType.MifareClassic, BilheteUnicoSPTransitData.FACTORY, BilheteUnicoSPTransitData.CARD_INFO, InputType.MFC),
            TestCase("9e4937b0.mfd", "parsed/9e4937b0.json", BilheteUnicoSPTransitData::class, CardType.MifareClassic, BilheteUnicoSPTransitData.FACTORY, BilheteUnicoSPTransitData.CARD_INFO, InputType.MFC, manufFile = "parsed/9e4937b0_manuf.json"),
            TestCase("9e4937b0/201111241013.dump", "parsed/9e4937b0/201111241013.json", BilheteUnicoSPTransitData::class, CardType.MifareClassic, BilheteUnicoSPTransitData.FACTORY, BilheteUnicoSPTransitData.CARD_INFO, InputType.MFC),
            TestCase("9e4937b0/201111281500.dump", "parsed/9e4937b0/201111281500.json", BilheteUnicoSPTransitData::class, CardType.MifareClassic, BilheteUnicoSPTransitData.FACTORY, BilheteUnicoSPTransitData.CARD_INFO, InputType.MFC),
            TestCase("9e4937b0/201111281515.dump", "parsed/9e4937b0/201111281515.json", BilheteUnicoSPTransitData::class, CardType.MifareClassic, BilheteUnicoSPTransitData.FACTORY, BilheteUnicoSPTransitData.CARD_INFO, InputType.MFC),
            TestCase("9e4937b0/201112011015.dump", "parsed/9e4937b0/201112011015.json", BilheteUnicoSPTransitData::class, CardType.MifareClassic, BilheteUnicoSPTransitData.FACTORY, BilheteUnicoSPTransitData.CARD_INFO, InputType.MFC),
            TestCase("9e4937b0/201112151145.dump", "parsed/9e4937b0/201112151145.json", BilheteUnicoSPTransitData::class, CardType.MifareClassic, BilheteUnicoSPTransitData.FACTORY, BilheteUnicoSPTransitData.CARD_INFO, InputType.MFC),
            TestCase("9e4937b0/201112191024.dump", "parsed/9e4937b0/201112191024.json", BilheteUnicoSPTransitData::class, CardType.MifareClassic, BilheteUnicoSPTransitData.FACTORY, BilheteUnicoSPTransitData.CARD_INFO, InputType.MFC),
            TestCase("9e4937b0/201112191036.dump", "parsed/9e4937b0/201112191036.json", BilheteUnicoSPTransitData::class, CardType.MifareClassic, BilheteUnicoSPTransitData.FACTORY, BilheteUnicoSPTransitData.CARD_INFO, InputType.MFC),
            TestCase("fcd4cf1f.mfd", "parsed/fcd4cf1f.json", BilheteUnicoSPTransitData::class, CardType.MifareClassic, BilheteUnicoSPTransitData.FACTORY, BilheteUnicoSPTransitData.CARD_INFO, InputType.MFC),
            TestCase("fcd4cf1f/201111241013.dump", "parsed/fcd4cf1f/201111241013.json", BilheteUnicoSPTransitData::class, CardType.MifareClassic, BilheteUnicoSPTransitData.FACTORY, BilheteUnicoSPTransitData.CARD_INFO, InputType.MFC),
            TestCase("fcd4cf1f/201112011030.dump", "parsed/fcd4cf1f/201112011030.json", BilheteUnicoSPTransitData::class, CardType.MifareClassic, BilheteUnicoSPTransitData.FACTORY, BilheteUnicoSPTransitData.CARD_INFO, InputType.MFC),
            TestCase("easycard/deadbeef.mfc", "parsed/deadbeef.json", EasyCardTransitData::class, CardType.MifareClassic, EasyCardTransitData.FACTORY, EasyCardTransitData.CARD_INFO, InputType.MFC),
            TestCase(
                inputFile = "opal/opal-transit-litter.json",
                parsedFile = "parsed/opal-transit-litter.json",
                transitType = OpalTransitData::class,
                cardType = CardType.MifareDesfire,
                factory = OpalTransitData.FACTORY,
                cardInfo = OpalTransitData.CARD_INFO,
                manufFile = "opal/opal-transit-litter-manuf.json",
                rawFile = "opal/opal-transit-litter-raw.json"
            ),
            TestCase(
                inputFile = "opal/opal-transit-litter-auto.json",
                parsedFile = "parsed/opal-transit-litter-auto.json",
                transitType = OpalTransitData::class,
                cardType = CardType.MifareDesfire,
                factory = OpalTransitData.FACTORY,
                cardInfo = OpalTransitData.CARD_INFO,
                manufFile = "opal/opal-transit-litter-manuf.json"
            ),
            TestCase(
                inputFile = "opal/opal-transit-litter.xml",
                parsedFile = "parsed/opal-transit-litter.json",
                transitType = OpalTransitData::class,
                cardType = CardType.MifareDesfire,
                factory = OpalTransitData.FACTORY,
                cardInfo = OpalTransitData.CARD_INFO,
                inputType = InputType.XML,
                canonical = "opal/opal-transit-litter.json",
                manufFile = "opal/opal-transit-litter-manuf.json",
                rawFile = "opal/opal-transit-litter-raw.json"
            ),
            TestCase(
                inputFile = "opal/opal-transit-litter-nomanufdata.xml",
                parsedFile = "parsed/opal-transit-litter.json",
                transitType = OpalTransitData::class,
                cardType = CardType.MifareDesfire,
                factory = OpalTransitData.FACTORY,
                cardInfo = OpalTransitData.CARD_INFO,
                inputType = InputType.XML,
                manufFile = "opal/opal-transit-litter-manuf.json",
                rawFile = "opal/opal-transit-litter-raw.json"
            ),

            TestCase("hsl/hslv2.json", "parsed/hslv2.json", HSLTransitData::class, CardType.MifareDesfire, HSLTransitData.FACTORY, HSLTransitData.HSL_CARD_INFO, manufFile = "hsl/hslv2-manuf.json", rawFile = "hsl/hslv2-raw.json"),
            /*
             * Per https://github.com/mchro/RejsekortReader/blob/master/dumps/anonymt_dump-20120814.txt
             *
             * A brand new card with card number 308430 000 027 859 5
             * Purchased 2012-07-27 13:28
             * Balance is at least 80 DKK (I paid 150 DKK where the card cost 70 DKK).
             * The card has never been used.
             */
            TestCase("anonymt_dump-20120814.mfd", "parsed/anonymt_dump-20120814.json",
                RkfTransitData::class, CardType.MifareClassic, RkfTransitData.FACTORY, RkfTransitData.issuerMap[RkfLookup.REJSEKORT], InputType.MFC),
            TestCase("ndef/felicandef.json", "parsed/ndefuri.json",
                NdefData::class, CardType.FeliCa, NdefFelicaTransitFactory, NdefData.CARD_INFO),
            TestCase("ndef/felicalitendef.json", "parsed/ndefuri.json",
                NdefData::class, CardType.FeliCa, NdefFelicaTransitFactory, NdefData.CARD_INFO),
            TestCase(
                "ndef/mfcndef.json", "parsed/ndefuri.json",
                NdefData::class, CardType.MifareClassic, NdefClassicTransitFactory, NdefData.CARD_INFO
            ),
            TestCase(
                "ndef/mfcndef.xml", "parsed/ndefuri.json",
                NdefData::class, CardType.MifareClassic, NdefClassicTransitFactory, NdefData.CARD_INFO,
                InputType.XML, "ndef/mfcndefxml.json"
            ),
            TestCase(
                "ndef/ulndef.json", "parsed/ndeftxt.json",
                NdefData::class, CardType.MifareUltralight, NdefUltralightTransitFactory, NdefData.CARD_INFO
            ),
            TestCase("ndef/felicatxt.json", "parsed/ndeftxt.json",
                NdefData::class, CardType.FeliCa, NdefFelicaTransitFactory, NdefData.CARD_INFO),
            TestCase("ndef/felicawifi.json", "parsed/ndefwifi.json",
                NdefData::class, CardType.FeliCa, NdefFelicaTransitFactory, NdefData.CARD_INFO),
            TestCase("ndef/felicapkg.json", "parsed/ndefpkg.json",
                NdefData::class, CardType.FeliCa, NdefFelicaTransitFactory, NdefData.CARD_INFO),
            TestCase("cepas/legacy.json", "parsed/cepaslegacy.json",
                EZLinkCompatTransitData::class, CardType.CEPAS, null, null),
            TestCase("cepas/legacy.xml", "parsed/cepaslegacy.json",
                EZLinkCompatTransitData::class, CardType.CEPAS, null, null, InputType.XML, canonical="cepas/legacy.json"),
            TestCase("hsl/hslul.json", "parsed/hslul.json",
                HSLUltralightTransitData::class, CardType.MifareUltralight, HSLUltralightTransitFactory,
                HSLTransitData.HSL_CARD_INFO
            ),
            TestCase("troika/troikaul.json", "parsed/troikaul.json",
                TroikaUltralightTransitData::class, CardType.MifareUltralight,
                TroikaUltralightTransitData.FACTORY, TroikaTransitData.CARD_INFO),
            TestCase("tmoney/oldtmoney.json", "parsed/oldtmoney.json",
                TMoneyTransitData::class, CardType.ISO7816, TMoneyTransitData.FACTORY,
                TMoneyTransitData.CARD_INFO),
            TestCase("tmoney/oldtmoney.xml", "parsed/oldtmoney.json",
                TMoneyTransitData::class, CardType.ISO7816, TMoneyTransitData.FACTORY,
                TMoneyTransitData.CARD_INFO, InputType.XML, canonical="tmoney/oldtmoney.json"),
            TestCase("selecta/selecta.json", "parsed/selecta.json",
                SelectaFranceTransitData::class, CardType.MifareClassic,
                SelectaFranceTransitData.FACTORY, SelectaFranceTransitData.CARD_INFO),
            TestCase("mfu/blank_old.json", null, null,
                     CardType.MifareUltralight, null, null),
            TestCase(
                "mfu/blank_old.xml", null, null,
                CardType.MifareUltralight, null, null, InputType.XML
            ),
            TestCase(
                "iso7816/mobib_blank.xml", "parsed/mobib_blank.json",
                MobibTransitData::class, CardType.ISO7816, MobibTransitData.FACTORY,
                MobibTransitData.CARD_INFO, InputType.XML, "iso7816/mobib_blank.json",
                "parsed/mobib_blank_raw.json", "parsed/mobib_blank_manuf.json"
            ),
            TestCase(
                "iso7816/mobib_blank.json", "parsed/mobib_blank.json",
                MobibTransitData::class, CardType.ISO7816, MobibTransitData.FACTORY,
                MobibTransitData.CARD_INFO, InputType.JSON, null,
                "parsed/mobib_blank_raw.json", "parsed/mobib_blank_manuf.json"
            )
        )
    }
}
