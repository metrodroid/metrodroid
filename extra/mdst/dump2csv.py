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
from collections import OrderedDict
from csv import DictWriter
import google.protobuf.internal.decoder as decoder
from google.protobuf.text_format import MessageToString
from stations_pb2 import StationDb, Station
import struct

SCHEMA_VER = 1
FIELD_NAMES = ['id', 'name_en', 'name', 'name_short', 'name_short_en', 'lat', 'lon', 'oper_id', 'oper_en', 'oper', 'oper_short', 'line_id', 'line_en', 'line', 'line_short']

def read_delimited_message(cls, f):
  t = f.tell()
  # Though numbers can be longer, only read 4 bytes = 2**27 bytes
  b = f.read(4)
  l, p = decoder._DecodeVarint(b, 0)
  
  # rewind the file to the correct position
  f.seek(t + p)
  return cls.FromString(f.read(l))

def other_names(other, language_id_names):
  if not other:
    return ''
  o = []
  for lang_id, v in other.items():
    o.append((language_id_names[lang_id], v))
  o.sort(key=lambda x: x[0])
  return o

def dump2csv(database, output_fn):
  f = open(database, 'rb')
  magic, ver, index_off = struct.unpack('!4sII', f.read(12))

  if magic != b'MdST': raise Exception
  if ver != 1: raise Exception

  # Read in the header blob
  header = read_delimited_message(StationDb, f)
  #print('file version = %d, local languages = %r, tts_hint_language = %s' % (header.version, list(header.local_languages), header.tts_hint_language))
  if header.license_notice:
    print('== START OF LICENSE NOTICE (%d bytes) ==' % len(header.license_notice))
    print(header.license_notice)
    print('== END OF LICENSE NOTICE ==')
    header.license_notice = ''
  print(MessageToString(header, as_utf8=True))

  language_id_names = OrderedDict()
  for code, lang_id in header.languages.items():
    if lang_id not in language_id_names or language_id_names[lang_id] > code:
      language_id_names[lang_id] = code

  output_fh = open(output_fn, mode='w', encoding='utf-8')
  writer = DictWriter(output_fh, fieldnames=FIELD_NAMES)
  writer.writeheader()
  
  start_pos = f.tell()

  while start_pos + index_off > f.tell():
    rec = read_delimited_message(Station, f)
    d = {'id': rec.id}
    if rec.name.english:
      d['name_en'] = rec.name.english
    if rec.name.english_short:
      d['name_short_en'] = rec.name.english_short
    if rec.name.other:
      d['name'] = repr(other_names(rec.name.other, language_id_names))
    elif rec.name.local:
      d['name'] = rec.name.local
    if rec.name.other_short:
      d['name_short'] = repr(other_names(rec.name.other_short, language_id_names))
    if rec.latitude != 0 and rec.longitude != 0:
      d['lat'] = '%.6f' % rec.latitude
      d['lon'] = '%.6f' % rec.longitude
    if rec.operator_id:
      d['oper_id'] = rec.operator_id
      oper = header.operators[rec.operator_id]
      d['oper_en'] = oper.name.english
      if oper.name.other:
        d['oper'] = repr(other_names(oper.name.other, language_id_names))
      elif oper.name.local:
        d['oper'] = oper.name.local
      if oper.name.other_short:
        d['oper_short'] = repr(other_names(oper.name.other_short, language_id_names))
    if rec.line_id:
      d['line_id'] = ','.join([str(l) for l in rec.line_id])
      d['line_en'] = ','.join([header.lines[l].name.english for l in rec.line_id])
      # TODO localised line names
      d['line'] = ','.join([header.lines[l].name.local for l in rec.line_id])
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


