.PHONY: clean

all: seq_go_stations.db3

seq_go_stations.db3: mapping.csv SEQ_GTFS.zip ../../extra/compile_stops_from_gtfs.py
	python ../../extra/compile_stops_from_gtfs.py SEQ_GTFS.zip -m mapping.csv -e 'zone_id,airtrain_zone_exempt' --extra-fields-from-child -o $@
	
SEQ_GTFS.zip:
	wget -N https://gtfsrt.api.translink.com.au/GTFS/$@

clean:
	rm -f SEQ_GTFS.zip seq_go_stations.db3

