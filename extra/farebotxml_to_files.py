#!/usr/bin/env python
# -*- mode: python; indent-tabs-mode: nil; tab-width: 2 -*-
"""
farebotxml_to_files.py - converts Farebot XML files to binary files for easier
analysis

Copyright 2015-2017 Michael Farrell <micolous+git@gmail.com>

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
from lxml import objectify
from os.path import join
from zipfile import ZipFile
import base64
import codecs


def getMobibName(file_id, record_id):
    full = '/'.join([file_id, record_id])
    mapping = {
        ':2/1': 'ICC',
        ':3f1c/1': 'Holder1',
        ':3f1c/2': 'Holder2',
        ':2000:2001/1': 'EnvHol1',
        ':2000:2001/2': 'EnvHol2',
        ':2000:2010/1': 'EvLog1',
        ':2000:2010/2': 'EvLog2',
        ':2000:2010/3': 'EvLog3',
        ':2000:2050/1': 'ConList',
        ':2000:2020/1': 'Contra1',
        ':2000:2020/2': 'Contra2',
        ':2000:2020/3': 'Contra3',
        ':2000:2020/4': 'Contra4',
        ':2000:2020/5': 'Contra5',
        ':2000:2020/6': 'Contra6',
        ':2000:2020/7': 'Contra7',
        ':2000:2020/8': 'Contra8',
        ':2000:2020/9': 'Contra9',
        ':2000:2020/10': 'Contra10',
        ':2000:2020/11': 'Contra11',
        ':2000:2020/12': 'Contra12',
        ':2000:2069/1': 'Counter',
        ':1000:1014/1': 'LoadLog',
        ':1000:1015/1': 'Purcha1',
        ':1000:1015/2': 'Purcha2',
        ':1000:1015/3': 'Purcha3',
    }
    return mapping.get(full, full)


def zipify(input_xml, output_zipf, mfcdump, mobib):
  output_zip = None
  if mobib:
      output_zipf = codecs.getwriter('ascii')(output_zipf)
  elif not mfcdump:
    output_zip = ZipFile(output_zipf, 'w')
  xml = objectify.parse(input_xml)
  root = xml.getroot()
  if root.tag == 'cards':
    cards = root.iterchildren()
  elif root.tag == 'card':
    cards = [root]
  else:
    print ('unexpected root node: %r' % (root.tag,))
    return

  if (mfcdump or mobib) and len(cards) != 1:
    print ('expected 1 card dump in mfcdump and mobib modes, there were %d' % (len(cards),))
    return

  used_card_dirs = set()

  # iterate through cards
  for card in cards:
    assert card.tag == 'card'
    scanned_at = card.get('scanned_at')
    card_id = card.get('id')
    card_type = card.get('type')

    if mfcdump and card_type != '0':
      print ('Only MIFARE Classic cards can be dumped this way')
      return

    if mobib and card_type != '5':
      print ('Only Calypso cards can be dumped this way')
      return

    card_dir = 'scan_%s_%s' % (scanned_at, card_id)
    if card_dir in used_card_dirs:
        counter = 1
        while ('%s_%s' % (card_dir, counter)) in used_card_dirs:
            counter += 1
        card_dir = '%s_%s' % (card_dir, counter)
    used_card_dirs.add(card_dir)

    sectors = card.find('sectors')
    if sectors is not None:
      # MIFARE classic card
      # Iterate sectors
      sectors_i = sorted(sectors.findall('sector'), key=lambda e: int(e.get('index')))
      for sector in sectors_i:
        sector_id = sector.get('index')
        if sector.get('unauthorized') == 'true' or sector.get('invalid') == 'true':
          if mfcdump:
            print ('locked sector found, cannot recreate dump')
            return

          # Locked sector, skip and leave marker
          marker = '.invalid'
          if sector.get('unauthorized') == 'true':
            marker = '.unauthorized'
          output_zip.writestr(join(card_dir, sector_id, marker), '')
          continue

        blocks = sorted(sector.find('blocks').findall('block'), key=lambda e: int(e.get('index')))
        for block in blocks:
          # Lets pull some blocks!
          assert block.get('type') == 'data'
          if mfcdump:
            output_zipf.write(base64.b64decode(block.find('data').text))
          else:
            output_zip.writestr(join(card_dir, sector_id, block.get('index')),
                                base64.b64decode(block.find('data').text))
      continue

    applications = card.find('applications')
    if applications is not None:
      # MIFARE DESfire or like
      for application in applications.findall('application'):
        application_id = application.get('id') or application.get('type')
        files = application.find('files')
        for f in files.findall('file') if files is not None else []:
          file_id = f.get('id')
          error = f.find('error')
          if error is not None:
            continue
          
          output_zip.writestr(join(card_dir, application_id, file_id),
                              base64.b64decode(f.find('data').text))
        histories = application.find('histories')
        for f in histories.findall('history') if histories is not None else []:
            if f.text is not None:
                output_zip.writestr(join(card_dir, application_id, "histories", f.get('idx')), base64.b64decode(f.text))
        purses = application.find('purses')
        for f in purses.findall('purse') if purses is not None else []:
            if f.text is not None:
                output_zip.writestr(join(card_dir, application_id, "purses", f.get('idx')), base64.b64decode(f.text))
        records = application.find('records')
        for f in records.findall('file') if records is not None else []:
            file_id = f.get('name')
            error = f.find('error')
            if error is not None:
              continue

            datanode = f.find('data')
            if datanode is not None:
                output_zip.writestr(join(card_dir, application_id, file_id, "data"), base64.b64decode(datanode.text))

            fcinode = f.find('fci')
            if fcinode is not None:
                output_zip.writestr(join(card_dir, application_id, file_id, "fci"), base64.b64decode(fcinode.text))

            for rec in f.find('records').findall('record'):
              record_id = rec.get('index')
              if mobib:
                hexs = base64.b64decode(rec.text).hex()
                hexsp = ' '.join(a+b for a,b in zip(hexs[::2], hexs[1::2]))
                output_zipf.write(getMobibName(file_id, record_id) + ": "
                                  + hexsp.upper() + "\n")
              else:
                output_zip.writestr(join(card_dir, application_id, file_id, record_id), base64.b64decode(rec.text))

    systems = card.find('systems')
    if systems is not None:
      # FeliCa
      for system in systems.findall('system'):
        system_id = system.get('code')
        for service in system.find('services').findall('service'):
          service_id = service.get('code')
          last_addr = -1
          data = b''
          for block in service.find('blocks').findall('block'):
            addr = int(block.get('address'))
            assert last_addr < addr, 'blocks not in order'
            last_addr = addr

            data += base64.b64decode(block.text)

          output_zip.writestr(join(card_dir, system_id, service_id), data)
      continue

    pages = card.find('pages')
    if pages is not None:
      # MIFARE Ultralight
      for page in pages.findall('page'):
        page_id = page.get('index')
        data = base64.b64decode(page.find('data').text)
        output_zip.writestr(join(card_dir, page_id), data)
      
      continue

  if output_zip:
    output_zip.close()
  output_zipf.close()

def main():
  parser = ArgumentParser()
  parser.add_argument('input_xml', nargs=1, type=FileType('rb'),
    help='Farebot XML file to read')

  parser.add_argument('-o', '--output', type=FileType('wb'),
    help='Output ZIP file to write')

  parser.add_argument('-m', '--mfcdump', action='store_true',
    help='Instead of writing a ZIP file, store an MFC/MFD raw binary backup of the card. Only for single MIFARE Classic exports.')

  parser.add_argument('--mobib', action='store_true',
                      help='Instead of writing a ZIP file, store a MOBIB-extractor raw text dump of the card. Only for single Calypso exports.')

  options = parser.parse_args()
  zipify(options.input_xml[0], options.output, options.mfcdump, options.mobib)


if __name__ == '__main__':
  main()

