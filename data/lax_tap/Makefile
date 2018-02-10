.PHONY: clean

all: lax_tap_stations.db3

lax_tap_stations.db3: mapping.csv ../../third_party/gtfs_lacmta_rail/gtfs_rail.zip ../../extra/compile_stops_from_gtfs.py
	rm -f lax_tap_stations.db3
	python3 ../../extra/compile_stops_from_gtfs.py ../../third_party/gtfs_lacmta_rail/gtfs_rail.zip -m mapping.csv -a 1 -o $@
	
	@echo ''
	@echo '## Stop report'
	@sqlite3 -header $@ 'select agency_id, count(1) stop_count from stops group by agency_id'
	
	@echo ''
	@echo '## Database version'
	@sqlite3 $@ 'pragma user_version'

clean:
	rm -f lax_tap_stations.db3
