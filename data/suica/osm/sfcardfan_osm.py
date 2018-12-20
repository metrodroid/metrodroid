#!/usr/bin/env python3
# -*- mode: python; indent-tabs-mode: nil; tab-width: 2; coding: utf-8 -*-
"""
data/suica/osm/sfcardfan_osm.py - Reads in SFCardFan data and attempts matching to OSM data

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
from argparse import ArgumentParser, FileType
from csv import DictWriter
from decimal import Decimal
import xml.etree.ElementTree as ET

# Data normalisation
STATION = '駅' # eki
INSIDE_STATION = '駅構内'

HON = '本'
MAIN_LINE = '本線'
LINE = '線'

RAILWAY = '鉄道'
RAILWAY_LINE = '鉄道線'

def get_osm_tag(elem, key):
  e = elem.find('.//tag[@k=\'%s\']' % key)
  if e is None:
    return None
  return e.attrib['v']

class OsmNode:
  def __init__(self, elem):
    self.id = int(elem.get('id'))
    self.lat = Decimal(elem.get('lat'))
    self.lon = Decimal(elem.get('lon'))
    self.name_en = get_osm_tag(elem, 'name:en')
    self.name_ja = get_osm_tag(elem, 'name:ja') or get_osm_tag(elem, 'name')

class OsmRelation:
  def __init__(self, elem):
    self.id = int(elem.get('id'))
    self.train_route = get_osm_tag(elem, 'route') == 'train'
    self.name_en = get_osm_tag(elem, 'name:en')
    self.name_ja = get_osm_tag(elem, 'name:ja') or get_osm_tag(elem, 'name')
    self.stops = {}
    self.stop_ids = set()
    for e in elem.iterfind('.//member[@type=\'node\']'):
      self.stop_ids.add(int(e.attrib['ref']))
    self.stop_ids = frozenset(self.stop_ids)

    self.relation_ids = set()
    for e in elem.iterfind('.//member[@type=\'relation\']'):
      self.relation_ids.add(int(e.attrib['ref']))
    self.relation_ids = frozenset(self.relation_ids)

  def inject_stops(self, nodes):
    for i in self.stop_ids:
      if i in nodes:
        self.stops[i] = nodes[i]

  def __str__(self):
    o = 'Relation #%d: %s (%s)\n' % (self.id, self.name_en, self.name_ja)
    for r in self.relation_ids:
      o += '- Relation #%d\n' % r
    for s in self.stop_ids:
      if s in self.stops:
        stop = self.stops[s]
        o += '- Stop #%d: %s (%s)\n' % (stop.id, stop.name_en, stop.name_ja)
      else:
        o += '- Stop #%d: (unknown)\n' % (s,)
    return o
    

def read_osmdata(osm_xml_fn):
  tree = ET.parse(osm_xml_fn)
  root = tree.getroot()
  nodes = {}

  for nx in root.iter('node'):
    node = OsmNode(nx)
    nodes[node.id] = node

  relations = {}
  for rx in root.iter('relation'):
    relation = OsmRelation(rx)
    if not relation.train_route:
      # TODO
      continue
    
    # Wire up relations
    relation.inject_stops(nodes)

    print(relation)
    

def main():
  parser = ArgumentParser()
  parser.add_argument('-x', '--osm_xml', required=True)
  #parser.add_argument('-s', '--sfcardfan_csv')
  options = parser.parse_args()
  
  read_osmdata(options.osm_xml)

if __name__ == '__main__':
  main()
