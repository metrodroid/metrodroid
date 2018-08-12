#!/usr/bin/env python3
# -*- mode: python; indent-tabs-mode: nil; tab-width: 2; coding: utf-8 -*-
"""
mdst.py - Helper for writing MdST files

Copyright 2018 Michael Farrell <micolous+git@gmail.com>

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
from __future__ import absolute_import, print_function
from google.protobuf.internal import encoder
from stations_pb2 import StationDb, Operator, Line, Station, StationIndex
import struct

SCHEMA_VER = 1

def delimited_value(msg):
  # Emits a writeDelimited compatible Protobuf message
  o = msg.SerializeToString()
  d = encoder._VarintBytes(len(o))
  return d + o

class MdstWriter(object):
  def __init__(self, fh, version, local_languages=None, operators=None, lines=None, tts_hint_language=None):
    """
    Creates a new MdST database.
    
    This class must be initialised with the parameters in the StationDb header.
    
    fh: required, file-like object to write to.
    version: required, this is a numeric revision number for the database.
    local_languages: optional, list of languages which should be treated as "local".
    operators: optional, dict of [int](name.english,name.local) declaring a mapping of operators.
    lines: optional, dict of [int](name.english,name.local) declaring a mapping of lines.
    tts_hint_language: optional, str of LocaleSpan hint for station names.
    
    
    """
    sdb = StationDb()
    sdb.version = version
    if tts_hint_language:
      sdb.tts_hint_language = tts_hint_language
    
    if local_languages:
      for l in local_languages:
        sdb.local_languages.append(l)

    if operators:
      for k, v in operators.items():
        if isinstance(v, Operator):
          sdb.operators[k].name.english = v.name.english
          sdb.operators[k].name.english_short = v.name.english_short
          sdb.operators[k].name.local = v.name.local
          sdb.operators[k].name.local_short = v.name.local_short
          continue
        if v[0] != None:
          sdb.operators[k].name.english = v[0]
        if len(v) > 1 and v[1] != None:
          sdb.operators[k].name.local = v[1]

    if lines:
      for k, v in lines.items():
        if v[0] != None:
          sdb.lines[k].name.english = v[0]
        if len(v) > 1 and v[1] != None:
          sdb.lines[k].name.local = v[1]

    # Write out the header
    fh.write(b'MdST')
    fh.write(struct.pack('!II', SCHEMA_VER, 0))
    fh.write(delimited_value(sdb))
    
    self.stationlist_off = fh.tell()
    self.fh = fh
    self.stations = {}

  def push_station(self, station):
    """
    Adds a station entry. Expects a Station message.
    """
    self.stations[station.id] = self.fh.tell() - self.stationlist_off
    self.fh.write(delimited_value(station))

  def finalise(self):
    """
    Finalises the database by writing the index, and closing the file.

    Returns the total size of the file.
    """
    self.index_off = self.fh.tell()
    sidx = StationIndex()
    for station_id, offset in self.stations.items():
      sidx.station_map[station_id] = offset
    self.fh.write(delimited_value(sidx))
    
    index_end_off = self.fh.tell()

    # Write the location of the index
    self.fh.seek(4+4)
    self.fh.write(struct.pack('!I', self.index_off - self.stationlist_off))
    self.fh.close()
    
    return index_end_off


