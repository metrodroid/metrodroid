---
title: Metrodroid
permalink: /
layout: home
---

## Screenshots

{% include screenshots.html screenshots=site.data.index %}

[More screenshots...](https://micolous.github.io/metrodroid/screenshots)

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
[Go card][20] | :australia: Brisbane and South East Queensland, Australia | :new: :closed_lock_with_key: `MFC` [Note][5]
[Kartu Multi Trip][39] | :indonesia: Jakarta, Indonesia | `KMT`
[Manly Fast Ferry][19] | :australia: Sydney, NSW, Australia | :new: :closed_lock_with_key: `MFC`
[Matkakortti][16], [HSL][17] | :finland: Finland |
[Metrocard][34] | :new_zealand: Christchurch, New Zealand | :new: :closed_lock_with_key: `MFC`
[Myki][21] | :australia: Melbourne (and surrounds), VIC, Australia | :new: `123` [Note][4]
[MyWay][28] | :australia: Australian Capital Territory, Australia | :new: :closed_lock_with_key: `MFC`
[NETS FlashPay][31] | :singapore: Singapore | 
[Octopus][25] | :hong_kong: Hong Kong | :new:
[Opal][18] | :australia: Sydney (and surrounds), NSW, Australia | :new:
[Opus][40] | :canada: Québec, Canada | :new:
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

This project **will only read data from the card itself**, without having to
connect to the agency's back-office systems. In some cases, limited data is
available, so balance information and trip history might not be available.

[4]: https://micolous.github.io/metrodroid/myki
[5]: https://micolous.github.io/metrodroid/seqgo


[0]: http://www.orcacard.com/
[1]: https://www.clippercard.com/
[6]: https://en.wikipedia.org/wiki/MIFARE#MIFARE_DESFire
[7]: http://www.ezlink.com.sg/
[8]: https://en.wikipedia.org/wiki/FeliCa
[9]: https://en.wikipedia.org/wiki/Suica
[10]: https://en.wikipedia.org/wiki/ICOCA
[11]: https://en.wikipedia.org/wiki/PASMO
[12]: https://en.wikipedia.org/wiki/Edy
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


