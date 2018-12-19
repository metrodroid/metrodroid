.PHONY: all clean

all: StationCode.csv

%.csv: %.xls xls2csv.py
	python3 xls2csv.py -o $@ $<

clean:
	rm -f StationCode.csv