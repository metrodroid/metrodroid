.PHONY: clean

all: seq_gocard_stops.db3

seq_gocard_stops.db3: mapping.csv SEQ.zip
	python ../../extra/compile_stops_from_gtfs.py SEQ.zip -m mapping.csv -o $@
	
SEQ.zip:
	wget -N http://translink.com.au/sites/default/files/assets/resources/about-translink/reporting-and-publications/open-data/gtfs/$@

clean:
	rm -f SEQ.zip seq_gocard_stops.db3

