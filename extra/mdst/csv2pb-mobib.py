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

metrolinesraw = {
  # 0-0xff are buses
  0x101: "1A",
  0x102: "1A/1B",
  0x103: "1A/1B/2",
  0x104: "1A/2",
  0x105: "1B",
  0x106: "2",
  0x107: "2/PremNS",
  0x108: "Ligne",
  0x109: "North-South",
  0x10a: "West",
}

metrolinesinv = dict((v, k) for (k,v) in metrolinesraw.items())
metrolines = dict((k, (v,)) for (k,v) in metrolinesraw.items())

def convert_coords(row):
  x = row.get('Coord_x', None) or row.get('coord_x', None)
  y = row.get('Coord_y', None) or row.get('coord_y', None)
  if not x or not y:
    return (0, 0)
  x = int(x)
  y = int(y)
  # Earth is not flat but flat is good enough approximation for
  # a small area like Brussels and low precision that we need
  # Bigger problem is that the plan used by mobib-extractor isn't very
  # accurate and is a plan and not a map.

  lat = -5.471576898118017e-05 * (y - 425) + 50.896530
  lon = 8.616420503909665e-05 * (x - 103) + 4.266989
  return (lat,lon)


def compile_stops_from_csv(metro_f, bus_f, output_f, version=None, tts_hint_language=None, operators_f=None, local_languages=None):
  metro_f = codecs.getreader('utf-8-sig')(metro_f)
  bus_f = codecs.getreader('utf-8-sig')(bus_f)

  operators = {}

  if operators_f is not None:
    operators_f = codecs.getreader('utf-8-sig')(operators_f)
    operators = mdst.read_operators_from_csv(operators_f)
    operators_f.close()

  db = mdst.MdstWriter(
    fh=open(output_f, 'wb'),
    version=version,
    operators=operators,
    lines=metrolines,
    local_languages=local_languages.split(',') if local_languages is not None else [],
    tts_hint_language=tts_hint_language,
  )

  read_metro_stops(db, metro_f)
  metro_f.close()

  read_bus_stops(db, bus_f)
  bus_f.close()

  index_end_off = db.finalise()

  print('Finished writing database.')


def read_metro_stops(db, csv_f):
  exread = csv.DictReader(csv_f)

  for stop in exread:
    s = Station()
    s.id = int(stop['Station'], 2)
    s.id |= int(stop['Sous-zone'], 2) << 7
    s.id |= int(stop['Zone'], 2) << 11
    s.id |= int(stop['Type'], 2) << 22
    s.name.local = stop['Arret']
    s.line_id = metrolinesinv[stop['Ligne']]
    (s.latitude, s.longitude) = convert_coords(stop)

    db.push_station(s)


def read_bus_stops(db, csv_f):
  exread = csv.DictReader(csv_f)

  for stop in exread:
    s = Station()
    s.id = int(stop['Code'])
    s.id |= int(stop['Bus']) << 13
    s.id |= 0xf << 22
    s.name.local = stop['Arret']
    (s.latitude, s.longitude) = convert_coords(stop)

    db.push_station(s)


def main():
  parser = ArgumentParser()
  
  parser.add_argument('-o', '--output',
    required=True,
    help='Output data file (MdST)'
  )

  parser.add_argument('input_csv',
    nargs=2,
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

  compile_stops_from_csv(options.input_csv[0], options.input_csv[1], options.output, options.version, options.tts_hint_language, options.operators, options.local_languages)

if __name__ == '__main__':
  main()
