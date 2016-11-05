.PHONY: clean

all: lax_tap_stations.db3

lax_tap_stations.db3: mapping.csv ../../third_party/gtfs_lacmta_rail/gtfs_rail.zip ../../extra/compile_stops_from_gtfs.py
	python ../../extra/compile_stops_from_gtfs.py ../../third_party/gtfs_lacmta_rail/gtfs_rail.zip -m mapping.csv -o $@

clean:
	rm -f lax_tap_stations.db3
