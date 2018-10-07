# MdST: Metrodroid Station Table format (Version 1)

MdST is a Protobuf-based format for representing Metrodroid's station databases.

This file is composed of multiple Protobuf blobs, in order to make it faster to
read, and so that the entire data structure doesn't need to be held in memory
all at once.

It aims to replace the various custom SQLite3 databases used in earlier versions
of Metrodroid and Farebot.

The Java implementation adds about 140 KiB to `classes.dex` compared to using
Android's in-built SQLite implementation. However, data files shrink by around
a megabyte, and copying files out of Assets into Cache is no longer required,
resulting in significant net reductions to application size.

## File size deltas

Database | sqlite3 size | MdST size | Delta
---------|--------------|-----------|---------
LAX TAP  | 6.0 KiB      | 2.4 KiB   | -3.6 KiB
SEQ Go   | 6.0 KiB      | 2.0 KiB   | -4.0 KiB
Suica    | 1.3 MiB      | 519.7 KiB | -856 KiB
OVC      | 1.9 MiB      | 1.0 MiB   | -0.9 MiB

Before MdST, around 3.2 MiB of storage is used on stop database files. After, we
use about 1.5 MiB.

## Building it

All the scripts here use Python 3.  You need `protoc` and `python3-protobuf`.

To build all the station databases, protobuf bindings, and extract them again to
CSV files, run `make`.

If you just want to build the Python protobuf bindings, run
`make stations_pb2.py`.

## Tools

There are a number of translation tools for the database files sourced from
Farebot in this directory:

* `dump2csv.py`: Dumps out a MdST file to CSV format.

* `lookup.py`: Looks up a single stop in a MdST file using the index.

* `sqlite2pb-*.py`: Converts various legacy Farebot station database from
  SQLite3 format to MdST.

## License

This is licensed under the same terms as
[Metrodroid](https://github.com/micolous/metrodroid) (GPLv3+).

## File structure

All integers are big endian.

Protocol buffers are described in `stations.proto`.

### Version 1

Type | Length | Name | Description
-----|--------|------|-------------
`char` | 4      | magic | Always set to `MdST`.
`uint32` | 4    | version | Always set to 1. This is the file format version, and not intended to describe the contents of this file.
`uint32` | 4    | stations_length | The total number of bytes occupied by the stations structure. Can be used to find the `StationIndex` message, after the `StationDb` message has been read.
`StationDb` | _varies_ | header | Metadata header, containing the `StationDb` message. This is a _delimited_ Protobuf message, and prefixed with a varint representing its length.
repeated `Station` | _varies_ | station(repeated) | Repeated `Station` messages. This is a _delimited_ Protobuf message, and each one is prefixed by a varint representing its length.
`StationIndex` | _varies_ | station_index | Structure with a Protobuf `map`, describing the offsets of each station by its ID.

### Version 2

This version compresses data with brotli (or anything else)

Type | Length | Name | Description
-----|--------|------|-------------
`char` | 4      | magic | Always set to `MdST`.
`uint32` | 4    | version | Always set to 2. This is the file format version, and not intended to describe the contents of this file.
`varint` | _varies_ | header_len | Length of `header`.
`compressed(StationDb)` | _varies_ | header | Metadata header, containing compressed the `StationDb` message.
`compressed(repeated Station)` | _varies_ | Repeated station messages. The entire block is compressed, the length being in `header.v2_stations_length`. Each message is delimited (prefixed with `varint` of its length.
`varint` | _varies_ | index_len | Length of `index`.
`compressed(StationIndex)` | _varies_ | index | Structure with a Protobuf `map`, describing the offsets of each station by its ID.
