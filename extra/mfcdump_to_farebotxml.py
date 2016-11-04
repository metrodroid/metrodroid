#!/usr/bin/env python
"""
mfcdump_to_farebotxml.py - Converts a mfoc/mfcuk .mfc dump file for Mifare
Classic to Farebot XML export format.

Copyright 2015-2016 Michael Farrell <micolous+git@gmail.com>

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
from os.path import basename, getmtime
from xml.etree import ElementTree as etree


def mfc_to_farebot(input_fs, output_f):
	cards = []

	for input_f in input_fs:
		# Read the Mifare card entirely first
		card_data = input_f.read()

		# Card data should be 1K or 4K
		assert len(card_data) in (1024, 4096)

		# Lets make some XML.
		card = etree.Element('card',
			type='0',
			id=b16encode(card_data[0:4]).lower(),
			scanned_at=str(int(getmtime(input_f.name)*1000)),
			label=basename(input_f.name)
		)
		sectors = etree.SubElement(card, 'sectors')

		if len(card_data) == 1024:
			sector_count = 16
		elif len(card_data) == 4096:
			sector_count = 40
		
		for sector_no in range(sector_count):
			sector = etree.SubElement(sectors, 'sector', index=str(sector_no))
			blocks = etree.SubElement(sector, 'blocks')
			if sector_no < 32:
				block_count = 4
			else:
				block_count = 16

			for block_no in range(block_count):
				block = etree.SubElement(blocks, 'block', index=str(block_no), type='data')
				data = etree.SubElement(block, 'data')
			
				if sector_no < 32:
					offset = (sector_no * 64) + (block_no * 16)
				else:
					offset = 2048 + ((sector_no - 32) * 256) + (block_no * 16)

				data.text = b64encode(card_data[offset:offset+16])

		cards.append(card)

	if len(cards) > 1:
		root = etree.Element('cards')
		root.extend(cards)
	else:
		root = cards[0]

	# We have made a structure, dump it to disk
	tree = etree.ElementTree(root)
	tree.write(output_f, encoding="utf-8", xml_declaration=True)
	output_f.flush()
	output_f.close()
	input_f.close()
	

def main():
	parser = ArgumentParser()
	parser.add_argument('input_mfc', nargs='+', type=FileType('rb'),
		help='Card MFC dumps to read')

	parser.add_argument('-o', '--output', type=FileType('wb'),
		help='Output Farebot XML to write')

	options = parser.parse_args()
	mfc_to_farebot(options.input_mfc, options.output)


if __name__ == '__main__':
	main()

