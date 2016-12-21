#!/usr/bin/env python
# -*- mode: python; indent-tabs-mode: nil; tab-width: 2 -*-
"""
mfcdump_to_keys.py - Gets the keys from a mfoc/mfcuk .mfc dump file into
Farebot's key format

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
import csv

KEY_LENGTH = 6
KEYA_OFFSET = 48
KEYB_OFFSET = 58
SECTOR_LENGTH = 64
LONG_SECTOR_LENGTH = 256
LONG_SECTOR_OFFSET = 192

def mfc_to_farebot(input_fs, output_f, keyb=False, csv_format=False):
  if csv_format:
    output_c = csv.DictWriter(output_f, ['uid', 'sector', 'key'])
    output_c.writeheader()

  for input_f in input_fs:
    # Read the Mifare card entirely first
    card_data = input_f.read()
    key_offset = KEYB_OFFSET if keyb else KEYA_OFFSET

    # Card data should be 1K or 4K
    assert len(card_data) in (1024, 4096)
  
    if len(card_data) == 1024:
      sector_count = 16
    elif len(card_data) == 4096:
      sector_count = 40

    uid = card_data[:4]
  
    for sector_no in range(sector_count):
      if sector_no < 31:
        offset = (sector_no * SECTOR_LENGTH) + key_offset
      else:
        offset = (32 * SECTOR_LENGTH) + ((sector_no - 32) * LONG_SECTOR_LENGTH) + LONG_SECTOR_OFFSET + key_offset

      key = card_data[offset:offset+KEY_LENGTH]

      if csv_format:
        output_c.writerow(dict(uid=uid.encode('hex'), sector=str(sector_no), key=key.encode('hex')))
      else:
        output_f.write(key)

    input_f.close()

  output_f.flush()
  output_f.close()
  

def main():
  parser = ArgumentParser()
  parser.add_argument('input_mfc', nargs='+', type=FileType('rb'),
    help='Card MFC dump to read')

  parser.add_argument('-o', '--output', type=FileType('wb'),
    help='Output farebotkeys file to write')

  parser.add_argument('-b', '--key-b', action='store_true',
    help='Output Key B instead of Key A')

  parser.add_argument('-c', '--csv', action='store_true',
    help='Create a CSV file of keys instead of farebotkeys')

  options = parser.parse_args()
  mfc_to_farebot(options.input_mfc, options.output, options.key_b, options.csv)


if __name__ == '__main__':
  main()

