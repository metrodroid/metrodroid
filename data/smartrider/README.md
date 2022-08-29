# About the SmartRider mapping file

## Rebuilding the database

If you want to add additional stations, you'll need to edit `mapping.csv` and then run `make`.

You can verify the presence of stations in `smartrider.csv`.

You can copy the MdST into the assets directory with `make copy`.

## About the data source

The [upstream GTFS data is freely provided][gtfs] by the Public Transit
Authority of Western Australia.

## Routes

All routes are stored as a plain text string of up to 4 characters on the card.

All train services are marked as `RAIL`.

## Stop IDs

SmartRider only stores the station that you that you _started_ your journey at,
and that you last tagged on at (for transfers). Unlike many other cards,
_SmartRider never stores tag off locations_.

If you're collecting stop IDs, you'll need to scan your card (ie: with
Metrodroid) after _every_ tag on.

In an older version of the SmartRider website, these IDs were displayed with
your travel history.

Stop IDs appear to be binary-coded decimal, but we're treating them as a
`uint16` for now.

### Buses

_Metrodroid doesn't even try to implement bus stops._

Implementing bus stops is probably infeasible:

* Bus stop IDs are an ordinal stop number for a given route. This means that
  stop 1 of route A is not the same as stop 1 of route B. This is different to
  most other cards.

  MdST stores stop IDs as a `uint32` (4 bytes), and there is 6 bytes of data on
  the card to match (route + stop). We could probably treat the stops as
  binary-coded decimal and do some clever bit packing by limiting the route ID's
  character set, but this is all pretty complicated.

* Bus routes have the same name for both directions (inbound and outbound), so
  it is difficult to discriminate between the two.

  We might be able to use zones, but this adds even more data to the MdST stop
  ID.

* Bus services _always_ start counting stops sequentially from 1, even if a
  particular service started at a _later_ point in the route than normal.

You would need a complete timetable to be able to guess which stop is being
used, and continually update it, and handle late services.

So that's too hard. 😞

### Trains

These _mostly_ follow line ordering.

Stations built after SmartRider's roll-out, or after the rest of the line, may
be placed between the IDs of two other stations (eg: Aubin Grove `3050`), or
appear with totally different numbering (eg: Greenwood `1448`).

The ordering in [Transperth's GTFS feed][gtfs] is totally different, and does
not match line ordering. Historical feeds have the same issue.

Transperth's ESRI Shapefile
([available from the same page as their GTFS feed][gtfs]) has similar IDs for
_one_ platform at many stations. They sometimes appear with an extra digit at
the start, or are otherwise "slightly off", and it's not consistent...
_so this isn't useful._

[gtfs]: https://www.transperth.wa.gov.au/About/Spatial-Data-Access
