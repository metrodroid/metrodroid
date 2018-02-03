#!/usr/bin/env python3
# -*- mode: python; indent-tabs-mode: nil; tab-width: 2; coding: utf-8 -*-
"""
dump2csv.py - Dump MdST file to CSV.

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
from argparse import ArgumentParser
from csv import DictWriter
import google.protobuf.internal.decoder as decoder
from stations_pb2 import StationDb, Station
import struct

SCHEMA_VER = 1
FIELD_NAMES = ['id', 'name_en', 'name', 'lat', 'lon', 'oper_id', 'oper_en', 'oper', 'line_id', 'line_en', 'line']

def read_delimited_message(cls, f):
  t = f.tell()
  # Though numbers can be longer, only read 4 bytes = 2**27 bytes
  b = f.read(4)
  l, p = decoder._DecodeVarint(b, 0)
  
  # rewind the file to the correct position
  f.seek(t + p)
  return cls.FromString(f.read(l))

def dump2csv(database, output_fn):
  f = open(database, 'rb')
  magic, ver, index_off = struct.unpack('!4sII', f.read(12))

  if magic != b'MdST': raise Exception
  if ver != 1: raise Exception

  # Read in the header blob
  header = read_delimited_message(StationDb, f)
  print('file version = %d, local languages = %r' % (header.version, list(header.local_languages)))
  #print(MessageToString(header, as_utf8=True))

  output_fh = open(output_fn, mode='w', encoding='utf-8')
  writer = DictWriter(output_fh, fieldnames=FIELD_NAMES)
  writer.writeheader()
  
  start_pos = f.tell()


  while start_pos + index_off > f.tell():
    rec = read_delimited_message(Station, f)
    d = {'id': rec.id}
    if rec.english_name:
      d['name_en'] = rec.english_name
    if rec.local_name:
      d['name'] = rec.local_name
    if rec.latitude != 0 and rec.longitude != 0:
      d['lat'] = '%.6f' % rec.latitude
      d['lon'] = '%.6f' % rec.longitude
    if rec.operator_id:
      d['oper_id'] = rec.operator_id
      oper = header.operators[rec.operator_id]
      d['oper_en'] = oper.english_name
      d['oper'] = oper.local_name
    if rec.line_id:
      d['line_id'] = rec.line_id
      line = header.lines[rec.line_id]
      d['line_en'] = line.english_name
      d['line'] = line.local_name
    writer.writerow(d)

  output_fh.close()

def main():
  parser = ArgumentParser()
  parser.add_argument('input_mdst', help='Input MdST file', nargs=1)
  parser.add_argument('-o', '--output', help='Output CSV file')
  
  options = parser.parse_args()
  
  dump2csv(options.input_mdst[0], options.output)

if __name__ == '__main__':
  main()


