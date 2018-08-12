import shapefile
import csv
import codecs
from osgeo import osr,ogr
from mdst import MdstWriter
import stations_pb2
import zipfile
import io

OUTPUT = "ezlink.mdst"

tfz = zipfile.ZipFile("names.zip")
tfb = tfz.open("Train Station Codes and Chinese Names.csv", "r")
tf = codecs.getreader('utf-16')(tfb)

stationszip = zipfile.ZipFile("stations.zip")
sfName = "TrainStation_Oct2017/MRTLRTStnPtt"
shpcontents = {}
shpfiles = {}
for ext in ["prj", "shp", "dbf"]:
    with stationszip.open(sfName + "." + ext, "r") as f:
        shpcontents[ext] = f.read()
        shpfiles[ext] = io.BytesIO(shpcontents[ext])
sf = shapefile.Reader(**shpfiles)

sourcesr=osr.SpatialReference(wkt=codecs.decode(shpcontents["prj"], "utf-8"))

targetsr = osr.SpatialReference()
targetsr.ImportFromEPSG(4326)
    
transform = osr.CoordinateTransformation(sourcesr, targetsr)

tf_reader = csv.DictReader(tf, delimiter="\t")

names = {}
for tf_record in tf_reader:
    names[tf_record['stn_code']] = (tf_record['mrt_station_english'], tf_record['mrt_station_chinese'])

coordinates = {}

for shapeRec in sf.shapeRecords():
    name = shapeRec.record[1]
    for id in  map(lambda x: x.strip(), shapeRec.record[2].split("/")):
        geom = ogr.Geometry(ogr.wkbPoint)
        geom.AddPoint(shapeRec.shape.points[0][0],
                      shapeRec.shape.points[0][1])
        geom.Transform(transform)
        coordinates[id] = (geom.GetX(), geom.GetY())

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
)

s = stations_pb2.Station()
s.id = getID("GTM")
s.name.english = "Manual top-up"
db.push_station(s)

for station in mapping:
    s = stations_pb2.Station()
    s.id = getID(station["id"].strip())
    s.name.english = names[station["code"]][0].strip()
    s.name.local = names[station["code"]][1].strip()
    s.latitude = coordinates[station["code"]][1]
    s.longitude = coordinates[station["code"]][0]

    db.push_station(s)


mapping_f.close()

print('Building index...')
db.finalise()

print('Finished writing database.')
