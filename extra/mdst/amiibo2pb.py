#!/usr/bin/env python3
# -*- mode: python; indent-tabs-mode: nil; tab-width: 2 -*-
"""
csv2pb.py
Compiles MdST stop database from CSV data.

Copyright 2015-2018 Michael Farrell <micolous+git@gmail.com>
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

from __future__ import print_function
from argparse import ArgumentParser, FileType
from datetime import datetime, timedelta
from stations_pb2 import Station, Operator, Line
import mdst
import codecs, json


def compile_stops_from_csv(json_f, output_f, version=None, notice_f=None):
  json_f = codecs.getreader('utf-8-sig')(json_f)

  aroot = json.loads(json_f.read())

  operators = {}
  lines = {}

  for (opid, opname) in aroot["amiibo_series"].items():
    oppb = Operator()
    oppb.name.english = opname
    operators[int(opid, 0)] = oppb

  db = mdst.MdstWriter(
    fh=open(output_f, 'wb'),
    version=version,
    operators=operators,
    lines=lines,
    local_languages=[],
    tts_hint_language=None,
    license_notice_f=notice_f,
  )

  for (cid, cname) in aroot["characters"].items():
    s = Station()
    s.id = int(cid, 0)
    s.name.english = cname
    db.push_station(s)

  json_f.close()

  index_end_off = db.finalise()

  print('Finished writing database.')


def main():
  parser = ArgumentParser()
  
  parser.add_argument('-o', '--output',
    required=True,
    help='Output data file (MdST)'
  )

  parser.add_argument('input_json',
    nargs=1,
    type=FileType('rb'),
    help='Path to JSON file to extract data from.')

  parser.add_argument('-V', '--version',
    required=True,
    type=int,
    help='Enter a version to write to the file.')

  parser.add_argument('-n', '--license-notice',
    required=False,
    type=FileType('r'),
    help='If specified, the file from which to read a license notice from.')

  options = parser.parse_args()

  compile_stops_from_csv(options.input_json[0], options.output, options.version, options.license_notice)

if __name__ == '__main__':
  main()
