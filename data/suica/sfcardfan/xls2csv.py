#!/usr/bin/env python3
# -*- mode: python; indent-tabs-mode: nil; tab-width: 2; coding: utf-8 -*-
"""
data/suica/sfcardfan/xls2csv.py - Dumps SFCardFan database into CSV format.

Data source: http://www.denno.net/SFCardFan/

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
from xlrd import open_workbook

XLS_CHARSET = 'cp932'
STATION = '駅' # eki
INSIDE_STATION = '駅構内'
ZERO = ('0', '0.0',)

# Sheets that contain Suica mapping information
SHEETS = {
  # Suica railway stations
  'suica_rail': 'Suica系コード',
  # Suica point-of-sale terminals
  'suica_pos': 'Suica物販端末コード',
  # ICa bus stops
  'ica_bus': 'ICa駅・バス停コード',
  # IruCa stop codes
  'iruca_bus': 'IruCa停留所コード',
  # CI-CA bus stop codes
  'cica_bus': 'CI-CA バス停留所コード',
  # Nice Pass station + bus stop codes
  'nice_pass': 'nice・pass駅・バス停留所コード',
}

# Canonical column name -> Japanese name(s).
# These must match column names in the XLS.
COLUMN_TRANSLATION = {
  'src': set(),
  'area_code': ('地区コード', 'エリアコード'),
  'line_code': ('線区コード', '路線コード'),
  'station_code': ('駅順コード', '駅・バス停コード', '停留所コード', 'バス停留所コード', '駅・バス停留所コード'),
  'terminal_type': ('端末種別',),
  'terminal_code_high': ('端末コード上位',),
  'terminal_code_low': ('端末コード下位',),
  'company_name': ('会社名',),
  'line_name': ('線区名', '路線名'),
  'station_name': ('駅名', '駅・バス停名', '停留所名', 'バス停留所名', '駅・バス停留所名'),
  'comment': ('備考',),
  'latitude': ('緯度',),
  'longitude': ('経度',),
  'store_name': ('店舗名',),
  'cash_register': ('レジ',),
}

# Japanese column name -> canonical name
# This is so we don't repeat ourselves in COLUMN_TRANSLATION
COLUMN_MAPPING = dict((v, k) for k,vs in COLUMN_TRANSLATION.items() for v in vs)

def extractor(input_xls_fn, output_csv_fn):
  output_csv_fh = open(output_csv_fn, mode='w', encoding='utf-8')
  output_csv = DictWriter(output_csv_fh, COLUMN_TRANSLATION.keys())
  output_csv.writeheader()
  input_xls = open_workbook(input_xls_fn, encoding_override=XLS_CHARSET)
  print('Sheet names:', input_xls.sheet_names())
  for canonical_name, jp_name in SHEETS.items():
    sheet = input_xls.sheet_by_name(jp_name)
    print('Sheet(%s) = %s' % (canonical_name, sheet.name))
    row1 = sheet.row(1)

    # Build a mapping of column offset to canonical name
    mapping = {}
    hexify_cols = set()
    loc_cols = set()
    for i, c in enumerate(row1):
      mapping[i] = COLUMN_MAPPING.get(c.value)
      if mapping[i] is None:
        print('Warning: Column(%d) "%s" is missing from COLUMN_TRANSLATION.' % (i, c.value,))
      elif mapping[i].endswith('_code'):
        hexify_cols.add(i)
      elif mapping[i] in ('latitude', 'longitude'):
        loc_cols.add(i)
    
    for i, row in enumerate(sheet.get_rows()):
      if i < 2:
        # Skip header rows
        continue
      
      d = {'src': canonical_name}
      for j, cell in enumerate(row):
        if j in hexify_cols:
          v = '0x%X' % (int(cell.value, base=16))
        elif j in loc_cols:
          v = str(cell.value)
          if v in ZERO:
            continue
        else:
          v = str(cell.value)
        d[mapping[j]] = v

      #print(d)
      output_csv.writerow(d)
      
  

def main():
  parser = ArgumentParser()
  parser.add_argument('input_xls', nargs=1)
  parser.add_argument('-o', '--output')
  options = parser.parse_args()
  extractor(options.input_xls[0], options.output)

if __name__ == '__main__':
  main()
