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

* [Adelaide Metro GTFS (historical)](https://github.com/gtfsdata/adelaidemetro-gtfs/) -- git repository containing historical GTFS data going back to 2011.

  There may be a way to use the historical data to guess what the other route numbers are.  They appear to be assigned in chronological order.

[2011-01-04 data (earliest in git)](https://github.com/gtfsdata/adelaidemetro-gtfs/blob/eb0b4261fc01c3f395602aaea1f5a09ff26d4be2/gtfs/routes.txt) appears to be a reasonable fit for the original routes. There was [a major update on 2011-01-16](https://github.com/gtfsdata/adelaidemetro-gtfs/blob/a95a206bf1eb3e2130df50f6ff1b8d1a8953d489/gtfs/routes.txt). The routes aren't a perfect fit -- suspect that the original set of routes were added from a route list not publicly released, or an earlier GTFS version.

We don't use the GTFS builder tools in Metrodroid yet, as this doesn't support routes, and we still need to figure out historical bits. This probably will be used in future.
