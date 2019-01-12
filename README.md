# ![logo](https://github.com/micolous/metrodroid/raw/master/src/main/res/mipmap-hdpi/ic_launcher.png) [Metrodroid](https://github.com/micolous/metrodroid)

[![Translation status](https://hosted.weblate.org/widgets/metrodroid/-/svg-badge.svg)][weblate] [![Build Status](https://travis-ci.org/micolous/metrodroid.svg?branch=master)][travis]

<a href="https://f-droid.org/repository/browse/?fdid=au.id.micolous.farebot" target="_blank">
<img src="https://f-droid.org/badge/get-it-on.png" height="80"/></a>
<a href="https://play.google.com/store/apps/details?id=au.id.micolous.farebot" target="_blank">
<img src="https://play.google.com/intl/en_us/badges/images/generic/en-play-badge.png" height="80"/></a>
<a href="https://github.com/micolous/metrodroid/releases">Direct APK download</a>

Version: 2.9.38

View your remaining balance, recent trips, and other information from contactless public transit
cards using your NFC-enabled Android device!

* [Michael Farrell](https://github.com/micolous)

I presented Metrodroid and the work I did on supporting a number of Australian agencies' cards at
linux.conf.au 2018, in my talk, [Tap on to reverse engineering](https://youtu.be/qVvNdfKRw7M).

## Thanks to

* [Eric Butler][5] (Farebot, on which this project is based)
* [Karl Koscher][3] (ORCA)
* [Sean Cross][4] (CEPAS/EZ-Link)
* Anonymous Contributor (Clipper)
* [nfc-felica][13] and [IC SFCard Fan][14] projects (Suica)
* [Chris Norden](https://github.com/cnorden) (Edy)
* [Wilbert Duijvenvoorde](https://github.com/wandcode) (MIFARE Classic/OV-chipkaart)
* [tbonang](https://github.com/tbonang) (NETS FlashPay)
* [Marcelo Liberato](https://github.com/mliberato) (Bilhete Único)
* [Lauri Andler](https://github.com/landler/) (HSL)
* [Michael](https://github.com/micolous/) (Opal, Manly Fast Ferry, Go card, Myki, Octopus, Cubic Nextfare, LAX TAP, SmartRider, MyWay, MIFARE Ultralight, ERG, Christchurch Metrocard)
* [Steven Steiner](https://github.com/steets250) (LAX TAP)
* [Rob O'Regan](http://www.robx1.net/nswtkt/private/manlyff/manlyff.htm) (Manly Fast Ferry card image)
* [The Noun Project][15] (Various icons)
* [Vladimir Serbinenko](https://github.com/phcoder) (Podorozhnik, Troika, Compass, Shenzhen Tong, Rav-Kav, T-money, Leap, CharlieCard, EN1545, Intercode, IstanbulKart, Lisboa Viva, Mobib, RicaricaMi, Chinese cards, Adelaide Metrocard, Rejesekort)
* [Toomas Losin](http://www.lenrek.net) (Compass)
* [Sinpo Lib](https://github.com/sinpolib) (Shenzhen Tong)
* Maria Komar (Podorozhnik database and dumps)
* [Bondan](https://github.com/sybond) [Sumbodo](http://sybond.web.id) (Kartu Multi Trip, COMMET)

## Translated by

Thanks to those who have [helped to make Metrodroid available in languages other than English][22]:

* Dutch: [Vistaus](https://github.com/Vistaus)
* Finnish: Lari Oesch, Lauri Andler
* French: [Albirew](https://github.com/Albirew)
* Hebrew: [Steven Steiner](https://github.com/steets250)
* Indonesian: [Bondan](https://github.com/sybond) [Sumbodo](http://sybond.web.id)
* Japanese: [naofum](https://github.com/naofum), [Chris Norden](https://github.com/cnorden), [Eric Butler][5]
* Kabyle: [belkacem77](https://github.com/belkacem77), [aqvayli](https://github.com/aqvayli)
* Norwegian Bokmål: [comradekingu](https://github.com/comradekingu), Petter Reinholdtsen
* Russian: [Vladimir Serbinenko](https://github.com/phcoder)
* Spanish: [NokisDemox](https://github.com/NokisDemox)
* Turkish: [omersiar](https://github.com/omersiar)

## Supported card protocols

* [FeliCa][felica]
* [FeliCa Lite][felica]
* ISO 7816-4
  * [Calypso][calypso]
  * [CEPAS][cepas]
  * [T-Money][tmoney]
* [MIFARE Classic][mfc] (Not compatible with all devices)
* [MIFARE DESFire][mfd]
* [MIFARE Ultralight][mfu] (Not compatible with all devices)

## Supported cards / agencies

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
[Go card][seqgo] | :australia: Brisbane and South East Queensland, Australia | :new: :closed_lock_with_key: `MFC`
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
[Myki][myki] | :australia: Melbourne (and surrounds), VIC, Australia | :new: `123`
[MyWay][myway] | :australia: Australian Capital Territory, Australia | :new: :closed_lock_with_key: `MFC`
[Navigo][navigo] | :fr: Paris, France | :new:
[NETS FlashPay][nets] | :singapore: Singapore |
[Octopus][octopus] | :hong_kong: Hong Kong | :new:
[Opal][opal] | :australia: Sydney (and surrounds), NSW, Australia | :new:
[Opus][opus] | :canada: Québec, Canada | :new:
[ORCA][orca] | :us: Seattle, WA, USA |
[OùRA][oura] | :fr: Grenoble, France | :new:
[OV-chipkaart][ovc] | :netherlands: Netherlands | :closed_lock_with_key: `MFC`
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

## Supported Phones

Metrodroid requires an Android phone running 4.1 or later, with NFC support.

Some devices do not support MIFARE Classic.  MIFARE Classic is not an NFC-compliant card format, so can only be read with phones with NXP chipsets.

## License and copyright

Copyright 2015 – 2018 Michael Farrell

Copyright 2011 – 2013 Eric Butler

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.

This program contains Leaflet, a JavaScript mapping library, licensed under the BSD license.

This program contains nfc-felica-lib, a library for communicating with Sony FeliCa cards, licensed under the Apache 2.0 license.

This software and it's authors are not associated with any public transit agency.  Pictures of supported cards, including their logos, are included with this software package for the purposes of identifying cards.

## Building / hacking on this software

1. Clone the repository including submodules:

   ```
   $ git clone --recursive https://github.com/micolous/metrodroid.git
   ```
   
   [If you get an error from Gradle about `:nfc-felica-lib` not being available, then your clone doesn't have the submodules.](https://github.com/micolous/metrodroid/issues/32)

2. Import the directory into Android Studio.

   Android Studio will prompt you to install the appropriate SDK version, build tools, and Gradle.

[3]: https://twitter.com/supersat
[4]: https://twitter.com/xobs
[5]: https://twitter.com/codebutler
[13]: http://code.google.com/p/nfc-felica/
[14]: http://www014.upp.so-net.ne.jp/SFCardFan/
[15]: http://www.thenounproject.com/
[weblate]: https://hosted.weblate.org/engage/metrodroid/
[travis]: https://travis-ci.org/micolous/metrodroid

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
