.PHONY: clean

all: seq_gocard_stops.csv

seq_gocard_stops.csv: mapping.csv SEQ.zip
	python ../../extra/compile_stops_from_gtfs.py SEQ.zip -m mapping.csv -o $@
	
SEQ.zip:
	wget -N http://translink.com.au/sites/default/files/assets/resources/about-translink/reporting-and-publications/open-data/gtfs/$@

clean:
	rm -f SEQ.zip seq_gocard_stops.csv

