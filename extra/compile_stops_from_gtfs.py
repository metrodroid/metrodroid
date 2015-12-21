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
);
"""

INSERT_QUERY = 'INSERT INTO stops VALUES (?, ?, ?, ?)'
VERSION_EPOCH = datetime(2008, 1, 1)


def compile_stops_from_gtfs(input_gtfs_f, output_f, matching_f=None, version=None):
	gtfs = Gtfs(input_gtfs_f)

	if version is None:
		feed_info = gtfs.open('feed_info.txt')
		feed_header = feed_info.next()
		feed_start_date = feed_info.next()[feed_header.index('feed_start_date')]
		assert len(feed_start_date) == 8
		feed_start_date = datetime.strptime(feed_start_date, '%Y%m%d')
		version = (feed_start_date - VERSION_EPOCH).days
		print 'Data version: %s (%s)' % (version, feed_start_date.date().isoformat())

	stops = gtfs.open('stops.txt')
	stops_header = stops.next()
	stop_id = stops_header.index('stop_id')
	stop_code = stops_header.index('stop_code')
	stop_name = stops_header.index('stop_name')
	stop_y = stops_header.index('stop_lat')
	stop_x = stops_header.index('stop_lon')

	db = sqlite3.connect(output_f)
	cur = db.cursor()
	cur.execute(DB_SCHEMA)

	# See if there is a matching file
	if matching_f is None:
		# No matching data, dump all stops.
		stop_map = map(lambda stop: (stop[stop_id], stop[stop_name].strip(), stop[stop_y].strip(), stop[stop_x].strip()),
			stops)

		cur.executemany(INSERT_QUERY, stop_map)
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
			name = stop[stop_name].strip()
			y = stop[stop_y].strip()
			x = stop[stop_x].strip()

			if stop[stop_id] in stop_ids:
				cur.executemany(INSERT_QUERY, map(lambda r: (r, name, y, x), stop_ids[stop[stop_id]]))
			if stop[stop_code] in stop_codes:
				cur.executemany(INSERT_QUERY, map(lambda r: (r, name, y, x), stop_codes[stop[stop_code]]))
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

	options = parser.parse_args()

	compile_stops_from_gtfs(options.input_gtfs[0], options.output, options.matching, options.override_version)

if __name__ == '__main__':
	main()

