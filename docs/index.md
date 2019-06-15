---
title: Metrodroid
permalink: /
layout: home
---

## Screenshots

{% include screenshots.html screenshots=site.data.index %}

[More screenshots...](https://micolous.github.io/metrodroid/screenshots)

## Supported cards / agencies

This table is from the _development_ version of Metrodroid -- some cards might not be supported yet
in the version on F-Droid or Google Play!

Card / Agency | Location | Notes
------------- | -------- | -----
[AT HOP][athop] | :new_zealand: Auckland, New Zealand | :new: `123`
[Beijing Municipal Card][beijing] | :cn: Beijing, China | :new:
[Bilhete Único][bu] | :brazil: São Paulo, Brazil | :closed_lock_with_key: `MFC`
[CharlieCard][charlie] | :us: Boston, MA, USA | :new: :closed_lock_with_key: `MFC`
City Union | :cn: Mainland China | :new:
[Clipper][clipper] | :us: San Francisco, CA, USA
[Compass][compass] | :canada: Vancouver, BC, Canada | :new: `SINGLE`
[Cubic Nextfare][nextfare] | :earth_americas: _many locations_ | :new: :closed_lock_with_key: `MFC`
[EasyCard][easycard] | :taiwan: Taipei, Taiwan | :closed_lock_with_key: `MFC`
[Edy][edy] | :jp: Japan
[Envibus][envibus] | :fr: Sophia Antipolis, France | :new:
[ERG][erg] | :earth_asia: _many locations_ | :new: :closed_lock_with_key: `MFC`
[EZ-Link][ezlink] | :singapore: Singapore |
[Go card][seqgo] | :australia: Brisbane and South East Queensland, Australia | :new: :closed_lock_with_key: `MFC` [Note][seqgonote]
[Go-to card][mspgoto] | :us: Minneapolis-St. Paul, MN, USA | :new: :closed_lock_with_key: `MFC`
[Hop Fastpass][hfp] | :us: Portland, OR, USA | :new: `123`
[HSL][hsl], [Matkakortti][matka] | :finland: Finland |
[IstanbulKart][istanbul] | :tr: Istanbul, Turkey | :new: `123`
[Kartu Multi Trip][kmt] | :indonesia: Jakarta, Indonesia | `KMT`
Kiev Metro | :ukraine: Kiev, Ukraine | :new: :closed_lock_with_key: `MFC`
Krasnodar | :ru: Krasnodar, Russia | :new: :closed_lock_with_key: `MFC`
[Leap][leap] | :ireland: Ireland | :new: :unlock:
[Lisboa Viva][lisboa] | :portugal: Lisbon, Portugal
[Manly Fast Ferry][manly] | :australia: Sydney, NSW, Australia | :new: :closed_lock_with_key: `MFC`
[Metrocard][adl] | :australia: Adelaide, SA, Australia | :new:
[Metrocard][chc] | :new_zealand: Christchurch, New Zealand | :new: :closed_lock_with_key: `MFC`
[Mobib][mobib] | :belgium: Brussels, Belgium | :new:
[Myki][myki] | :australia: Melbourne (and surrounds), VIC, Australia | :new: `123` [Note][mykinote]
[MyWay][myway] | :australia: Australian Capital Territory, Australia | :new: :closed_lock_with_key: `MFC`
[Navigo][navigo] | :fr: Paris, France | :new:
[NETS FlashPay][nets] | :singapore: Singapore |
[Octopus][octopus] | :hong_kong: Hong Kong | :new:
[Opal][opal] | :australia: Sydney (and surrounds), NSW, Australia | :new:
[Opus][opus] | :canada: Québec, Canada | :new:
[ORCA][orca] | :us: Seattle, WA, USA |
[OùRA][oura] | :fr: Grenoble, France | :new:
[OV-chipkaart][ovc] | :netherlands: Netherlands | :closed_lock_with_key: `MFC`
[Oyster][oyster] | :gb: London, United Kingdom | :closed_lock_with_key: `MFC`
[Podorozhnik][podoro] | :ru: Saint Petersburg, Russia | :new: :closed_lock_with_key: `MFC`
[Rav-Kav][ravkav] | :israel: Israel | :new:
[Rejsekort][rejse] | :norway: Norway | :new: :closed_lock_with_key: `MFC`
[RicaricaMi][ricarica] | :it: Milan, Italy | :new: :closed_lock_with_key: `MFC`
Selecta | :fr: France | :new:
[Shenzhen Tong][shenzhen] | :cn: Shenzhen, Guangdong Province, China | :new:
[SLAccess][slaccess] | :sweden: Stockholm, Sweden | :new: :closed_lock_with_key: `MFC`
[SmartRider][smartrider] | :australia: Western Australia, Australia | :new: :closed_lock_with_key: `MFC`
[Strelka][strelka] | :ru: Moscow, Russia | :closed_lock_with_key: `123` `MFC`
[Suica][suica], [ICOCA][icoca], [PASMO][pasmo] | :jp: Japan
[SunCard][suncard] | :us: Orlando, FL, USA | :new: :closed_lock_with_key: `123` `MFC`
[TaM][tam] | :fr: Montpellier, France | :new:
[Tartu Bus][tartu] | :estonia: Tartu, Estonia | :new: `123` `MFC`
[T-Money][tmoney] | :kr: South Korea | :new:
T-Union | :cn: Mainland China | :new:
[TransGironde][gironde] | :fr: Gironde, France | :new:
[Transit Access Pass][laxtap] | :us: Los Angeles, CA, USA | :new: :closed_lock_with_key: `MFC`
[Troika][troika] | :ru: Moscow, Russia | :new: :closed_lock_with_key: `MFC`
[Ventra][ventra] | :us: Chicago, IL, USA | :new: `SINGLE`
[Wuhan Tong][wuhan] | :cn: Wuhan, Hubei Province, China | :new:
Yaroslavl ETK | :ru: Yaroslavl, Russia | :new: :closed_lock_with_key: `MFC`
[Zolotaya Korona][zolotaya] | :ru: _multiple cities in Russia_ | :new: :closed_lock_with_key: `MFC`

Note | Meaning
---- | -------
:new: | New in Metrodroid.
:closed_lock_with_key: | Encryption keys required to read this card.
:unlock: | Encryption keys are downloaded from the operator.
`MFC` | MIFARE Classic card; requires NXP NFC chipset in your phone.
`123` | Only the card number can be read.
`FALLBACK` | Fallback reader -- must be explicitly enabled in the application's settings.
`KMT` | Only new FeliCa-based cards can be read.
`SINGLE` | Only single-use tickets can be read.

This project **will only read data from the card itself**, without having to
connect to the agency's back-office systems. In some cases, limited data is
available, so balance information and trip history might not be available.

Note: The Leap card reader connects to Transport for Ireland's server for
challenge-response authentication with the card. The data is otherwise
interpreted locally, and _connectivity is disabled by default._

## Supported card media

* [FeliCa][felica]
* [FeliCa Lite][felica]
* ISO 7816-4
  * [Calypso][calypso]
  * [CEPAS][cepas]
  * [T-Money][tmoney]
* [MIFARE Classic][mfc] (Not compatible with all devices)
* [MIFARE DESFire][mfd]
* [MIFARE Ultralight][mfu] (Not compatible with all devices)

[mykinote]: https://micolous.github.io/metrodroid/myki
[seqgonote]: https://micolous.github.io/metrodroid/seqgo

[calypso]: http://www.calypsotechnology.net/
[cepas]: https://en.wikipedia.org/wiki/CEPAS
[felica]: https://en.wikipedia.org/wiki/FeliCa
[mfc]: https://en.wikipedia.org/wiki/MIFARE#MIFARE_Classic
[mfd]: https://en.wikipedia.org/wiki/MIFARE#MIFARE_DESFire
[mfu]: https://en.wikipedia.org/wiki/MIFARE#MIFARE_Ultralight_and_MIFARE_Ultralight_EV1

[adl]: https://adelaidemetro.com.au/Tickets-fares/metroCARD
[athop]: https://at.govt.nz/bus-train-ferry/at-hop-card/
[beijing]: https://en.wikipedia.org/wiki/Yikatong
[bu]: http://bilheteunico.sptrans.com.br/
[charlie]: https://www.mbta.com/fares/charliecard
[chc]: http://www.metroinfo.co.nz/
[clipper]: https://www.clippercard.com/
[compass]: https://www.compasscard.ca/
[easycard]: https://www.easycard.com.tw/en/
[edy]: https://en.wikipedia.org/wiki/Edy
[envibus]: http://www.envibus.fr/
[erg]: https://github.com/micolous/metrodroid/wiki/ERG-MFC
[ezlink]: http://www.ezlink.com.sg/
[gironde]: https://www.transgironde.fr/
[hfp]: https://myhopcard.com/
[hsl]: http://www.hsl.fi/EN/
[icoca]: https://en.wikipedia.org/wiki/ICOCA
[istanbul]: https://www.istanbulkart.istanbul/
[kmt]: https://en.wikipedia.org/wiki/Kereta_Commuter_Indonesia
[laxtap]: https://www.taptogo.net/
[leap]: https://www.leapcard.ie/
[lisboa]: https://www.portalviva.pt/
[manly]: http://www.manlyfastferry.com.au/
[matka]: http://www.hsl.fi/EN/passengersguide/travelcard/Pages/default.aspx
[mobib]: https://mobib.be/
[mspgoto]: https://www.metrotransit.org/go-to-card
[myki]: http://ptv.vic.gov.au/
[myway]: https://www.transport.act.gov.au/myway-and-fares
[navigo]: http://www.navigo.fr/
[nets]: http://www.netsflashpay.com.sg/
[nextfare]: https://github.com/micolous/metrodroid/wiki/Cubic-Nextfare-MFC
[octopus]: http://www.octopus.com.hk/home/en/index.html
[opal]: http://www.opal.com.au/
[opus]: http://www.stm.info/en/info/fares/opus-cards-and-other-fare-media/opus-card
[orca]: http://www.orcacard.com/
[oura]: https://www.oura.com/
[ovc]: http://www.ov-chipkaart.nl/
[oyster]: https://oyster.tfl.gov.uk/
[pasmo]: https://en.wikipedia.org/wiki/PASMO
[podoro]: http://podorozhnik.spb.ru/en/
[ravkav]: https://www.rail.co.il/en/ravkav/Pages/default.aspx
[rejse]: https://www.rejsekort.dk/
[ricarica]: https://www.atm.it/en/ViaggiaConNoi/Biglietti/Pages/TesseraRIcaricaMI.aspx
[seqgo]: http://translink.com.au/tickets-and-fares/go-card
[shenzhen]: http://www.shenzhentong.com/
[slaccess]: https://sl.se/en/eng-info/fares/sl-access/
[smartrider]: http://www.transperth.wa.gov.au/SmartRider/
[strelka]: https://strelkacard.ru/
[suica]: https://en.wikipedia.org/wiki/Suica
[suncard]: https://sunrail.com/tickets-suncards/suncards/
[tam]: http://www.tam-voyages.com/
[tartu]: https://www.tartu.ee/en/tartu-bus-card
[tmoney]: https://www.t-money.co.kr/
[troika]: http://troika.mos.ru/
[ventra]: https://www.ventrachicago.com/
[wuhan]: http://www.whcst.com/
[zolotaya]: https://en.wikipedia.org/wiki/Zolotaya_Korona

