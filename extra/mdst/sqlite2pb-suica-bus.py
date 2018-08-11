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
import sqlite3
from stations_pb2 import Station
from mdst import MdstWriter

STATIONS_DB = '../../data/felica_stations.db3'
OUTPUT = 'suica_bus.mdst'

operators = {}
lines = {}

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

db = MdstWriter(
  fh=open(OUTPUT, 'wb'),
  version=1,
  local_languages=['ja'],
  tts_hint_language='ja',
  # Pivot the operator and line lists from k(ja)=v(id,en) to k(id)=v(en,ja)
  operators=dict(map(lambda v: (v[1][0], (v[1][1], v[0])), operators.items())),
  lines=dict(map(lambda v: (v[1][0], (v[1][1], v[0])), lines.items())),
)

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
  
  # create the record
  s = Station()
  s.id = station_id
  s.name.local = row[5]
  if row[6] is not None:
    s.name.english = row[6]

  #else:
  #  print('Missing location: (%d) %s' % (s.id, s.name.english))
  if operator_id is not None:
    s.operator_id = operator_id
  if line_id is not None:
    s.line_id = line_id
  
  db.push_station(s)
  station_count += 1

print('Building index...')
index_end_off = db.finalise()

print('Finished writing database.  Here\'s the stats:')
print(' - total ............ %8d stations' % station_count)
print('                      %8d bytes' % index_end_off)
print()
station_count = float(station_count)
print(' - header ........... %8d bytes' % db.stationlist_off)
stations_len = (db.index_off - db.stationlist_off)
print(' - stations ......... %8d bytes (%.1f per record)' % (stations_len, stations_len / station_count))
index_len = (index_end_off - db.index_off)
print(' - index ............ %8d bytes (%.1f per record)' % (index_len, index_len / station_count))


