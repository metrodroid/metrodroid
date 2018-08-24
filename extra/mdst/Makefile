# -*- mode: makefile; indent-tabs-mode: tabs; tab-width: 2 -*-

.PHONY: all clean

all: stations_pb2.py mdsts

stations_pb2.py: stations.proto
	protoc --python_out=. stations.proto

clean:
	rm -f stations_pb2.py
	rm -f $(MDSTS)
	rm -f $(MDSTS:.mdst=.csv)

MDSTS += orca.mdst
orca.mdst: csv2pb-orca.py stations_pb2.py mdst.py ../../data/orca/*.csv
	python3 csv2pb-orca.py ../../data/orca orca.mdst

MDSTS += suica_rail.mdst
suica_rail.mdst: sqlite2pb-suica.py stations_pb2.py mdst.py ../../data/felica_stations.db3
	python3 sqlite2pb-suica.py

MDSTS += suica_bus.mdst
suica_bus.mdst: sqlite2pb-suica-bus.py stations_pb2.py ../../data/felica_stations.db3
	python3 sqlite2pb-suica-bus.py

MDSTS += kmt.mdst
kmt.mdst: csv2pb.py stations_pb2.py mdst.py ../../data/kmt/stations.csv
	python3 csv2pb.py ../../data/kmt/stations.csv -o $@ -V 2 -L id

MDSTS += ovc.mdst
ovc.mdst: sqlite2pb-ovc.py stations_pb2.py mdst.py ../../data/ovc_stations.db3
	python3 sqlite2pb-ovc.py

MDSTS += shenzhen.mdst
shenzhen.mdst: csv2pb-szt.py stations_pb2.py mdst.py ../../data/szt/*.txt
	python3 csv2pb-szt.py

MDSTS += podorozhnik.mdst
podorozhnik.mdst: csv2pb-podorozhnik.py stations_pb2.py mdst.py ../../data/podorozhnik/*.csv
	python3 csv2pb-podorozhnik.py

MDSTS += opus.mdst
opus.mdst: xml2pb-opus.py stations_pb2.py mdst.py ../../data/opus/*.xml
	python3 xml2pb-opus.py

MDSTS += compass.mdst
compass.mdst: csv2pb.py stations_pb2.py mdst.py ../../data/compass/stations.csv
	python3 csv2pb.py ../../data/compass/stations.csv -o $@ -V 2

MDSTS += troika.mdst
troika.mdst: csv2pb.py stations_pb2.py mdst.py ../../data/troika/stations.csv
	python3 csv2pb.py ../../data/troika/stations.csv -o $@ -V 2 -L ru

%.csv: %.mdst dump2csv.py stations_pb2.py
	python3 dump2csv.py -o $@ $<

mdsts: $(MDSTS) $(MDSTS:.mdst=.csv)

