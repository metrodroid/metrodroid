# `au.id.micolous.metrodroid.transit`

Contains a base interface for representing information on a public transit smartcard in a
standardised form, and readers for individual agencies transit smartcards.

Further documentation about card formats [can be found in the Metrodroid wiki][0].

## Base packages

Package    | Description
---------- | -----------
`erg`      | [ERG][1] base card reader
`nextfare` | [Cubic Nextfare][2] card reader
`stub`     | Stubs for incomplete or partially known card types

## Agency-specific readers

Package            | Location(s)
------------------ | -----------
`bilhete_unico`    | :brazil: São Paulo, Brazil
`chc_metrocard`    | :new_zealand: Christchurch, New Zealand
`clipper`          | :us: San Francisco, CA, USA
`edy`              | :jp: Japan
`ezlink`           | :singapore: Singapore
`hsl`              | :finland: Finland
`lax_tap`          | :us: Los Angeles, CA, USA
`manly_fast_ferry` | :australia: Sydney, NSW, Australia
`myki`             | :australia: Melbourne (and surrounds), VIC, Australia
`octopus`          | :hong_kong: Hong Kong, :cn: Shenzhen, Guangdong Province, China
`opal`             | :australia: Sydney (and surrounds), NSW, Australia
`orca`             | :us: Seattle, WA, USA
`ovc`              | :netherlands: Netherlands
`seq_go`           | :australia: Brisbane and South East Queensland, Australia
`smartrider`       | :australia: Western Australia, Australia, :australia: Australian Capital Territory, Australia
`suica`            | :jp: Japan

[0]: https://github.com/micolous/metrodroid/wiki#card-data-formats
[1]: https://github.com/micolous/metrodroid/wiki/ERG-MFC
[2]: https://github.com/micolous/metrodroid/wiki/Cubic-Nextfare-MFC
