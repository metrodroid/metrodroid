# Amiibo mapping data

Metrodroid uses [data][amiibo.json] from [AmiiboAPI][] to decode the identity of [Amiibo][]
figurines.

`/extra/mdst/amiibo2pb.py` converts from [AmiiboAPI][] JSON format into [MdST][] format.

The [data schema][amiibo.json] is mapped as:

* Amiibo series are mapped to `Operators`.
* Amiibos are mapped to `Stations`.

More information can be found in the `AmiiboTransitData` class.

[amiibo]: https://en.wikipedia.org/wiki/Amiibo
[amiibo.json]: https://github.com/N3evin/AmiiboAPI/blob/master/database/amiibo.json
[amiiboapi]: https://github.com/N3evin/AmiiboAPI
[mdst]: https://github.com/micolous/metrodroid/tree/master/extra/mdst
