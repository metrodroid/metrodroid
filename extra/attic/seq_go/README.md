# Go Card attic

Metrodroid 2.9.28 contained a fare calculation system for Go Card.

Without knowing how the fares are actually stored, I implemented this fare
calculation logic for Go Card.  It's fairly coupled to the way that
Metrodroid represents trips, and should be used in conjunction with the
`mapping.csv` file in `data/seq_go`.

Now we just use the information that Nextfare stores for values, and that
should be valid in perpetuity, and not require the stop database to be
complete.

It includes a fairly complete set of unit tests for the ~2016 fare schedule.

This would need to be updated for the 2017 fare schedule, as that completely
changes zones.
