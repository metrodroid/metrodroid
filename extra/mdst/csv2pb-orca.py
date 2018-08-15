#!/usr/bin/env python3
# -*- mode: python; indent-tabs-mode: nil; tab-width: 2; coding: utf-8 -*-
"""
csv2pb.py - converts csv stop database to MdST

Copyright 2018 Google

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
import stations_pb2
from mdst import MdstWriter
import os
import csv
import sys

db_path = sys.argv[1]
output = sys.argv[2]

operators = {}
lines = {}
reverse_operators = {}

with open(os.path.join(db_path, 'agencies.csv'), 'r') as csvfile:
  stationreader = csv.reader(csvfile, delimiter=';', quotechar='"')
  for row in stationreader:
    if row[0] == 'ID':
      continue
    if row[0].startswith('#'):
      continue
    id = int(row[0].strip(),0)
    o = stations_pb2.Operator()
    reverse_operators[row[1].strip()] = id
    o.name.local = row[2].strip()
    o.name.local_short = row[3].strip()
    if row[4].strip() != '':
      o.default_transport = stations_pb2.TransportType.Value(row[4].strip())
    operators[id] = o

def parseAgency(val):
  if val in reverse_operators:
    return reverse_operators[val]
  return int(val)

db = MdstWriter(
  fh=open(output, 'wb'),
  version=1,
  local_languages=['en'],
  tts_hint_language='en',
  operators=operators,
  lines=lines,
)

with open(os.path.join(db_path, 'stations.csv'), 'r') as csvfile:
  stationreader = csv.reader(csvfile, delimiter=';', quotechar='"')
  for row in stationreader:
    if row[0] == 'Agency':
      continue
    if row[0].startswith('#'):
      continue
    s = stations_pb2.Station()
    s.id = (parseAgency(row[0].strip()) << 16) | int(row[1].strip(), 0)
    s.name.local = row[2].strip()
    s.name.local_short = row[3].strip()
    if row[4].strip() and row[5].strip():
      s.latitude = float(row[4].strip())
      s.longitude = float(row[5].strip())
    db.push_station(s)


print('Building index...')
db.finalise()

print('Finished writing database.')
