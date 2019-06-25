# LAX TAP mapping data

This maps between [LACMTA][]'s [GTFS data][upstream] and the [stop IDs](./mapping.csv) used on
[TAP cards][tap].  The GTFS data is used for station names and locations.

This is compiled into an [MdST][] file (`lax_tap.mdst`) that gets distributed with Metrodroid.

## Adding new data

### Stops

Edit `mapping.csv`. [You can find a list of stop_codes in LACMTA's repository][stops.txt].

Always use the `S` version of the stop code, as this is the parent for all entities at a station,
and contains the cleanest data.  Using [Union Station][] as an example:

* `80214S`: **This is the parent of all entities for the station.  Use this in mapping.csv.**
* `80209`: Union Station stop for the Gold line, including the line name.  _Don't use this._
* `80214`: Union Station stop for the Red/Purple lines, including the line names.  _Don't use this._
* `80209A`, `80214A` ~ `80214C`: These are entrances and exits for a station.  _Don't use these._

When you're done, compile a new stop database (see below).

### Operators

Add operators to `operators.csv`.  If there is some special handling needed for the data (eg: stops
are actually routes, or the operator only uses one ID), you might need to make changes to `LaxTap*`
classes in Metrodroid's codebase.

When you're done, compile a new stop database (see below).

### Compiling the stop database

Once you've made changes to `mapping.csv` or `operators.csv`, you need to copy the changes into
Metrodroid.

* `make`: builds the [MdST][] file (`lax_tap.mdst`) and a CSV representation of its content
  (`lax_tap.csv`).  Use this to check everything went as expected.

* `make copy`: copies the built MdST file into Metrodroid's assets directory.  This gets committed
  to `git`.

If this doesn't work, you can still submit a pull request.  Just note that you _haven't_ rebuilt the
MdST in the pull request.

## About the data source

### LACMTA / Metro

The [upstream GTFS data][upstream] is made available under [the following terms][terms], and
provided by the [Los Angeles County Metropolitan Transportation Authority (Metro)][lacmta].

No software from LACMTA is used.

[lacmta]: http://www.metro.net/
[mdst]: https://github.com/micolous/metrodroid/tree/master/extra/mdst
[stops.txt]: https://gitlab.com/LACMTA/gtfs_rail/blob/master/stops.txt
[tap]: https://github.com/micolous/metrodroid/wiki/Transit-Access-Pass
[terms]: http://developer.metro.net/the-basics/policies/terms-and-conditions/
[union station]: https://en.wikipedia.org/wiki/Union_Station_(Los_Angeles)
[upstream]: https://gitlab.com/LACMTA/gtfs_rail
