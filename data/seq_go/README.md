# About this mapping file

## Rebuilding the database

If you want to add additional stations, you'll need to edit `mapping.csv` and then run `make`.

You can verify the presence of stations in `seq_go.csv`.

You can copy the MdST into the assets directory with `make copy`.

## About the data source

The [upstream GTFS data][0] is licensed under the [CC BY 4.0 AU license][1], provided by the
[Queensland Department of Transport and Main Roads][2].

## Getting stop IDs

The goal is to get the most granular information possible for matching stop IDs. Generally speaking,
this means the interpretation should match _the vending machines_.

_Bus stop IDs are different_ on the Go card and GTFS data for stops on each side of the road, and
generally also different for each platform at a bus interchange or busway station. Don't use
`place_*` for anything but train stations.

_All bus events_ use a combination of GPS and driver interaction in order to select the current
stop. As a result, some information may be recorded incorrectly (if it has been locked on to a
different stop, or the card was tapped off _before_ the bus stopped).

_All top-up events_ use a different set of IDs. No attempt has been made at these yet.

### Vending machines (best)

_Go card vending machines_ are best source of data, but only show the last _10 tap events_ (there
are 12 on the card).

The bus stop names are different again (from GTFS and Go card website data), but the Stop ID (used
in GTFS) is shown in square brackets.  For example:

```
31/12/16 12:00 pm Location: South Bank busway station Platform 1 [19052]
Touch On
```

Sometimes bus stop IDs on Go card vending machines have a suffix separated by a dash, for example:
`12345-2`. Omit the suffix, but put it in the note column if present.

### Go card website

The _[Go card website][goweb]_ shows _some_ data on registered cards, which is good enough for train
stations _only_.  It has some problems for bus stops:

* the bus stop ID is not shown
* the bus stop names don't exactly match GTFS data

## Pattern notes

### Train

The rail lines appear to mostly be in line order (map: [2016][map16], [2018][map18]):

* Stop IDs <= 0x16 appear in an arbitrary order -- these were probably used as test stations

* Stop IDs 0x3a - 0x40 are Bowen Hills to Northgate.

* There's no neatly-fitting line for 0x41 - 0x55, so no guesses made there.

* Stop IDs 0x56 - 0x6c are the Beenleigh line.

* Gold Coast line stations might be >= 0x6d (likely excluding Varsity Lakes), but there's a pretty large gap before Cleveland line.

* Stop IDs 0x7f - 0x89 may be the Ferny Grove line, but this doesn't appear in order (see below).

* Stop IDs 0xaa - 0xbb are the Cleveland line.

#### Station closures

There haven't been any permanent train station closures since Go card was rolled out.

#### New stations

Rail network additions since Go card rolled out (2008):

* 2009-12-13: Varsity Lakes station opened.

* 2013-12: Springfield line (Darra - Springfield Central) opened.

* 2016-10-04: Redcliffe Peninsula line (Petrie - Kippa-Ring) opened.

#### Ideal stations to collect

* To improve confidence in Beenleigh/Gold Coast line:

  * Beenleigh (would confirm line theory)
  * Ormeau
  * Robina
  * Varsity Lakes

* To improve confidence in Cleveland line:

  * Buranda
  * Cleveland

* To attempt Doomben line:

  * Eagle Junction
  * Clayfield
  * Doomben

* To attempt Shorncliffe line:

  * Northgate
  * Bindha
  * Shorncliffe

* To attempt Redcliffe Peninsula line:

  * Northgate
  * Virginia
  * Petrie
  * Kallangur
  * Kippa-Ring

* To attempt Caboolture / Sunshine Coast lines:

  * Northgate
  * Virginia
  * Caboolture
  * Nambour
  * Gympie North

* Ferny Grove line appears non-contiguous.  Winsdor = 0x7f, Gaythorne = 0x83; 3 IDs between them, when there are 4 stations.  Will need to do the remainder of the whole line:

  * Wilston
  * Newmarket
  * Alderley
  * Enoggera
  * Mitchelton
  * Oxford Park
  * Grovely
  * Keperra
  * Ferny Grove

* To attempt Springfield / Ipswitch / Rosewood lines:

  * Milton
  * Darra
  * Wacol
  * Richlands
  * Springfield Central
  * Ipswitch
  * Rosewood

### Tram / G:link

Probably sequential, but don't have any data to support this yet.

### Buses

Bus lines are partly sequential, but lots of stops have been added and removed over time. Some
reader IDs appear to be out-of-sequence with Stop Codes.

There is [historical GTFS data avaliable which goes back to 2015-04][4]. This git repository doesn't
get used for Metrodroid because shittypack removes stop IDs (rail uses `place_*` identifiers) and
the repository itself is huge (~2 GiB for a complete copy).

We interpolate reader IDs where possible. These are special successful cases:

* 0x3dd - 0x3e2: Stop code 001079 doesn't exist.

* 0x413 - 0x41f: Stop codes 001166 and 001173 don't exist.

Reader IDs where interpolation was not attempted:

* 0x1f2 - 0x266: Some stop IDs were skipped (128 stops vs. 116 reader IDs)

* 0x266 - 0x31b: Some stop IDs were added (168 stops vs. 181 reader IDs)

* 0x31b - 0x3cc: Some stop IDs were skipped (173 stops vs. 177 reader IDs)

* 0x3cc - 0x3dd: A stop ID was added (16 stops vs. 17 reader IDs)

* 0x3e2 - 0x3e7: Some stop IDs were added (4 stops vs. 5 reader IDs)

* 0x3e7 - 0x405: Some stop IDs were added (24 stops vs. 30 reader IDs)

* 0x405 - 0x410: Some stop IDs were skipped (13 stops vs. 11 reader IDs)

* 0x979 - 0x9d5: Some stop IDs were added (85 stops vs. 92 reader IDs)

* 0x9d5 - 0xbbb: Some stop IDs were skipped (520 stops vs. 486 reader IDs)

* 0xbbb - 0xfa6: Some stop IDs were added (943 stops vs. 1003 reader IDs)

[0]: https://data.qld.gov.au/dataset/general-transit-feed-specification-gtfs-seq
[1]: https://creativecommons.org/licenses/by/4.0/au/
[2]: http://www.tmr.qld.gov.au
[map16]: https://translink.com.au/sites/default/files/assets/resources/plan-your-journey/maps/160104-train-network-map.pdf
[map18]: https://translink.com.au/sites/default/files/assets/resources/plan-your-journey/maps/180401-train-busway-ferry-tram-network-map.pdf
[4]: https://bitbucket.org/micolous/queensland-gtfs/
[goweb]: https://gocard.translink.com.au/
