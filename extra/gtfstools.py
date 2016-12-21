#!/usr/bin/env python
# -*- mode: python; indent-tabs-mode: nil; tab-width: 2 -*-
"""
gtfstools.py
Helper functions for working with GTFS

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

import zipfile, csv
try:
  from cStringIO import StringIO
except ImportError:
  from StringIO import StringIO

class GtfsDialect(csv.excel):
  lineterminator = '\n'

def swallow_windows_unicode(fileobj, rewind=True):
  """
  Windows programs (specifically, Notepad) puts '\xef\xbb\xbf' at the start of
  a Unicode text file.  This is used to handle "utf-8-sig" files.

  This function looks for those bytes and advances the stream past them if
  they are present.

  Returns fileobj, fast-forwarded past the characters.
  
  This implementation is taken from gtfs2geojson.py (which is BSD licensed).
  """
  if rewind:
    try:
      pos = fileobj.tell()
    except:
      pos = None

  try:
    bom = fileobj.read(3)
  except:
    # End of file, revert!
    fileobj.seek(pos)
  if bom == '\xef\xbb\xbf':
    return fileobj

  # Bytes not present, rewind the stream
  if rewind:
    if pos is None:
      # .tell is not supported, dump the file contents into a cStringID
      fileobj = StringIO(bom + fileobj.read())
    else:
      fileobj.seek(pos)
  return fileobj


class Gtfs(object):
  def __init__(self, gtfs_f):
    self.gtfs_zip = zipfile.ZipFile(gtfs_f, 'r')
  
  def open(self, filename):
    return csv.DictReader(swallow_windows_unicode(self.gtfs_zip.open(filename, 'r')), restval=None)

  def infolist(self):
    return self.gtfs_zip.infolist()

