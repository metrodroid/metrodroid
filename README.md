# ![logo](https://github.com/micolous/metrodroid/raw/master/src/main/res/mipmap-hdpi/ic_launcher.png) [Metrodroid](https://github.com/micolous/metrodroid)

[![Translation status](https://hosted.weblate.org/widgets/metrodroid/-/svg-badge.svg)][22] [![Build Status](https://travis-ci.org/micolous/metrodroid.svg?branch=master)](https://travis-ci.org/micolous/metrodroid)

<a href="https://f-droid.org/repository/browse/?fdid=au.id.micolous.farebot" target="_blank">
<img src="https://f-droid.org/badge/get-it-on.png" height="80"/></a>
<a href="https://play.google.com/store/apps/details?id=au.id.micolous.farebot" target="_blank">
<img src="https://play.google.com/intl/en_us/badges/images/generic/en-play-badge.png" height="80"/></a>
<a href="https://github.com/micolous/metrodroid/releases">Direct APK download</a>

Version: 2.9.36

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
* [Wilbert Duijvenvoorde](https://github.com/wandcode) (MIFARE Classic/OV-chipkaart)
* [tbonang](https://github.com/tbonang) (NETS FlashPay)
* [Marcelo Liberato](https://github.com/mliberato) (Bilhete Único)
* [Lauri Andler](https://github.com/landler/) (HSL)
* [Michael](https://github.com/micolous/) (Opal, Manly Fast Ferry, Go card, Myki, Octopus, Cubic Nextfare, LAX TAP, SmartRider, MyWay, MIFARE Ultralight, ERG, Christchurch Metrocard)
* [Steven Steiner](https://github.com/steets250) (LAX TAP)
* [Rob O'Regan](http://www.robx1.net/nswtkt/private/manlyff/manlyff.htm) (Manly Fast Ferry card image)
* [The Noun Project][15] (Various icons)
* [Vladimir Serbinenko](https://github.com/phcoder) (Podorozhnik, Troika, Compass, Shenzhen Tong, Rav-Kav, T-money)
* [Toomas Losin](http://www.lenrek.net) (Compass)
* [Sinpo Lib](https://github.com/sinpolib) (Shenzhen Tong)
* Maria Komar (Podorozhnik database and dumps)
* [Bondan](https://github.com/sybond) [Sumbodo](http://sybond.web.id) (Kartu Multi Trip, COMMET)

## Translated by

Thanks to those who have [helped to make Metrodroid available in languages other than English][22]:

* Dutch: [Vistaus](https://github.com/Vistaus)
* Finnish: Lari Oesch
* French: [Albirew](https://github.com/Albirew)
* Hebrew: [Steven Steiner](https://github.com/steets250)
* Japanese: [naofum](https://github.com/naofum)
* Indonesian: [Bondan](https://github.com/sybond) [Sumbodo](http://sybond.web.id)
* Kabyle: [belkacem77](https://github.com/belkacem77), [aqvayli](https://github.com/aqvayli)
* Norwegian Bokmål: [comradekingu](https://github.com/comradekingu)
* Russian: [Vladimir Serbinenko](https://github.com/phcoder)
* Turkish: [omersiar](https://github.com/omersiar)

## Supported card protocols

* [Calypso][36]
* [CEPAS][2]
* [FeliCa][8]
* [MIFARE Classic][23] (Not compatible with all devices)
* [MIFARE DESFire][6]
* [MIFARE Ultralight][24] (Not compatible with all devices)

## Supported cards / agencies

Card / Agency | Location | Notes
------------- | -------- | -----
[Bilhete Único][30] | :brazil: São Paulo, Brazil | :closed_lock_with_key: `MFC` `FALLBACK`
[Clipper][1] | :us: San Francisco, CA, USA
[Compass][41] | :canada: Vancouver, BC, Canada | :new: `SINGLE`
[Cubic Nextfare][33] | :earth_americas: _many locations_ | :new: :closed_lock_with_key: `MFC`
[Edy][12] | :jp: Japan
[ERG][35] | :earth_asia: _many locations_ | :new: :closed_lock_with_key: `MFC`
[EZ-Link][7] | :singapore: Singapore |
[Go card][20] | :australia: Brisbane and South East Queensland, Australia | :new: :closed_lock_with_key: `MFC`
[Kartu Multi Trip][39] | :indonesia: Jakarta, Indonesia | `KMT`
[Manly Fast Ferry][19] | :australia: Sydney, NSW, Australia | :new: :closed_lock_with_key: `MFC`
[Matkakortti][16], [HSL][17] | :finland: Finland |
[Metrocard][34] | :new_zealand: Christchurch, New Zealand | :new: :closed_lock_with_key: `MFC`
[Myki][21] | :australia: Melbourne (and surrounds), VIC, Australia | :new: `123`
[MyWay][28] | :australia: Australian Capital Territory, Australia | :new: :closed_lock_with_key: `MFC`
[NETS FlashPay][31] | :singapore: Singapore | 
[Octopus][25] | :hong_kong: Hong Kong | :new:
[Opal][18] | :australia: Sydney (and surrounds), NSW, Australia | :new:
[Opus][40] | :canada: Montréal, QC, Canada | :new:
[ORCA][0] | :us: Seattle, WA, USA |
[OV-chipkaart][32] | :netherlands: Netherlands | :closed_lock_with_key: `MFC`
[Podorozhnik][37] | :ru: Saint Petersburg, Russia | :new: :closed_lock_with_key: `MFC`
[Rav-Kav][42] | :israel: Israel | :new:
[Shenzhen Tong][27] | :cn: Shenzhen, Guangdong Province, China | :new:
[SmartRider][29] | :australia: Western Australia, Australia | :new: :closed_lock_with_key: `MFC`
[Suica][9], [ICOCA][10], [PASMO][11] | :jp: Japan
[T-Money][43] | :kr: South Korea | :new:
[Transit Access Pass][26] | :us: Los Angeles, CA, USA | :new: :closed_lock_with_key: `MFC`
[Troika][38] | :ru: Moscow, Russia | :new: :closed_lock_with_key: `MFC`

Note | Meaning
---- | -------
:new: | New in Metrodroid.
:closed_lock_with_key: | Encryption keys required to read this card.
`MFC` | MIFARE Classic card; requires NXP NFC chipset in your phone.
`123` | Only the card number can be read.
`FALLBACK` | Fallback reader -- must be explicitly enabled in the application's settings.
`KMT` | Only new FeliCa-based cards can be read.
`SINGLE` | Only single-use tickets can be read.

This project **will only read data from the card itself**, without having to connect to the agency's back-office systems. In some cases, limited data is available, so balance information and trip history might not be available.

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

[0]: http://www.orcacard.com/
[1]: https://www.clippercard.com/
[2]: https://en.wikipedia.org/wiki/CEPAS
[3]: https://twitter.com/supersat
[4]: https://twitter.com/xobs
[5]: https://twitter.com/codebutler
[6]: https://en.wikipedia.org/wiki/MIFARE#MIFARE_DESFire
[7]: http://www.ezlink.com.sg/
[8]: https://en.wikipedia.org/wiki/FeliCa
[9]: https://en.wikipedia.org/wiki/Suica
[10]: https://en.wikipedia.org/wiki/ICOCA
[11]: https://en.wikipedia.org/wiki/PASMO
[12]: https://en.wikipedia.org/wiki/Edy
[13]: http://code.google.com/p/nfc-felica/
[14]: http://www014.upp.so-net.ne.jp/SFCardFan/
[15]: http://www.thenounproject.com/
[16]: http://www.hsl.fi/EN/passengersguide/travelcard/Pages/default.aspx
[17]: http://www.hsl.fi/EN/
[18]: http://www.opal.com.au/
[19]: http://www.manlyfastferry.com.au/
[20]: http://translink.com.au/tickets-and-fares/go-card
[21]: http://ptv.vic.gov.au/
[22]: https://hosted.weblate.org/engage/metrodroid/
[23]: https://en.wikipedia.org/wiki/MIFARE#MIFARE_Classic
[24]: https://en.wikipedia.org/wiki/MIFARE#MIFARE_Ultralight_and_MIFARE_Ultralight_EV1
[25]: http://www.octopus.com.hk/home/en/index.html
[26]: https://www.taptogo.net/
[27]: http://www.shenzhentong.com/
[28]: https://www.transport.act.gov.au/myway-and-fares
[29]: http://www.transperth.wa.gov.au/SmartRider/
[30]: http://bilheteunico.sptrans.com.br/
[31]: http://www.netsflashpay.com.sg/
[32]: http://www.ov-chipkaart.nl/
[33]: https://github.com/micolous/metrodroid/wiki/Cubic-Nextfare-MFC
[34]: http://www.metroinfo.co.nz/
[35]: https://github.com/micolous/metrodroid/wiki/ERG-MFC
[36]: http://www.calypsotechnology.net/
[37]: http://podorozhnik.spb.ru/en/
[38]: http://troika.mos.ru/
[39]: https://en.wikipedia.org/wiki/Kereta_Commuter_Indonesia
[40]: http://www.stm.info/en/info/fares/opus-cards-and-other-fare-media/opus-card
[41]: https://www.compasscard.ca/
[42]: https://www.rail.co.il/en/ravkav/Pages/default.aspx
[43]: https://www.t-money.co.kr/
