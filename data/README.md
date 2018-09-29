# data

This directory contains data that is used to build stop databases (MdST).

The normal process with these files is to check in built versions of these files
to `src/main/assets/`.

However, all MdST files need to be reproducable (to a degree). The preferred
build mechanism is GNU Makefiles.

## Simple examples

These are recommended examples to copy from:

* `lax_tap`: Shows how to build an MdST from GTFS data, where the GTFS files are
  distributed in a remote git repository. `third_party/gtfs_lacmta_rail`
  contains the ZIP files for this, which gets fetched as a git submodule.

* `seq_go`: Shows how to build an MdST from GTFS data, where the GTFS files are
  distributed via HTTP.

* `clipper`: Shows how to build an MdST from multiple GTFS data files, which are
  distributed via HTTP.

* `tfi_leap`: Shows how to build an MdST from GTFS data, where the GTFS files
  are distrubuted via HTTP, and the operator names are written in both English
  and Irish.

* `compass`: Shows how to build an MdST from a static CSV file, where all the
  stop names are in English only.

* `kmt`: Shows how to build an MdST from a static CSV file, where all the stop
  names are in Indonesian only.

* `troika`: Shows how to build an MdST from a static CSV file, and handle
  localisation. All the local names of stops are written in both English and
  Russian.

## Complex examples

* `ezlink`: Shows how to build an MdST file manually (see
  `extra/mdst/ezlink.py`). This merges a mapping CSV, a bilingual stop list CSV
  file stored in a ZIP file (served over HTTP), and an ESRI Shapefile stored in
  another ZIP file (served over HTTP).

* `opus`: Builds an MdST file manually from an external XML format. Makefiles
  for this are not yet implemented.

## Historic examples

Don't do these anymore.

* `ovc`, `suica`: These both import data from Farebot's sqlite3 format. This is
  not recommended anymore.

