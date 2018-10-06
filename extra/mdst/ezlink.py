#!/usr/bin/env python3
# -*- mode: python; indent-tabs-mode: nil; tab-width: 4 -*-
import csv
import codecs
# gdal-python3
from osgeo import osr, ogr
from mdst import MdstWriter
import stations_pb2
import zipfile
from sys import exit

NOTICE = "NOTICE"
OUTPUT = "ezlink.mdst"
esri_driver = ogr.GetDriverByName('ESRI Shapefile')

tfz = zipfile.ZipFile("names.zip")
tfb = tfz.open("Train Station Codes and Chinese Names.csv", "r")
tf = codecs.getreader('utf-16')(tfb)

sfName = "TrainStation_Oct2017/MRTLRTStnPtt"
shp = esri_driver.Open('/vsizip/stations.zip/' + sfName + '.shp', 0) # read only
if shp is None:
    print('Cannot open shapefile')
    exit(1)

layer = shp.GetLayer(0)

# Setup a transform to EPSG 4326
targetsr = osr.SpatialReference()
targetsr.ImportFromEPSG(4326)
transform = osr.CoordinateTransformation(layer.GetSpatialRef(), targetsr)

# Read in the station names
tf_reader = csv.DictReader(tf, delimiter="\t")

names = {}
for tf_record in tf_reader:
    names[tf_record['stn_code']] = (tf_record['mrt_station_english'], tf_record['mrt_station_chinese'])

coordinates = {}

for shapeRec in layer:
    geom = shapeRec.GetGeometryRef()
    geom.Transform(transform)
    for stn_id in map(lambda x: x.strip(), shapeRec.GetField('STN_NO').split('/')):
         coordinates[stn_id] = (geom.GetX(), geom.GetY())
    shapeRec.Destroy()


shp.Destroy()

mapping_f = codecs.open("mapping.csv", "r", "utf-8")
mapping = csv.DictReader(mapping_f)

def getID(idstr):
    if len(idstr) != 3:
        raise Exception('Wrong ID length')
    return (ord(idstr[0]) << 16) | (ord(idstr[1]) << 8) | ord(idstr[2])


db = MdstWriter(
  fh=open(OUTPUT, 'wb'),
  version=1,
  local_languages=['zh'],
  tts_hint_language='zh',
  license_notice_f=open(NOTICE, 'r'),
)

s = stations_pb2.Station()
s.id = getID("GTM")
s.name.english = "Manual top-up"
db.push_station(s)

dropped_stations = set(names.keys()) | set(coordinates.keys())
total_stations = len(dropped_stations)

for station in mapping:
    s = stations_pb2.Station()
    s.id = getID(station["id"].strip())
    code = station["code"].strip()
    s.name.english = names[code][0].strip()
    s.name.local = names[code][1].strip()
    s.latitude = coordinates[code][1]
    s.longitude = coordinates[code][0]

    if code in dropped_stations:
        dropped_stations.remove(code)

    db.push_station(s)

mapping_f.close()

print('%d out of %d stations dropped: %s' % (len(dropped_stations), total_stations, sorted(dropped_stations)))

print('Building index...')
db.finalise()

print('Finished writing database.')

