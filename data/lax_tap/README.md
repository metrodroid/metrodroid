# About this mapping file

Most of the time, if you want a new database built, you should be able to type `make`.

You'll also need to pull in any updates in the LACMTA Rail GTFS repository, which you can find in
`third_party/gtfs_lacmta_rail`.

If you want to add additional stations, you'll need to edit `mapping.csv` and then run `make`.

This will create `lax_tap_stations.db3`, which can be dropped in the application's assets directory.

# About stop codes

Use the "S" version of each stop_code, rather than the bare number.  This is because stations that
are on multiple lines will otherwise have the line names stuffed in the station name, making it very
long.

# About the data source

## LACMTA / Metro

The [upstream GTFS data](https://gitlab.com/LACMTA/gtfs_rail) is made available under [the following
terms](http://developer.metro.net/the-basics/policies/terms-and-conditions/), and provided by the
[Los Angeles County Metropolitan Transportation Authority (Metro)](http://www.metro.net).

No software from LACMTA is used.

