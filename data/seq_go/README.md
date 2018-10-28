# About this mapping file

Three additional columns are defined in this mapping file, which deal with
special cases in the Go card fare rules.

Most of the time, if you want a new database built, you should be able to
type `make`.  This will download the latest GTFS data from Translink and
build `seq_go.mdst`.

If you want to add additional stations, you'll need to edit `mapping.csv`
and then run `make`.

You can verify the presence of stations in `seq_go.csv`.

You can copy the MdST into the assets directory with `make copy`.

## About the data source

The [upstream GTFS data][0] is licensed under the [CC BY 4.0 AU license][1], provided by the
[Queensland Department of Transport and Main Roads][2].

## Pattern notes

### Train

The rail lines appear to mostly be in [line order][3]:

* Stop IDs <= 22 appear in an arbitrary order -- these were probably used as test stations

* Stop IDs 58 - 64 are Bowen Hills to Northgate.

* There's no neatly-fitting line for 65-85, so no guesses made there.

* Stop IDs 86 - 108 are the Beenleigh line.

* Gold Coast line stations might be >= 109 (likely excluding Varsity Lakes), but there's a pretty large gap before Cleveland line.

* Stop IDs 170 - 187 are the Cleveland line.

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

* To attempt Ferny Grove line:

  * Windsor
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

* 989 - 994: Stop code 001079 doesn't exist.

* 1043 - 1055: Stop codes 001166 and 001173 don't exist.

Reader IDs where interpolation was not attempted:

* 498 - 614: Some stop IDs were skipped (128 stops vs. 116 reader IDs)

* 614 - 795: Some stop IDs were added (168 stops vs. 181 reader IDs)

* 795 - 972: Some stop IDs were skipped (173 stops vs. 177 reader IDs)

* 972 - 989: A stop ID was added (16 stops vs. 17 reader IDs)

* 994 - 999: Some stop IDs were added (4 stops vs. 5 reader IDs)

* 999 - 1029: Some stop IDs were added (24 stops vs. 30 reader IDs)

* 1029 - 1040: Some stop IDs were skipped (13 stops vs. 11 reader IDs)

* 2425 - 2517: Some stop IDs were added (85 stops vs. 92 reader IDs)

* 2517 - 3003: Some stop IDs were skipped (520 stops vs. 486 reader IDs)

* 3003 - 4006: Some stop IDs were added (943 stops vs. 1003 reader IDs)

[0]: https://data.qld.gov.au/dataset/general-transit-feed-specification-gtfs-seq
[1]: https://creativecommons.org/licenses/by/4.0/au/
[2]: http://www.tmr.qld.gov.au
[3]: https://translink.com.au/sites/default/files/assets/resources/plan-your-journey/maps/160104-train-network-map.pdf
[4]: https://bitbucket.org/micolous/queensland-gtfs/

