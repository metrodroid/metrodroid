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
from google.protobuf.text_format import MessageToString
from stations_pb2 import StationDb, Station
import struct
import mdst
from io import BytesIO

FIELD_NAMES = ['id', 'name_en', 'name', 'name_short_en', 'lat', 'lon', 'oper_id', 'oper_en', 'oper', 'line_id', 'line_en', 'line']
# Link compression mode to mdst script
compression = mdst.compression

def read_delimiter(f):
  t = f.tell()
  # Though numbers can be longer, only read 4 bytes = 2**27 bytes
  b = f.read(4)
  l, p = decoder._DecodeVarint(b, 0)

  # rewind the file to the correct position
  f.seek(t + p)
  return l

def read_delimited_message(cls, f):
  l = read_delimiter(f)
  return cls.FromString(f.read(l))

def read_delimited_compression_message(cls, f):
  l = read_delimiter(f)
  d = BytesIO(compression.decompress(f.read(l)))
  return read_delimited_message(cls, d)

def message_reader_v1(f, index_off):
  start_pos = f.tell()
  while start_pos + index_off > f.tell():
    yield read_delimited_message(Station, f)

def message_reader_v2(f, index_off):
  decompressed = compression.decompress(f.read(index_off))
  stream = BytesIO(decompressed)
  return message_reader_v1(stream, len(decompressed))


def dump2csv(database, output_fn):
  f = open(database, 'rb')
  magic, ver = struct.unpack('!4sI', f.read(8))
  if magic != b'MdST': raise Exception

  print('MdST format version = %d' % ver)
  header = None
  index_off = 0
  if ver == 1:
    index_off = struct.unpack('!I', f.read(4))
    header = read_delimited_message(StationDb, f)
  elif ver == 2:
    header = read_delimited_compression_message(StationDb, f)
    index_off = header.v2_stations_length
  else:
    raise Exception

  # Read in the header blob
  print('file version = %d, local languages = %r, tts_hint_language = %s' % (header.version, list(header.local_languages), header.tts_hint_language))
  if header.license_notice:
    license_notice = header.license_notice
    print('== START OF LICENSE NOTICE (%d bytes) ==' % len(license_notice))
    print(license_notice)
    print('== END OF LICENSE NOTICE ==')
  #print(MessageToString(header, as_utf8=False))

  output_fh = open(output_fn, mode='w', encoding='utf-8')
  writer = DictWriter(output_fh, fieldnames=FIELD_NAMES)
  writer.writeheader()
  

  reader = None
  if ver == 1:
    reader = message_reader_v1(f, index_off)
  elif ver == 2:
    reader = message_reader_v2(f, index_off)

  for rec in reader:
    d = {'id': rec.id}
    if rec.name.english:
      d['name_en'] = rec.name.english
    if rec.name.english_short:
      d['name_short_en'] = rec.name.english_short
    if rec.name.local:
      d['name'] = rec.name.local
    if rec.latitude != 0 and rec.longitude != 0:
      d['lat'] = '%.6f' % rec.latitude
      d['lon'] = '%.6f' % rec.longitude
    if rec.operator_id:
      d['oper_id'] = rec.operator_id
      oper = header.operators[rec.operator_id]
      d['oper_en'] = oper.name.english
      d['oper'] = oper.name.local
    if rec.line_id:
      d['line_id'] = rec.line_id
      line = header.lines[rec.line_id]
      d['line_en'] = line.name.english
      d['line'] = line.name.local
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


