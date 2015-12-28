#!/usr/bin/env python3
"""
compile_stops_from_gtfs.py
Compiles stop database from GTFS data and reader ID.

Copyright 2015 Michael Farrell <micolous+git@gmail.com>

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

from argparse import ArgumentParser, FileType
from datetime import datetime, timedelta
from gtfstools import Gtfs, GtfsDialect
import csv, sqlite3

DB_SCHEMA = """
CREATE TABLE stops (
	id unique,
	name,
	y,
	x
	%(extra_fields)s
);
"""

INSERT_QUERY = 'INSERT INTO stops VALUES (?, ?, ?, ? %(extra_fields)s)'
VERSION_EPOCH = datetime(2006, 1, 1)

def massage_name(name, suffixes):
	name = name.strip()
	for suffix in suffixes:
		if name.endswith(suffix):
			name = name[:-len(suffix)].strip()
	
	return name


def empty(s):
	return s is None or s.strip() == ''

def compile_stops_from_gtfs(input_gtfs_f, output_f, matching_f=None, version=None, strip_suffixes='', extra_fields='', extra_fields_from_child=False):
	# trim whitespace
	strip_suffixes = [x.strip() for x in strip_suffixes.split(',')]
	extra_fields = [x.strip() for x in extra_fields.split(',')]
	
	if extra_fields:
		db_schema = DB_SCHEMA % dict(extra_fields=',' + (','.join(extra_fields)))
		insert_query = INSERT_QUERY % dict(extra_fields=', ?' * len(extra_fields))
	else:
		db_schema = DB_SCHEMA % dict(extra_fields='')
		insert_query = INSERT_QUERY % dict(extra_fields='')
	
	gtfs = Gtfs(input_gtfs_f)

	if version is None:
		feed_info = gtfs.open('feed_info.txt')
		row = feed_info.next()
		feed_start_date = row['feed_start_date']
		assert len(feed_start_date) == 8
		feed_start_date = datetime.strptime(feed_start_date, '%Y%m%d')
		version = (feed_start_date - VERSION_EPOCH).days
		print 'Data version: %s (%s)' % (version, feed_start_date.date().isoformat())

	stops = gtfs.open('stops.txt')
	if extra_fields_from_child:
		child_data = {}

	parent_station_extras = {}

	db = sqlite3.connect(output_f)
	cur = db.cursor()
	cur.execute(db_schema)

	# See if there is a matching file
	if matching_f is None:
		# No matching data, dump all stops.
		stop_map = map(lambda stop: [stop['stop_id'], massage_name(stop['stop_name'], strip_suffixes), stop['stop_lat'].strip(), stop['stop_lon'].strip()] + [stop[x] for x in extra_fields],
			stops)

		cur.executemany(insert_query, stop_map)
	else:
		# Matching data is available.  Lets use that.
		matching = csv.reader(matching_f)
		matching_header = matching.next()
		matching_reader = matching_header.index('reader_id')
		matching_code = matching_header.index('stop_code')
		matching_id = matching_header.index('stop_id')
		
		stop_codes = {}
		stop_ids = {}
		for match in matching:
			if match[matching_code]:
				if match[matching_code] not in stop_codes:
					stop_codes[match[matching_code]] = []
				stop_codes[match[matching_code]].append(match[matching_reader])
			elif match[matching_id]:
				if match[matching_id] not in stop_ids:
					stop_ids[match[matching_id]] = []
				stop_ids[match[matching_id]].append(match[matching_reader])
			else:
				raise Exception, 'neither stop_id or stop_code specified in row'
		
		# Now run through the stops
		for stop in stops:
			# preprocess stop data
			name = massage_name(stop['stop_name'], strip_suffixes)
			y = stop['stop_lat'].strip()
			x = stop['stop_lon'].strip()
			if extra_fields_from_child and not empty(stop['parent_station']):
				parent = stop['parent_station'].strip()
				if parent in stop_ids and parent not in child_data:
					# This is child has a parent we are interested in, and don't
					# already have.
					child_data[parent] = {}
					for k in extra_fields:
						child_data[parent][k] = stop[k]

			e = [child_data[stop['stop_id']][i] if empty(stop[i]) and stop['stop_id'] in child_data else stop[i]  for i in extra_fields]

			if stop['stop_id'] in stop_ids:
				cur.executemany(insert_query, map(lambda r: [r, name, y, x] + e, stop_ids[stop['stop_id']]))
			if stop['stop_code'] in stop_codes:
				cur.executemany(insert_query, map(lambda r: [r, name, y, x] + e, stop_codes[stop['stop_code']]))
		matching_f.close()

	cur.execute('PRAGMA user_version = %d' % version)
	db.commit()
	db.close()
	
		

def main():
	parser = ArgumentParser()
	
	parser.add_argument('-o', '--output',
		required=True,
		help='Output data file (sqlite3)'
	)

	parser.add_argument('input_gtfs',
		nargs=1,
		type=FileType('rb'),
		help='Path to GTFS ZIP file to extract data from.')
	
	parser.add_argument('-m', '--matching',
		required=False,
		type=FileType('rb'),
		help='If supplied, this is a matching file of reader_id to stop_code or stop_id. Missing stops will be dropped. If a matching file is not supplied, this will produce a list of all stops with stop_code instead.')

	parser.add_argument('-V', '--override-version',
		required=False,
		type=int,
		help='Enter a custom version to write to the file. If not specified, this tool will read it from feed_info.txt, reading the feed_start_date and converting it to a number of days since %s.' % (VERSION_EPOCH.date().isoformat(),))

	parser.add_argument('--strip-suffixes',
		default='railway station,train station,station',
		help='Comma separated, case-insensitive list of suffixes to remove from station names when populating the database. [default: %(default)s]')

	parser.add_argument('-e', '--extra-fields',
		required=False,
		help='Comma separated list of additional fields to be included in the database output.')
	
	parser.add_argument('--extra-fields-from-child',
		action='store_true',
		help='If set, this will attempt to collect extra fields from child stations, and use it on the parent stations if the station is unset or empty. This only works when matching the parent by their stop_id, and if the parent stops are listed after the child stops.')

	options = parser.parse_args()

	compile_stops_from_gtfs(options.input_gtfs[0], options.output, options.matching, options.override_version, options.strip_suffixes, options.extra_fields, options.extra_fields_from_child)

if __name__ == '__main__':
	main()

