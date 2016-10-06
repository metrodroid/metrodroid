# About this mapping file

Three additional columns are defined in this mapping file, which deal with special cases in the Go card fare rules.

Most of the time, if you want a new database built, you should be able to type `make`.  This will download the latest GTFS data from Translink and build `seq_gocard_stations.db3`.

If you want to add additional stations, you'll need to edit `mapping.csv` and then run `make`.

## `zone_id`

This allows overriding the zone of a particular stop.

AirTrain stations should have the override set to `airtrain`.

## `airtrain_zone_exempt`

Standard Airtrain fares on Go Card are $17.50 plus whatever other fares apply to the Journey.

However, between Eagle Junction and South Brisbane stations, no "zone" fare applies to the journey, and only the Airtrain charge applies.

All stations between Eagle Junction and South Brisbane stations should have this flag set to `1`.  Other stations should leave this unset.

While Airtrain does stop at South Bank and Park Road, these are charged an additional Zone 1 fare.  Altandi (Zone 4) is charged an additional 4 zone fare.

This shouldn't be set on Airport stations.

# About the data source

The [upstream GTFS data](https://data.qld.gov.au/dataset/general-transit-feed-specification-gtfs-seq) is licensed under the [CC BY 3.0 AU license](https://creativecommons.org/licenses/by/3.0/au/), provided by the [Queensland Department of Transport and Main Roads](http://www.tmr.qld.gov.au).
