#!/usr/bin/env python3
# -*- mode: python; indent-tabs-mode: nil; tab-width: 2 -*-
"""
compile_stops_from_gtfs.py
Compiles MdST stop database from GTFS data and reader ID.

Copyright 2015-2018 Michael Farrell <micolous+git@gmail.com>

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
from gtfstools import Gtfs, GtfsDialect
from stations_pb2 import Station, Operator, TransportType
import mdst
import codecs, csv

VERSION_EPOCH = datetime(2006, 1, 1)

def massage_name(name, suffixes):
  name = name.strip()
  for suffix in suffixes:
    if name.lower().endswith(suffix):
      name = name[:-len(suffix)].strip()
  
  return name

def empty(s):
  return s is None or s.strip() == ''

def compile_stops_from_gtfs(input_gtfs_f, output_f, all_matching_f=None, version=None,
                            strip_suffixes='', agency_id=-1, tts_hint_language=None,
                            operators_f=None, extra_f=None, local_languages=None,
                            license_notice_f=None):
  if all_matching_f is not None:
    all_matching_f = [codecs.getreader('utf-8-sig')(x) for x in all_matching_f]
  if operators_f is not None:
    operators_f = codecs.getreader('utf-8-sig')(operators_f)
  if extra_f is not None:
    extra_f = codecs.getreader('utf-8-sig')(extra_f)
  # trim whitespace
  strip_suffixes = [x.strip().lower() for x in strip_suffixes.split(',')]

  all_gtfs = [Gtfs(x) for x in input_gtfs_f]
  first_gtfs = all_gtfs[0]

  if version is None:
    try:
      feed_info = first_gtfs.open('feed_info.txt')
    except KeyError:
      # feed_info.txt is not in the file. Find the newest file in the archive
      feed_start_date = None
      for f in first_gtfs.infolist():
        ts = datetime(*f.date_time)
        if feed_start_date is None or feed_start_date < ts:
          feed_start_date = ts
    else:
      row = next(feed_info)
      feed_start_date = row['feed_start_date']
      assert len(feed_start_date) == 8
      feed_start_date = datetime.strptime(feed_start_date, '%Y%m%d')

    version = (feed_start_date - VERSION_EPOCH).days
    print('Data version: %s (%s)' % (version, feed_start_date.date().isoformat()))

  operators = {}

  if operators_f is not None:
    operators = mdst.read_operators_from_csv(operators_f)
    operators_f.close()


  db = mdst.MdstWriter(
    fh=open(output_f, 'wb'),
    version=version,
    operators=operators,
    local_languages=local_languages.split(',') if local_languages is not None else [],
    tts_hint_language=tts_hint_language,
    license_notice_f=license_notice_f,
  )

  station_count = 0

  for num, gtfs in enumerate(all_gtfs):
    stops = gtfs.open('stops.txt')
    # See if there is a matching file
    if all_matching_f is not None and len(all_matching_f) > num:
      matching_f = all_matching_f[num]
    else:
      matching_f = None
    if matching_f is None:
      # No matching data, dump all stops.
      stop_map = map(lambda stop: [stop['stop_id'], massage_name(stop['stop_name'], strip_suffixes), stop['stop_lat'].strip(), stop['stop_lon'].strip()],
        stops)

      for stop_id, stop_name, lat, lon in stop_map:
        s = Station()
        s.id = int(stop_id)
        s.name.english = stop_name
        if lat and lon:
          s.latitude = float(lat)
          s.longitude = float(lon)

        db.push_station(s)
        station_count += 1
    else:
      # Matching data is available.  Lets use that.
      matching = csv.DictReader(matching_f)

      stop_codes = {}
      stop_ids = {}
      short_names = {}
      for match in matching:
        if 'stop_code' in match and match['stop_code']:
          if match['stop_code'] not in stop_codes:
            stop_codes[match['stop_code']] = []
          stop_codes[match['stop_code']].append(match['reader_id'])
        elif 'stop_id' in match and match['stop_id']:
          if match['stop_id'] not in stop_ids:
            stop_ids[match['stop_id']] = []
          stop_ids[match['stop_id']].append(match['reader_id'])
        else:
          raise Exception('neither stop_id or stop_code specified in row')
        if 'short_name' in match and match['short_name']:
          short_names[match['reader_id']] = match['short_name']

      total_gtfs_stations = 0
      dropped_gtfs_stations = 0

      # Now run through the stops
      for stop in stops:
        # preprocess stop data
        name = massage_name(stop['stop_name'], strip_suffixes)
        y = float(stop['stop_lat'].strip())
        x = float(stop['stop_lon'].strip())

        used = False

        # Insert rows where a stop_id is specified for the reader_id
        stop_rows = []
        for reader_id in stop_ids.get(stop.get('stop_id', 'stop_id_absent'), []):
          s = Station()
          s.id = int(reader_id, 0)
          s.name.english = name
          if y and x:
            s.latitude = y
            s.longitude = x
          if reader_id in short_names:
            s.name.english_short = short_names[reader_id]
          if agency_id >= 0:
            s.operator_id = agency_id

          db.push_station(s)
          station_count += 1
          used = True

        # Insert rows where a stop_code is specified for the reader_id
        stop_rows = []
        for reader_id in stop_codes.get(stop.get('stop_code', 'stop_code_absent'), []):
          s = Station()
          s.id = int(reader_id, 0)
          s.name.english = name

          if y and x:
            s.latitude = y
            s.longitude = x

          if reader_id in short_names:
            s.name.english_short = short_names[reader_id]
          if agency_id >= 0:
            s.operator_id = agency_id

          db.push_station(s)
          station_count += 1
          used = True
        total_gtfs_stations += 1
        if not used:
          dropped_gtfs_stations += 1

      matching_f.close()
      print('Finished parsing GTFS ' + str(num) + '.  Here\'s the stats:')
      print(' - Dropped %d out of %d GTFS stations' % (dropped_gtfs_stations,
                                                     total_gtfs_stations))
      print()

  if extra_f is not None:
    mdst.read_stops_from_csv(db, extra_f)
    extra_f.close()

  index_end_off = db.finalise()

  print('Finished writing database.  Here\'s the stats:')
  print(' - total ............ %8d stations' % station_count)
  print('                      %8d bytes' % index_end_off)
  print()
  station_count = float(station_count)
  print(' - header ........... %8d bytes' % db.stationlist_off)
  stations_len = (db.index_off - db.stationlist_off)
  print(' - stations ......... %8d bytes (%.1f per record)' % (stations_len, stations_len / station_count))
  index_len = (index_end_off - db.index_off)
  print(' - index ............ %8d bytes (%.1f per record)' % (index_len, index_len / station_count))

def main():
  parser = ArgumentParser()
  
  parser.add_argument('-o', '--output',
    required=True,
    help='Output data file (MdST)'
  )

  parser.add_argument('input_gtfs',
    nargs='+',
    type=FileType('rb'),
    help='Path to GTFS ZIP file to extract data from.')
  
  parser.add_argument('-m', '--matching',
    required=False,
    action='append',
    type=FileType('rb'),
    help='If supplied, this is a matching file of reader_id to stop_code or stop_id. Missing stops will be dropped. If a matching file is not supplied, this will produce a list of all stops with stop_code instead.')

  parser.add_argument('-x', '--extra',
                      required=False,
                      type=FileType('rb'),
                      help='If supplied, this is a file with extra stops not derived from gtfs.')

  parser.add_argument('-p', '--operators',
    required=False,
    type=FileType('rb'),
    help='If supplied, this is an operators file of mapping between ids and operators. If a matching file is not supplied, this will produce an empty list of operators.')

  parser.add_argument('-V', '--override-version',
    required=False,
    type=int,
    help='Enter a custom version to write to the file. If not specified, this tool will read it from feed_info.txt, reading the feed_start_date and converting it to a number of days since %s.' % (VERSION_EPOCH.date().isoformat(),))

  parser.add_argument('--strip-suffixes',
    default='railway station,train station,station',
    help='Comma separated, case-insensitive list of suffixes to remove from station names when populating the database. [default: %(default)s]')

  parser.add_argument('-a', '--agency-id',
    default=-1,
    type=int,
    required=False,
    help='If specified, annotates the stops from this GTFS data with an agency ID (integer). This is not the agency data included in the GTFS feed. [default: %(default)s]')

  parser.add_argument('-l', '--tts-hint-language',
    required=False,
    help='If specified, provides a hint for LocaleSpan when marking up station names and lines. The default is to specify no language.')

  parser.add_argument('-L', '--local-languages',
    required=False,
    help='If specified, provides a list of languages when to show local name. Comma-separated')

  parser.add_argument('-n', '--license-notice',
    required=False,
    type=FileType('r'),
    help='If specified, the file from which to read a license notice from.')

  options = parser.parse_args()

  compile_stops_from_gtfs(options.input_gtfs, options.output, options.matching, options.override_version, options.strip_suffixes, options.agency_id, options.tts_hint_language, options.operators, options.extra, options.local_languages, options.license_notice)

if __name__ == '__main__':
  main()

