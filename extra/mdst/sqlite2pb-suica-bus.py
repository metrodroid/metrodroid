#!/usr/bin/env python3
# -*- mode: python; indent-tabs-mode: nil; tab-width: 2; coding: utf-8 -*-
"""
sqlite2pb.py - converts sqlite3 stop database to MdST

Copyright 2018 Michael Farrell <micolous+git@gmail.com>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
"""
from __future__ import absolute_import, print_function
from argparse import ArgumentParser, FileType
from google.protobuf.internal import encoder
import sqlite3
from stations_pb2 import StationDb, Operator, Line, Station, StationIndex
import struct

STATIONS_DB = '../../data/felica_stations.db3'
OUTPUT = 'suica_bus.mdst'
SCHEMA_VER = 1

def delimited_value(msg):
  # Emits a writeDelimited compatible Protobuf message
  o = msg.SerializeToString()
  d = encoder._VarintBytes(len(o))
  return d + o

operators = {}
lines = {}
stations = {}

db = sqlite3.connect(STATIONS_DB)
cur = db.cursor()

print('Building company and line list...')
cur.execute('SELECT CompanyName, CompanyName_en FROM IruCaStationCode GROUP BY CompanyName, CompanyName_en ORDER BY COUNT(1) DESC')

i = 1
for row in cur:
  if row[0] is None: continue
  operators[row[0]] = (i, row[1])
  i += 1


# Note: a number of lines have the name Region and Line codes
# (eg: Tokyo Metro #3 Ginza, #8 Yūrakuchō, #9 Chiyoda)
# Need to actually extract this into a new sequence.
cur.execute('SELECT LineName, LineName_en FROM IruCaStationCode GROUP BY LineName, LineName_en ORDER BY COUNT(1) DESC')
i = 1
for row in cur:
  if row[0] is None: continue
  lines[row[0]] = (i, row[1])
  i += 1


sdb = StationDb()
sdb.version = 1
sdb.local_languages.append('ja')
for i, operator in operators.items():
  sdb.operators[operator[0]].english_name = operator[1]
  sdb.operators[operator[0]].local_name = i

for i, line in lines.items():
  sdb.lines[line[0]].english_name = line[1]
  sdb.lines[line[0]].local_name = i

f = open(OUTPUT, 'wb')
f.write(b'MdST')
f.write(struct.pack('!II', SCHEMA_VER, 0))
f.write(delimited_value(sdb))

# Get the offset of the start of the station list
stationlist_off = f.tell()

print('Writing stations...')
# Now write out all the stations, and store their offset.
cur.execute('SELECT 0, LineCode, StationCode, CompanyName, LineName, StationName, StationName_en FROM IruCaStationCode ORDER BY LineCode, StationCode')
station_count = 0
for row in cur:
  operator_id = None
  if row[3] is not None:
    operator_id = operators[row[3]][0]

  line_id = None
  if row[4] is not None:
    line_id = lines[row[4]][0]
  
  # pack an int with the area/line/station code
  station_id = ((int(row[1], 16) & 0xff) << 8) + (int(row[2], 16) & 0xff)

  # store the offset of the start of the record
  stations[station_id] = f.tell() - stationlist_off
  
  # create the record
  s = Station()
  s.id = station_id
  s.local_name = row[5]
  if row[6] is not None:
    s.english_name = row[6]

  #else:
  #  print('Missing location: (%d) %s' % (s.id, s.english_name))
  if operator_id is not None:
    s.operator_id = operator_id
  if line_id is not None:
    s.line_id = line_id
  
  # Write it out
  f.write(delimited_value(s))
  station_count += 1

print('Building index...')
# Now build an index
index_off = f.tell()
sidx = StationIndex()
for station_id, offset in stations.items():
  sidx.station_map[station_id] = offset

f.write(delimited_value(sidx))
index_end_off = f.tell()

# Write the location of the index
f.seek(4+4)
f.write(struct.pack('!I', index_off - stationlist_off))
f.close()

print('Finished writing database.  Here\'s the stats:')
print(' - total ............ %8d stations' % station_count)
print('                      %8d bytes' % index_end_off)
print()
station_count = float(station_count)
print(' - header ........... %8d bytes' % stationlist_off)
stations_len = (index_off - stationlist_off)
print(' - stations ......... %8d bytes (%.1f per record)' % (stations_len, stations_len / station_count))
index_len = (index_end_off - index_off)
print(' - index ............ %8d bytes (%.1f per record)' % (index_len, index_len / station_count))

