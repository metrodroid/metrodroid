#!/usr/bin/env python3
# -*- mode: python; indent-tabs-mode: nil; tab-width: 2 -*-
"""
multi_crc.py
Attempts checking multiple CRC algorithms on a single file input.

Copyright 2016 Michael Farrell <micolous+git@gmail.com>

Note: This loads the entire file into memory blindly.  Do not use on large file
sizes, as the memory usage of this file is extremely inefficient.

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
from base64 import b16encode
import crcmod.predefined

crc16s = ['16', '16buypass', '16dds110', '16dect', '16dnp', '16en13757', '16genibus', '16maxim', '16mcrf4xx', '16riello', '16t10dif', '16teledisk', '16usb', 'x-25', 'xmodem', 'modbus', 'kermit', 'ccitt-false', 'aug-ccitt']

def multi_crc(input_file, start_offset=0, length=None):
  if isinstance(start_offset, str) and start_offset.startswith('0x'):
    start_offset = int(start_offset, 16)
  else:
    start_offset = int(start_offset)
  input_file.seek(start_offset)
  
  if isinstance(length, str) and length.startswith('0x'):
    length = int(length, 16)

  if length is not None:
    d = input_file.read(int(length))
  else:
    d = input_file.read()
  
  if len(d) < 128:
    print('Calculating checksum on input data:')
    print(b16encode(d))
    print()
  
  for x in crc16s:
    crc_func = crcmod.predefined.mkCrcFun(x)
    s = crc_func(d)
    print('%15s : %04x / %d' % (x, s, s))


def main():
  parser = ArgumentParser()
  parser.add_argument('input_file', type=FileType('rb'), nargs=1,
    help='Input filename')
  
  parser.add_argument('-o', '--start-offset', default=0,
    help='Start position in file [default: %(default)s]')
  
  parser.add_argument('-l', '--length',
    help='Length of data to read in [default: EOF]')

  options = parser.parse_args()

  multi_crc(options.input_file[0], options.start_offset, options.length)

if __name__ == '__main__':
  main()

