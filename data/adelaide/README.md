# Adelaide Metrocard

This contains route / stop IDs for Adelaide Metrocard.

The card appears to only ever use Agency 1.  For now, we only show this as "Adelaide Metro", and not the contract operators (eg: Southlink, TransAdelaide, etc.)

Line/route codes are composed in the same way as `En1545LookupSTR`:

```
0x0401abcd
  ||________ vehicle types (see below)
    ||______ agency ID (always 0x01)
      ||||__ route ID (written here in big-endian base16)
```

The vehicle types are per EN1545 specification:

* 0x01: Bus
* 0x04: Tram
* 0x05: Train

## Useful resources

* [Adelaide Metro GTFS (current)](https://data.sa.gov.au/data/dataset/adelaide-metro-general-transit-feed)

* [Adelaide Metro GTFS (historical)](https://github.com/gtfsdata/adelaidemetro-gtfs/) -- git repository containing historical GTFS data going back to 2013.

  There may be a way to use the historical data to guess what the other route numbers are.  They appear to be assigned in chronological order.

We don't use the GTFS builder tools in Metrodroid yet, as this doesn't support routes, and we still need to figure out historical bits. This probably will be used in future.
