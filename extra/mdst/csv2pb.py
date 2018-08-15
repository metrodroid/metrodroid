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
from stations_pb2 import Station, Operator
import mdst
import codecs, csv


def compile_stops_from_csv(csv_f, output_f, version=None, tts_hint_language=None, operators_f=None, local_languages=None):
  csv_f = codecs.getreader('utf-8-sig')(csv_f)

  operators = {}

  if operators_f is not None:
    operators_f = codecs.getreader('utf-8-sig')(operators_f)
    operators = mdst.read_operators_from_csv(operators_f)
    operators_f.close()

  db = mdst.MdstWriter(
    fh=open(output_f, 'wb'),
    version=version,
    operators=operators,
    local_languages=local_languages.split(',') if local_languages is not None else [],
    tts_hint_language=tts_hint_language,
  )

  mdst.read_stops_from_csv(db, csv_f)
  csv_f.close()

  index_end_off = db.finalise()

  print('Finished writing database.')


def main():
  parser = ArgumentParser()
  
  parser.add_argument('-o', '--output',
    required=True,
    help='Output data file (MdST)'
  )

  parser.add_argument('input_csv',
    nargs=1,
    type=FileType('rb'),
    help='Path to CSV file to extract data from.')
  
  parser.add_argument('-p', '--operators',
    required=False,
    type=FileType('rb'),
    help='If supplied, this is an operators file of mapping between ids and operators. If a matching file is not supplied, this will produce an empty list of operators.')

  parser.add_argument('-V', '--version',
    required=True,
    type=int,
    help='Enter a version to write to the file.')

  parser.add_argument('-l', '--tts-hint-language',
    required=False,
    help='If specified, provides a hint for LocaleSpan when marking up station names and lines. The default is to specify no language.')

  parser.add_argument('-L', '--local-languages',
    required=False,
    help='If specified, provides a list of languages when to show local name. Comma-separated')

  options = parser.parse_args()

  compile_stops_from_csv(options.input_csv[0], options.output, options.version, options.tts_hint_language, options.operators, options.local_languages)

if __name__ == '__main__':
  main()
