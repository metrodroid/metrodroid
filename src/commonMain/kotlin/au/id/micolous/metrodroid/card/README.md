# `au.id.micolous.metrodroid.card`

Functionality for reading and interfacing with contactless smartcards.

Package       | Description
------------- | ------------
`calypso`     | [Calypso][1], an ISO 7816 based card.
`cepas`       | [CEPAS (SS-518)][0], an ISO 7816 based card, used in Singapore.
`cepascompat` | [CEPAS (SS-518)][0] XML descriptors for dumps from old Metrodroid versions.
`classic`     | NXP/Phillips MIFARE Classic, and compatibles.
`china`       | (unknown) card media used in China, an ISO 7816 based card.
`desfire`     | NXP MIFARE DESFire.
`felica`      | Sony FeliCa (フェリカ / Felicity Card), a card commonly (but not exclusively) used in Japan.
`ksx6924`     | [KS X 6924][ksx6924] (티머니 / T-Money), an ISO 7816 card used in South Korea.
`iso7816`     | ISO/IEC 14443A + ISO 7816-4 base protocol implementation.
`nfcv`        | ISO/IEC 15693 (NfcV / Vicinity), an Ultralight-like card used mainly for ski passes.
`ultralight`  | NXP MIFARE Ultralight.

[0]: https://www.imda.gov.sg/industry-development/infrastructure/ict-standards-and-frameworks/specification-for-contactless-e-purse-application-cepas
[1]: http://www.calypsotechnology.net/
[ksx6924]: https://github.com/micolous/metrodroid/wiki/T-Money
