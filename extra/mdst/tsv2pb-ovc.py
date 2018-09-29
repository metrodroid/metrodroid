#!/usr/bin/env python3
# -*- mode: python; indent-tabs-mode: nil; tab-width: 2; coding: utf-8 -*-
"""
tsv2pb.py - converts tsv stop database to MdST

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
import csv
import codecs
import os
from stations_pb2 import Station
from mdst import MdstWriter

TSV_DIR = '../../third_party/ovc-tools/stations'
OUTPUT = 'ovc.mdst'

# From OVChipTransitData.java
OVC_OPERATORS = {
  # We should count starting at 1, but TLS are the system operator, and don't
  # have any "stations".
  0x00: 'TLS', # OVC operator
  0x01: 'Connexxion',
  0x02: 'GVB',
  0x03: 'HTM',
  0x04: 'NS',
  0x05: 'RET',

  0x07: 'Veolia',
  0x08: 'Arriva',
  0x09: 'Syntus',
  0x0A: 'Qbuzz',

  0x0C: 'DUO',
  0x19: 'Reseller',
  0x2C: 'DUO'
}

lines = {}

class TrimDictReader(csv.DictReader):
  @property                                    
  def fieldnames(self):
    if self._fieldnames is None:
      csv.DictReader.fieldnames.fget(self)
      if self._fieldnames is not None:
        self._fieldnames = [name.strip() for name in self._fieldnames]
    return self._fieldnames

def tsv_reader(tsvname):
  tsv_f = open(os.path.join(TSV_DIR, tsvname), 'r', encoding='utf-8')
  tsv_f.readline()
  prev = tsv_f.tell()
  while True:
    line = tsv_f.readline()
    if not line or not line.startswith('#'):
      break
    prev = tsv_f.tell()
  tsv_f.seek(prev)
  return TrimDictReader(tsv_f, dialect='excel-tab')

citystat = {}
# To save space, declare the "city" as the "line".
for tsvname in os.listdir(TSV_DIR):
  if not tsvname.startswith('data_') or not tsvname.endswith('.tsv'):
    continue
  station_count = 0
  for row in tsv_reader(tsvname):
    # Many records that are unknown... they look like "?" or "1234?"
    if row['name'] is None or row['name'].endswith('?'):
      continue

    if 'city' in row and row['city'] is not None and row['city'].strip() != '?':
      citystat[row['city'].strip()] = citystat.get(row['city'].strip(), 0) + 1

cities_sorted = sorted(citystat.items(), key=lambda x: (-x[1],x[0]), reverse=False)

i = 1
for row in cities_sorted:
  lines[row[0]] = i
  i += 1

db = MdstWriter(
  fh=open(OUTPUT, 'wb'),
  version=1,
  # Pivot the operator list from k(id)=v(en) to k(id)=v(en,)
  operators=dict(map(lambda v: (v[0], (v[1],)), OVC_OPERATORS.items())),
  # Pivot the line list from k(en)=v(id) to k(id)=v(en,)  
  lines=dict(map(lambda v: (v[1], (v[0],)), lines.items())),
)

print('Writing stations...')
# Now write out all the stations, and store their offset.
#cur.execute('SELECT company, ovcid, city, name, lat, lon FROM stations ORDER BY company, ovcid')
for tsvname in os.listdir(TSV_DIR):
  if not tsvname.startswith('data_') or not tsvname.endswith('.tsv'):
    continue
  station_count = 0
  for row in tsv_reader(tsvname):
    # Many records that are unknown... they look like "?" or "1234?"
    if row['name'] is None or row['name'].endswith('?'):
      continue

    operator_id = int(row['company']) & 0xffff
    line_id = None
    if 'city' in row and row['city'] is not None and row['city'].strip() != '?':
      line_id = lines[row['city'].strip()]

    # Skip TLS
    if operator_id == 0: continue

    # pack an int with the company + station code
    # Reduce so Connexxion is 0, this way the integers are shorter.
    station_id = ((operator_id - 1) << 16) + (int(row['ovcid']) & 0xffff)

    # create the record
    s = Station()
    s.id = station_id
    s.name.english = row['name'].strip()
    if 'lat' in row and 'lon' in row and row['lat'] is not None and row['lon'] is not None:
      s.latitude = float(row['lat'].strip())
      s.longitude = float(row['lon'].strip())
    #else:
    #  print('Missing location: (%d) %s' % (s.id, s.name.english))
    s.operator_id = operator_id
    if line_id is not None:
      s.line_id = line_id

    # Write it out
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

