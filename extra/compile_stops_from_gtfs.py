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
from gtfstools import Gtfs, GtfsDialect
import csv

def compile_stops_from_gtfs(input_gtfs_f, output_f, matching_f=None):
	gtfs = Gtfs(input_gtfs_f)
	stops = gtfs.open('stops.txt')
	stops_header = stops.next()
	stop_id = stops_header.index('stop_id')
	stop_code = stops_header.index('stop_code')
	stop_name = stops_header.index('stop_name')
	stop_y = stops_header.index('stop_lat')
	stop_x = stops_header.index('stop_lon')

	output_c = csv.writer(output_f, dialect=GtfsDialect)
	output_c.writerow(['id', 'name', 'y', 'x'])

	# See if there is a matching file
	if matching_f is None:
		# No matching data, dump all stops.
		for stop in stops:
			output_c.writerow([
				stop[stop_id],
				stop[stop_name],
				stop[stop_y],
				stop[stop_x]
			])
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
			if stop[stop_id] in stop_ids:
				for r in stop_ids[stop[stop_id]]:
					output_c.writerow([
						r,
						stop[stop_name].strip(),
						stop[stop_y].strip(),
						stop[stop_x].strip()
					])
			if stop[stop_code] in stop_codes:
				for r in stop_codes[stop[stop_code]]:
					output_c.writerow([
						r,
						stop[stop_name].strip(),
						stop[stop_y].strip(),
						stop[stop_x].strip()
					])
		matching_f.close()
	output_f.close()
	
		

def main():
	parser = ArgumentParser()
	
	parser.add_argument('-o', '--output',
		required=True,
		type=FileType('wb'),
		help='Output data file'
	)

	parser.add_argument('input_gtfs',
		nargs=1,
		type=FileType('rb'),
		help='Path to GTFS ZIP file to extract data from.')
	
	parser.add_argument('-m', '--matching',
		required=False,
		type=FileType('rb'),
		help='If supplied, this is a matching file of reader_id to stop_code or stop_id. Missing stops will be dropped. If a matching file is not supplied, this will produce a list of all stops with stop_code instead.')

	options = parser.parse_args()

	compile_stops_from_gtfs(options.input_gtfs[0], options.output, options.matching)

if __name__ == '__main__':
	main()

