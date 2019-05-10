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
import stations_pb2
from mdst import MdstWriter
import os
import csv

DB_PATH = '../../data/podorozhnik'
OUTPUT = 'podorozhnik.mdst'

db = MdstWriter(
  fh=open(OUTPUT, 'wb'),
  version=1,
  local_languages=['ru'],
  tts_hint_language='ru',
)

with open(os.path.join(DB_PATH, 'stations_metro.csv'), 'r') as csvfile:
  stationreader = csv.DictReader(csvfile, delimiter=';', quotechar='"')
  for row in stationreader:
    s = stations_pb2.Station()
    s.id = 0x10000 | (int(row['id'].strip(), 0) << 6)
    s.name.english = row['english'].strip()
    s.name.local = row['russian'].strip()
    if row['latitude'].strip() and row['longitude'].strip():
      s.latitude = float(row['latitude'].strip())
      s.longitude = float(row['longitude'].strip())

    db.push_station(s)


print('Building index...')
db.finalise()

print('Finished writing database.')
