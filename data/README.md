# data

This directory contains data that is used to build stop databases ([MdST][]).

The normal process with these files is to check in built versions of these files
to `mdst/`.

All MdST files need to be reproducible (to a degree). The preferred build mechanism is GNU
Makefiles.

## Targets

* `all`: builds `hermetic` and `web`
* `clean`: deletes all generated data
* `copy`: builds and copies all data for `hermetic` and `web`
* `*.build` (valid for a single data source): builds the given provider's data
* `*.copy` (valid for a single data source): copies the given provider's data to `mdst/`
* `*.clean` (valid for a single data source): deletes all generated data
* `hermetic`: builds all targets where the data is already in git
  * `hermetic_copy`: copies all `hermetic` targets to `mdst/`
  * `hermetic_clean`: deletes all generated data for `hermetic` targets
* `web`: builds all targets that need to fetch some data from the internet
  * `web_copy`: copies all `web` targets to `mdst/`
  * `web_clean`: deletes all generated data for `web` targets

### Simple examples

These are recommended examples to copy from:

* `lax_tap`: Shows how to build an MdST from GTFS data, where the GTFS files are
  distributed in a remote GitLab repository.

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

### Complex examples

* `amiibo`: Shows a custom schema for non-transit data, where required data
  files are distributed in a remote GitHub repository.

* `ezlink`: Shows how to build an MdST file manually (see
  `extra/mdst/ezlink.py`). This merges a mapping CSV, a bilingual stop list CSV
  file stored in a ZIP file (served over HTTP), and an ESRI Shapefile stored in
  another ZIP file (served over HTTP).

* `opus`: Builds an MdST file manually from an external XML format. Makefiles
  for this are not yet implemented.

### Historic examples

Don't do these anymore.

* `ovc`, `suica`: These both import data from Farebot's sqlite3 format. This is
  not recommended anymore.

## Other tools

* `Makefile.common`: Common methods used by per-source Makefiles.
* `Makefile.jq`: Used to test for the presence of [jq][].
* `Makefile.github`: A tool for interfacing with GitHub's API and fetching
  files.  Requires [jq][].
* `Makefile.gitlab`: A tool for interfacing with GitLab's API and fetching
  files.  Requires [jq][].


[jq]: https://stedolan.github.io/jq/
[mdst]: https://github.com/micolous/metrodroid/tree/master/extra/mdst
