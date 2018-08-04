#!/usr/bin/env python3
# -*- mode: python; indent-tabs-mode: nil; tab-width: 2; coding: utf-8 -*-
"""
list2pb.py - converts stop list to MdST

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
from stations_pb2 import Station
from mdst import MdstWriter
import os

DB_PATH = '../../data/szt'
MAIN_FILE = 'operators.xml'
OUTPUT = 'shenzhen.mdst'

operators = {}
lines = {}
stations = {}

db = MdstWriter(
  fh=open(OUTPUT, 'wb'),
  version=1,
  local_languages=['zh'],
  tts_hint_language='zh',
  operators=operators,
  lines=lines,
)

def hex2bcd(x):
  res = 0
  shift = 0
  while x > 0:
    res |= (x%10) << shift
    x //= 10
    shift = shift + 4
  return res

def read_metro_line(line_name, line_number, start_id):
  with open(os.path.join(DB_PATH, line_name + '_en.txt'), 'rb') as enfile:
    stations_en = enfile.readlines()
  with open(os.path.join(DB_PATH, line_name + '_zh.txt'), 'rb') as zhfile:
    stations_zh = zhfile.readlines()
  id = start_id
  for station_en, station_zh in zip(stations_en,stations_zh):
    s = Station()
    s.id = 0x60000100 | (line_number << 24) | (hex2bcd(id) << 12)
    s.english_name = station_en.strip()
    s.local_name = station_zh.strip()
    id = id + 1
    # Write it out
    db.push_station(s)

# I have no idea why line common number doesn't match its ID
read_metro_line('line1', 8, 1)
read_metro_line('line3', 1, 2)

print('Building index...')
db.finalise()

print('Finished writing database.')
