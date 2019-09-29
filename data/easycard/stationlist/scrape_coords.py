#!/usr/bin/env python3
# -*- mode: python; indent-tabs-mode: nil; tab-width: 2 -*-
"""
scrape-coords.py

Copyright 2019 Google

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
import codecs, csv
import urllib.parse
import urllib.request
from bs4 import BeautifulSoup


def trawl(csv_f, output_f, options):
  csv_fr = codecs.getreader('utf-8-sig')(csv_f)
  output_f = codecs.getwriter('utf-8')(output_f)

  exread = csv.DictReader(csv_fr)
  fns = exread.fieldnames
  fnsout = fns + [x for x in ['stop_lat', 'stop_lon'] if x not in fns]
  outcsv = csv.DictWriter(output_f, fnsout)
  outcsv.writeheader()

  for stop in exread:
    rid = int(stop['reader_id'], 0)
    if rid == 0x34:
      rid = 0x33
    ref = "%s-%03d" % (stop['ref'], rid)
    req = urllib.request.Request('http://web.metro.taipei/e/stationdetail2019.asp')
    vals = {
        'ID': ref
    }
    req.data = urllib.parse.urlencode(vals).encode('utf-8')
    response = urllib.request.urlopen(req)
    reply = response.read()
    soup = BeautifulSoup(reply, 'html.parser')
    td = soup.find_all("td", class_="stationinfo__td")[1]
    a = td.find('a')
    href = a.attrs['href']
    lp = urllib.parse.urlparse(href)
    lpq = urllib.parse.parse_qs(lp.query)
    stopout = {'stop_lat': lpq['Latitude'][0], 'stop_lon': lpq['Longitude'][0]}
    stopout.update(stop)
    outcsv.writerow(stopout)

  print('Finished.')


def main():
  parser = ArgumentParser()
  
  parser.add_argument('-o', '--output',
    required=True,
    type=FileType('wb'),
    help='Output file (CSV)'
  )

  parser.add_argument('input_csv',
    nargs=1,
    type=FileType('rb'),
    help='Path to CSV file to extract data from.')

  options = parser.parse_args()

  trawl(options.input_csv[0], options.output, options)

if __name__ == '__main__':
  main()
