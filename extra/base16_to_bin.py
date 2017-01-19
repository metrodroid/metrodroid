#!/usr/bin/env python
# -*- mode: python; indent-tabs-mode: nil; tab-width: 2 -*-
"""
base16_to_binary.py
Converts base16 (hex) dumps into binary form.

Copyright 2017 Michael Farrell <micolous+git@gmail.com>

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
import sys

def base16_to_bin(input_hex, output_bin):
  for l in input_hex:
    output_bin.write(b16decode(l.upper().strip()))
  
  output_bin.close()

def main():
  parser = ArgumentParser()
  parser.add_argument('input_hex', nargs='?', type=FileType('rb'),
    help='Base16 file / hexdump to read [default: stdin]')
  parser.add_argument('-o', '--output', type=FileType('wb'), required=True,
    help='Output binary file to write')

  options = parser.parse_args()
  if options.input_hex:
    infile = options.input_hex[0]
  else:
    infile = sys.stdin
  base16_to_bin(infile, options.output)



if __name__ == '__main__':
  main()
 
