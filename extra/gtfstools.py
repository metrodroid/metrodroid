#!/usr/bin/env python3
# -*- mode: python; indent-tabs-mode: nil; tab-width: 2 -*-
"""
gtfstools.py
Helper functions for working with GTFS

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

import zipfile, csv, codecs


class GtfsDialect(csv.excel):
  lineterminator = '\n'

class Gtfs(object):
  def __init__(self, gtfs_f):
    self.gtfs_zip = zipfile.ZipFile(gtfs_f, 'r')

  def open(self, filename):
    return csv.DictReader(
      codecs.getreader('utf-8-sig')(self.gtfs_zip.open(filename)),
      restval=None)

  def infolist(self):
    return self.gtfs_zip.infolist()

