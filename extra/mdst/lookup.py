#!/usr/bin/env python3
# -*- mode: python; indent-tabs-mode: nil; tab-width: 2; coding: utf-8 -*-
"""
lookup.py - lookup station in MdST

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
from google.protobuf.text_format import MessageToString
import google.protobuf.internal.decoder as decoder
from stations_pb2 import StationDb, Operator, Line, Station, StationIndex
import struct
from sys import argv

SCHEMA_VER = 1

def read_delimited_message(cls, f):
  t = f.tell()
  # Though numbers can be longer, only read 4 bytes = 2**27 bytes
  b = f.read(4)
  l, p = decoder._DecodeVarint(b, 0)
  
  # rewind the file to the correct position
  f.seek(t + p)
  return cls.FromString(f.read(l))

station_id = int(argv[2])
database = argv[1]

print('looking for station %d (%x)' % (station_id, station_id))
print('opening header...')
f = open(database, 'rb')
magic, ver, index_off = struct.unpack('!4sII', f.read(12))

if magic != b'MdST': raise Exception
if ver != 1: raise Exception

# Read in the header blob
header = read_delimited_message(StationDb, f)
print('file version = %d, local languages = %r' % (header.version, header.local_languages))
#print(MessageToString(header, as_utf8=True))

# Store table position
stationlist_off = f.tell()

# Jump to index
f.seek(index_off + stationlist_off)

# Read the index
print('opening index...')
index = read_delimited_message(StationIndex, f)

# Find the record
if station_id not in index.station_map:
  raise Exception, 'station not found'

station_off = index.station_map[station_id]
print('station record offset = %d (%d actual)' % (station_off, station_off + stationlist_off))
f.seek(station_off + stationlist_off)

print('Reading record...')
rec = read_delimited_message(Station, f)

print(MessageToString(rec, as_utf8=True))

# Lookup line and operator
if rec.operator_id:
  print('Operator:')
  print(MessageToString(header.operators[rec.operator_id], as_utf8=True))

if rec.line_id:
  print('Line:')
  print(MessageToString(header.lines[rec.line_id], as_utf8=True))


