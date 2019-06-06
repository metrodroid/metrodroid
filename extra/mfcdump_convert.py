#!/usr/bin/env python3
# -*- mode: python; indent-tabs-mode: nil; tab-width: 2 -*-
"""
mfcdump_convert.py - Converts a mfoc/mfcuk .mfc dump file for MIFARE
Classic to various formats, and extracts keys.

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
from argparse import ArgumentParser, FileType
from binascii import a2b_hex
from base64 import b16encode, b64encode
from io import TextIOWrapper
from os.path import basename, getmtime
from xml.etree import ElementTree as etree
import csv, itertools, json

FORMATS = [
  'md34',     # Adds keytype = ('KeyA', 'KeyB')
  'md31',     # Adds key
  'farebot',  # No bonus attributes
  
  # These options emit keys.
  # See https://github.com/micolous/metrodroid/wiki/Importing-MIFARE-Classic-keys
  # for file format details
  'raw-keys',         # Raw key format (from Farebot)
  'json-keys',        # JSON
  'json-no-uid-keys', # JSON
  'json-static-keys', # JSON (static key mode)
  
  # Third-party card formats
  'mfc',       # Raw MFC dump
  'mct',       # MIFARE Classic Tool -- https://github.com/ikarus23/MifareClassicTool/blob/master/tools/example-files/example-dump-file.txt
  'csv-keys',  # CSV file with keys
]

def is_xml(format_s):
  return format_s.startswith('md') or format_s == 'farebot'


class KeyBase:
  def make_block(self, block_no, data):
    pass

  def make_sector(self, sector_no, blocks, key):
    return key[0]


class RawKeys(KeyBase):
  def make_card(self, uid, scanned_at, label, sectors):
    return b''.join(sectors)

  def write_cards(self, output_f, cards):
    for card in cards:
      output_f.write(card)


class JsonKeys(KeyBase):
  def __init__(self, format_s):
    self.format = format_s

  def make_sector(self, sector_no, blocks, key):
    d = []
    if key[0] is not None:
      d += [{
        'keytype': 'KeyA',
        'key': b16encode(key[0]).decode('ascii').lower(),
        'sector': sector_no
      }]
    if key[1] is not None:
      d += [{
        'keytype': 'KeyB',
        'key': b16encode(key[1]).decode('ascii').lower(),
        'sector': sector_no
      }]
    return d

  def make_card(self, uid, scanned_at, label, sectors):
    d = {
      'KeyType': 'MifareClassicStatic' if self.format == 'json-static-keys' else 'MifareClassic',
      'Description': label,
      'keys': list(itertools.chain(*sectors))
    }
    if self.format == 'json-keys':
      d['TagId'] = b16encode(uid).decode('ascii').lower()
    return d

  def write_cards(self, output_f, cards):
    if self.format != 'json-static-keys' and len(cards) != 1:
      raise Exception('expected only 1 card in %s format' % self.format)

    card = cards[0]
    if self.format == 'json-static-keys':
      # merge the keys
      for c in cards[1:]:
        card['keys'] += c['keys']

    json.dump(card, output_f)


class CsvKeys(KeyBase):
  def make_sector(self, sector_no, blocks, key):
    return [(str(sector_no), b16encode(key[0]).decode('ascii').lower()),
            (str(sector_no), b16encode(key[1]).decode('ascii').lower())]

  def make_card(self, uid, scanned_at, label, sectors):
    uid = b16encode(uid).decode('ascii').lower()
    return list(itertools.chain([[uid, s[0][0], s[0][1]], [uid, s[1][0], s[1][1]]] for s in sectors))

  def write_cards(self, output_f, cards):
    output_c = csv.writer(output_f)
    output_c.writerow(['uid', 'sector', 'key'])
    for card in cards:
      for s in card:
        output_c.writerow(s)


class Mct:
  # https://github.com/ikarus23/MifareClassicTool/blob/master/tools/example-files/example-dump-file.txt
  def make_block(self, block_no, data):
    return b16encode(data).decode('ascii').upper() + '\n'

  def make_sector(self, sector_no, blocks, key):
    yield '+Sector: %d\n' % sector_no
    for block in blocks:
      yield block
  
  def make_card(self, uid, scanned_at, label, sectors):
    return itertools.chain(*sectors)

  def write_cards(self, output_f, cards):
    if len(cards) != 1:
      raise Exception('expected only 1 card in mct format')

    for l in cards[0]:
      output_f.write(l)


class Mfc:
  def make_block(self, block_no, data):
    return data

  def make_sector(self, sector_no, blocks, key):
    for block in blocks:
      yield block

  def make_card(self, uid, scanned_at, label, sectors):
    return itertools.chain(*sectors)

  def write_cards(self, output_f, cards):
    if len(cards) != 1:
      raise Exception('expected only 1 card in mfc format')

    for l in cards[0]:
      output_f.write(l)


class MetrodroidXml:
  def __init__(self, format_s):
    self.format = format_s

  def make_block(self, block_no, data):
    #print('make_block(%r, %r)' % (block_no, data))
    # FIXME: support multiple block types
    block = etree.Element('block', index=str(block_no), type='data')
    block_data = etree.SubElement(block, 'data')
    block_data.text = b64encode(data).decode('ascii')
    return block

  
  def make_sector(self, sector_no, blocks, key):
    #print('make_sector(%r, %r, %r)' % (sector_no, blocks, key))
    sector = etree.Element('sector', index=str(sector_no))
    sector_blocks = etree.SubElement(sector, 'blocks')
    sector_blocks.extend(blocks)
    if self.format != 'farebot':
      sector.attrib['key'] = b64encode(key[0]).decode('ascii')
    if self.format == 'md34':
      sector.attrib['keytype'] = 'KeyA'

    return sector
  
  def make_card(self, uid, scanned_at, label, sectors):
    #print('make_card(%r, %r, %r, %r)' % (uid, scanned_at, label, sectors))
    card = etree.Element('card',
      type='0',
      id=b16encode(uid).decode('ascii').lower(),
      scanned_at=str(scanned_at*1000),
      label=label
    )
    card_sectors = etree.SubElement(card, 'sectors')
    card_sectors.extend(sectors)
    return card
  
  
  def write_cards(self, output_f, cards):
    if len(cards) > 1:
      root = etree.Element('cards')
      root.extend(cards)
    else:
      root = cards[0]

    # We have made a structure, dump it to disk
    tree = etree.ElementTree(root)
    tree.write(output_f, encoding="utf-8", xml_declaration=True)


def read_mfc_dump(input_f, writer):
  # Read the MIFARE card entirely first
  card_data = input_f.read()

  # Card data should be 1K or 4K
  assert len(card_data) in (1024, 4096)

  if len(card_data) == 1024:
    sector_count = 16
  elif len(card_data) == 4096:
    sector_count = 40

  # Lets make some XML.
  sectors = []
  for sector_no in range(sector_count):
    if sector_no < 32:
      block_count = 4
    else:
      block_count = 16

    block_data = None
    blocks = []
    for block_no in range(block_count):
      if sector_no < 32:
        offset = (sector_no * 64) + (block_no * 16)
      else:
        offset = 2048 + ((sector_no - 32) * 256) + (block_no * 16)

      block_data = card_data[offset:offset+16]
      blocks.append(writer.make_block(block_no, block_data))

    # Put key A into the key attribute
    sectors.append(writer.make_sector(sector_no, blocks, (block_data[:6],
                                                          block_data[10:])))

  return writer.make_card(
    uid=card_data[0:4],
    scanned_at=int(getmtime(input_f.name)),
    label=basename(input_f.name),
    sectors=sectors
  )


def read_metro_json(input_f, writer):
  # Read the MIFARE card entirely first
  card_data = json.load(input_f)

  if 'mifareClassic' not in card_data:
    raise Exception('expected mifareClassic dump')

  sectors = []

  for sector_no, sector in enumerate(card_data['mifareClassic']['sectors']):
    blocks = []
    keyA = None
    keyB = None
    last_block = len(sector['blocks']) - 1
    for block_no, block in enumerate(sector['blocks']):
      block_data = a2b_hex(block)

      if block_no == last_block:
        # insert the key
        if 'keyA' in sector:
          keyA = a2b_hex(sector['keyA'])
          block_data = keyA + block_data[6:]
        if 'keyB' in sector:
          keyB = a2b_hex(sector['keyB'])
          block_data = block_data[:-6] + keyB

      blocks.append(writer.make_block(block_no, block_data))

    sectors.append(writer.make_sector(sector_no, blocks, (keyA, keyB)))

  return writer.make_card(
    uid=a2b_hex(card_data['tagId']),
    scanned_at=int(getmtime(input_f.name)),
    label=basename(input_f.name),
    sectors=sectors
  )


INPUT_FORMATS = {
  'mfc': read_mfc_dump,
  'mjson': read_metro_json,
  'metrojson': read_metro_json,
}


def mfc_converter(input_fs, output_f, format_s, informat_s):
  writer = None
  if is_xml(format_s):
    writer = MetrodroidXml(format_s)
  elif format_s == 'raw-keys':
    writer = RawKeys()
  elif format_s.startswith('json-'):
    writer = JsonKeys(format_s)
  elif format_s == 'csv-keys':
    writer = CsvKeys()
  elif format_s == 'mct':
    writer = Mct()
  elif format_s == 'mfc':
    writer = Mfc()
  else:
    raise Exception

  reader = INPUT_FORMATS.get(informat_s)
  if reader is None:
    raise Exception

  cards = []

  for input_f in input_fs:
    card = reader(input_f, writer)
    input_f.close()
    cards.append(card)

  if is_xml(format_s) or format_s in ('raw-keys', 'mfc'):
    writer.write_cards(output_f, cards)
    output_f.flush()
    output_f.close()
  else:
    with TextIOWrapper(output_f, encoding='utf-8') as wrapper:
      writer.write_cards(wrapper, cards)


def main():
  parser = ArgumentParser()
  parser.add_argument('input_mfc', nargs='+', type=FileType('rb'),
    help='Card MFC dumps to read')

  parser.add_argument('-o', '--output', type=FileType('wb'),
    help='Output Farebot XML to write')

  parser.add_argument('-f', '--format', choices=FORMATS, default=FORMATS[0],
    help='File format to emit')

  parser.add_argument('-F', '--input_format', choices=INPUT_FORMATS.keys(),
                      default=list(INPUT_FORMATS.keys())[0])

  options = parser.parse_args()
  mfc_converter(options.input_mfc, options.output,
                options.format, options.input_format)


if __name__ == '__main__':
  main()

