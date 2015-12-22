#!/usr/bin/env python
"""
mfcdump_to_farebotxml.py - Converts a mfoc/mfcuk .mfc dump file for Mifare
Classic to Farebot XML export format.

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
from base64 import b16encode, b64encode
from os.path import getmtime
from xml.etree import ElementTree as etree


def mfc_to_farebot(input_f, output_f):
	# Read the Mifare card entirely first
	card_data = input_f.read()

	# Card data should be 1K or 4K
	assert len(card_data) in (1024, 4096)

	# Lets make some XML.
	root = etree.Element('card', type='0', id=b16encode(card_data[0:4]).lower(), scanned_at=str(int(getmtime(input_f.name)*1000)))
	sectors = etree.SubElement(root, 'sectors')
	
	for sector_no in range(len(card_data) / 64):
		sector = etree.SubElement(sectors, 'sector', index=str(sector_no))
		blocks = etree.SubElement(sector, 'blocks')
		for block_no in range(4):
			block = etree.SubElement(blocks, 'block', index=str(block_no), type='data')
			data = etree.SubElement(block, 'data')
			offset = (sector_no * 64) + (block_no * 16)
			data.text = b64encode(card_data[offset:offset+16])

	# We have made a structure, dump it to disk
	tree = etree.ElementTree(root)
	tree.write(output_f, encoding="utf-8", xml_declaration=True)
	output_f.flush()
	output_f.close()
	input_f.close()
	

def main():
	parser = ArgumentParser()
	parser.add_argument('input_mfc', nargs=1, type=FileType('rb'),
		help='Card MFC dump to read')

	parser.add_argument('-o', '--output', type=FileType('wb'),
		help='Output Farebot XML to write')

	options = parser.parse_args()
	mfc_to_farebot(options.input_mfc[0], options.output)


if __name__ == '__main__':
	main()

