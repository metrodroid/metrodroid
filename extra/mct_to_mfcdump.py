#!/usr/bin/env python3
# -*- mode: python; indent-tabs-mode: nil; tab-width: 2 -*-
"""
mct_to_mfcdump.py - Converts a dump from MIFARE Classic Tool to mfoc/mfcuk .mfc
format (raw data)

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
from base64 import b16decode

# What an unreadable block looks like from MCT
UNREADABLE_BLOCK = '--------------------------------'
SECTOR_HEADER = '+Sector: '

# What to replace unreadable blocks with
DUMMY_REGULAR_BLOCK = '00000000000000000000000000000000'
DUMMY_TRAILER_BLOCK = 'FFFFFFFFFFFFFF078069FFFFFFFFFFFF'

def mct_to_mfc(input_f, output_f):
  # File format:
  # +Sector: 0
  # repeated base16 encoded block data
  # +Sector: 1
  # ...
  sector = -1
  block = 0
  for line in input_f:
    line = line.strip()
    if line.startswith(SECTOR_HEADER):
      new_sector = int(line[len(SECTOR_HEADER):])
      if new_sector != sector + 1:
        raise Exception('Sectors missing from dump (expected %d, got %d)' % (sector + 1, new_sector))
      sector = new_sector
      block = 0
      continue

    if block > 15 or (sector < 31 and block > 3):
      raise Exception("Excess blocks (%d) in sector %d" % (block, sector))

    if line == UNREADABLE_BLOCK:
      if block < 3 or (sector >= 31 and block < 15):
        line = DUMMY_REGULAR_BLOCK
      else:
        line = DUMMY_TRAILER_BLOCK

    line = b16decode(line)
    output_f.write(line)
    block += 1

  output_f.flush()
  output_f.close()
  

def main():
  parser = ArgumentParser()
  parser.add_argument('input_mct', nargs=1, type=FileType('r'),
    help='MIFARE Classic Tool dump file to read')

  parser.add_argument('-o', '--output', type=FileType('wb'),
    help='Output mfc dump')

  options = parser.parse_args()
  mct_to_mfc(options.input_mct[0], options.output)


if __name__ == '__main__':
  main()
