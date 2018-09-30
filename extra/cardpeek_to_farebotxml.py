#!/usr/bin/env python
# -*- mode: python; indent-tabs-mode: nil; tab-width: 2 -*-
"""
cardpeek_to_farebotxml.py - Converts a cardpeek xml
to Farebot XML export format.

Copyright 2015-2016 Michael Farrell <micolous+git@gmail.com>
Copyright 2018 Google

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
from os.path import basename, getmtime
from xml.etree import ElementTree as etree
from lxml import objectify
from lxml.etree import XMLParser as XP
import base64
import codecs
import re
import random

def parse_node(path, node, fls):
    classname = None
    attid = None
    val = None
    for att in node.findall('attr'):
        if att.get('name') == 'classname':
            classname = att.text
        if att.get('name') == 'id':
            attid = att.text
        if att.get('name') == 'val':
            val = att.text
    if classname == 'card':
        for subnode in node.findall('node'):
            parse_node(path, subnode, fls)
    if classname == 'file' or classname == 'folder':
        newpath = path + (int(attid, 16),)
        for subnode in node.findall('node'):
            parse_node(newpath, subnode, fls)
    if classname == 'record':
        if path not in fls:
            fls[path] = {}
        fls[path][attid] = bytes.fromhex(val[2:].strip())
    

def cardpeek_to_farebot(input_fs, output_f):
    cards = []

    random.seed()

    for input_fb in input_fs:
        tagid = codecs.encode('fake-' + str(random.randrange(0, 100000000)), 'ascii')
        # Cardpeek has a malformed XML header
        parser = XP(recover=True)
        input_xml = objectify.parse(input_fb, parser)
        root = input_xml.getroot()
        fls = {}
        for node in root.findall('node'):
            parse_node((), node, fls)

        # Lets make some XML.
        card = etree.Element('card',
                             type='5',
                             id=tagid.hex(),
                             scanned_at=str(int(getmtime(input_fb.name) * 1000)),
                             label=basename(input_fb.name),
                             partial_read='false',
                             )
        applications = etree.SubElement(card, 'applications')
        calypso_application = etree.SubElement(applications, 'application', type='calypso')
        etree.SubElement(calypso_application, 'application-name').text = 'MVRJQy5JQ0E='
        etree.SubElement(calypso_application, 'application-data').text = 'AAAA'
        etree.SubElement(calypso_application, 'tagid').text = codecs.decode(base64.b64encode(tagid), 'ascii')
        records = etree.SubElement(calypso_application, 'records')
        for (fname,frecs) in fls.items():
            fl = etree.SubElement(records, 'file', name=':' + ':'.join("%x" % x for x in fname))
            rcs = etree.SubElement(fl, 'records')
            for (rn, rv) in frecs.items():
                rc = etree.SubElement(rcs, 'record',index=str(rn))
                rc.text = codecs.decode((base64.b64encode(rv, None)), 'ascii')
            path = etree.SubElement(etree.SubElement(fl, 'selector'), 'path')
            for l in fname:
                etree.SubElement(etree.SubElement(path, 'element', kind='id'), 'id').text = str(l)

        cards.append(card)
        input_fb.close()

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


def main():
    parser = ArgumentParser()
    parser.add_argument('input_mfc', nargs='+', type=FileType('rb'),
                        help='Card Mobib dumps to read')

    parser.add_argument('-o', '--output', type=FileType('wb'),
                        help='Output Farebot XML to write')

    options = parser.parse_args()
    cardpeek_to_farebot(options.input_mfc, options.output)


if __name__ == '__main__':
    main()
