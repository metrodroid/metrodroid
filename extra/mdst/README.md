# MdST: Metrodroid Station Table format

MdST is a Protobuf-based format for representing Metrodroid's station databases. It replaces the
previous custom SQLite3 database used in previous versions of Metrodroid (and FareBot).

## File size deltas

The Java implementation added about 140 KiB to `classes.dex` compared to using Android's in-built
SQLite implementation.  The reader has since been ported to Kotlin, but the footprint is similar.

However, data files shrink by around a megabyte, and copying files out of Assets into Cache is no
longer required, resulting in significant net reductions to application size (and footprint).

Database | sqlite3 size | MdST size | Delta
---------|--------------|-----------|---------
LAX TAP  | 6.0 KiB      | 2.4 KiB   | -3.6 KiB
SEQ Go   | 6.0 KiB      | 2.0 KiB   | -4.0 KiB
Suica    | 1.3 MiB      | 519.7 KiB | -856 KiB
OVC      | 1.9 MiB      | 1.0 MiB   | -0.9 MiB

Before MdST, around 3.2 MiB of storage is used on station database files. After, we use about 1.5
MiB.

## License

This is licensed under the same terms as [Metrodroid][] (GPLv3+).

## Building it

All the scripts here use Python 3.  You need `protoc` and `python3-protobuf`.

To build the Python protobuf bindings, run `make stations_pb2.py`.

The Kotlin implementation of MdST is located in `StationTableReader.kt` (in the main source tree).

## Tools

<dl>
 <dt><code>dump2csv.py</code></dt>
 <dd>Dumps the content of an MdST file in CSV format.</dd>

 <dt><code>lookup.py</code></dt>
 <dd>Searches for a single station in an MdST file using the index.</dd>

 <dt><code>sqlite2db-suica*.py</code></dt>
 <dd>Converts the Suica stop information from FareBot's SQLite3 format.</dd>

 <dt><code>compile_stops_from_gtfs.py</code></dt>
 <dd>Used for building an MdST file based on GTFS data and a mapping file.</dd>

 <dt><code>csv2pb-podorozhnik.py</code></dt>
 <dd>Converts a Podorozhnik-specific dataset to MdST format.</dd>

 <dt><code>csv2pb.py</code></dt>
 <dd>Converts CSV files to MdST format using a common schema.</dd>

 <dt><code>ezlink.py</code></dt>
 <dd>Converts CEPAS/EZ-Link specific data files (CSV + ESRI shapefiles) to MdST format.</dd>

 <dt><code>tsv2pb-ovc.py</code></dt>
 <dd>Converts OVC data from <code>ovc-tools</code> TSV format to MdST.</dd>

 <dt><code>xml2pb-opus.py</code></dt>
 <dd>Converts Opus data from <code>LecteurOPUS</code> XML format to MdST.</dd>
</dl>

## File structure (v1)

* All integers are big-endian.
* All protocol buffer messages at the top level are [delimited][] (preceded by a [`varint`][],
  presenting the message length in bytes). This is the same as what
  [the Java Protobuf `writeDelimitedTo` uses][delimited].
* All protocol buffer messages are described in further detail in `stations.proto`.
* The structure is packed (not word-aligned).

At the top level, the file is laid out as:

Type                 | Name       | Description
---------------------|------------|--------------------
`char[4]`            | `magic`    | Magic string, always `MdST`.
`uint32`             | `version`  | File format version, always `1`.
`uint32`         | `stations_len` | Total number of bytes occupied by `stations`.
`StationDb`          | `header`   | Metadata header.
`Station` (repeated) | `stations` | Repeated [delimited][], `Station` messages.
`StationIndex`  | `station_index` | Byte offsets of each `Station` by its ID.

### How to read the file

Reading is designed to work within the limits of Android's implementation of
[Java's `InputStream` interface][inputstream]:

* `mark()` remembers the current file position (but with an unlimited `readlimit`).
* `reset()` jumps to the mark.
* `skip(N)` jumps forward `N` bytes (and cannot skip backwards).
* Only one `mark()` can be set on a file.
* There is no `ftell` / `tell`, so we never know the actual byte position of `mark()`.

Opening the file:

1. The `magic` and `version` are read and verified to be expected values.
2. `stations_len` and `header` are read and saved.
3. Mark the current offset in the file (start of `stations`).

The first time a search is required, read the `station_index`:

1. Reset file position to the mark (start of `stations`).
2. Jump forward `stations_len` bytes.
3. Read the `station_index` message.

Finding a `Station` by ID with the `station_index`:

1. Find the correct offset of the desired station in `station_index.station_map` as `N`.
2. Reset file position to the mark (start of `stations`).
3. Jump forward `N` bytes.
4. Read a delimited `Station` message.

Reading all records (scan):

1. Reset file position to the mark (start of `stations`).
2. Read a delimited `Station` message.
3. If the current file position is `stations_len` bytes past the mark, stop.


[delimited]: https://developers.google.com/protocol-buffers/docs/reference/java/com/google/protobuf/MessageLite#writeDelimitedTo-java.io.OutputStream-
[inputstream]: https://docs.oracle.com/javase/7/docs/api/java/io/InputStream.html
[metrodroid]: https://github.com/micolous/metrodroid
[varint]: https://developers.google.com/protocol-buffers/docs/encoding#varints
