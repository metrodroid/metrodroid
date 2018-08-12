#!/usr/bin/env python3
# -*- mode: python; indent-tabs-mode: nil; tab-width: 2; coding: utf-8 -*-
"""
csv2pb-ezlink.py - converts csv stop database to MdST

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

DB_PATH = '../../data/ezlink'
OUTPUT = 'ezlink.mdst'

db = MdstWriter(
  fh=open(OUTPUT, 'wb'),
  version=1,
  local_languages=['zh'],
  tts_hint_language='zh',
)

def getID(idstr):
    if len(idstr) != 3:
        raise Exception('Wrong ID length')
    return (ord(idstr[0]) << 16) | (ord(idstr[1]) << 8) | ord(idstr[2])

with open(os.path.join(DB_PATH, 'stations.csv'), 'r') as csvfile:
  stationreader = csv.reader(csvfile, delimiter=';', quotechar='"')
  for row in stationreader:
    if row[0] == 'ID':
      continue
    if row[0].startswith('#'):
      continue
    s = stations_pb2.Station()
    s.id = getID(row[0].strip())
    s.english_name = row[1].strip()
    if row[4].strip() and row[5].strip():
        s.latitude = float(row[4].strip())
        s.longitude = float(row[5].strip())

    db.push_station(s)


print('Building index...')
db.finalise()

print('Finished writing database.')
