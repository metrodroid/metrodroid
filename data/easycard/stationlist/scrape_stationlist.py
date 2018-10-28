#!/usr/bin/env python3
# -*- mode: python3; indent-tabs-mode: nil; tab-width: 2 -*-
"""
scrape_stationlist.py - Generates a list of station IDs for Taipei Metro
Copyright 2018 Michael Farrell <micolous+git@gmail.com>

Based on http://www.fuzzysecurity.com/tutorials/rfid/4.html

Updated for new Taipei Metro website: 
https://web.metro.taipei/c/selectstation2010.asp
https://web.metro.taipei/e/selectstation2010.asp

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

from argparse import ArgumentParser
from bs4 import BeautifulSoup
from csv import writer
from sys import stdin, stdout
from urllib.parse import urlparse, parse_qs

d = stdin.read()

soup = BeautifulSoup(d, 'html.parser')
e = soup.find('map')
o = []
for a in e.find_all('area'):
  if 'stationdetail2010.asp?ID=' not in a['href']:
    continue

  # Get the station IDs
  refs, code = parse_qs(urlparse(a['href']).query)['ID'][0].split('-')
  code = int(code)

  # Get the station name
  if not a['title'].startswith(refs):
    print(refs, a['title'])
    raise Exception('could not find refs in title')
    
  name = a['title'][len(refs):].strip()
  refs = refs.split(' ')
  o.append([code, name, ','.join(refs)])

o.sort(key=lambda x: x[0])

c = writer(stdout)
c.writerow(['reader_id', 'name', 'ref'])
for r in o:
  r[0] = hex(r[0])
  c.writerow(r)
