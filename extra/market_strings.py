#!/usr/bin/env python3
# -*- mode: python; indent-tabs-mode: nil; tab-width: 2 -*-
"""
market_strings.py
Gets the current strings for the Android Market / Google Play Store in a
given language.

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

from __future__ import print_function
from argparse import ArgumentParser
from lxml import objectify
from os import scandir, sep, altsep
import os.path
import sys

METRODROID_ROOT = os.path.join(os.path.dirname(__file__), '..')
RESOURCES_DIR = os.path.join(METRODROID_ROOT, 'src/main/res')
DEFAULT_LANG = 'values'
OTHER_LANG = 'values-'
RESC_FILE = 'market.xml'
DEFAULT_MARKET = os.path.join(RESOURCES_DIR, DEFAULT_LANG, RESC_FILE)
CHANGELOG = 'market_changelog_'

def get_languages():
  with scandir(RESOURCES_DIR) as it:
    for entry in it:
      if not entry.is_dir():
        continue

      fullpath = os.path.join(entry.path, RESC_FILE)
      if not os.path.exists(fullpath):
        continue

      if entry.name == DEFAULT_LANG:
        yield ('default', fullpath)

      if entry.name.startswith(OTHER_LANG):
        l = entry.name[len(OTHER_LANG):]
        yield (l, fullpath)


def list_languages():
  print('Available languages:')
  for c, p in get_languages():
    print('* %s: %s' % (c, p))

def android_resource_unescape(d):
  d = d.replace('\n', '').replace('\r', '')
  d = d.replace('\\n', '\n').replace('\\\'', '\'')
  return d

def show_language(lang):
  if (sep in lang) or (altsep is not None and altsep in lang):
    print('Path characters are not allowed in language codes.')
    return

  market_path = DEFAULT_MARKET
  if lang != 'default':
    market_path = os.path.join(RESOURCES_DIR, OTHER_LANG + lang, RESC_FILE)

  if not os.path.exists(market_path):
    print('No market resource found (%s)' % market_path)
    return
  
  print('Translations for %s' % lang)
  xml = objectify.parse(market_path)
  title = None
  short_desc = None
  long_desc = None
  
  for t in xml.getroot().iterchildren():
    if t.tag != 'string':
      continue
    if t.get('name') == 'market_title':
      title = str(t)
    if t.get('name') == 'market_short_desc':
      short_desc = str(t)
    if t.get('name') == 'market_long_desc':
      long_desc = str(t)
    
    if title and short_desc and long_desc:
      break
  
  print('')
  print('Title:')
  print(title)
  
  print('')
  print('Short description:')
  print(short_desc)

  print('')
  print('Full description:')
  print(android_resource_unescape(long_desc))


def show_changelog(version=None):
  # Figure out what the changelog is in English -- this gives the latest
  # version.
  xml = objectify.parse(DEFAULT_MARKET)

  v = 0
  d = None

  for t in xml.getroot().iterchildren():
    if t.tag != 'string':
      continue
    if version is not None:
      if t.get('name') == CHANGELOG + str(version):
        d = str(t)
        v = version
        break

    elif t.get('name').startswith(CHANGELOG):
      tv = int(t.get('name')[len(CHANGELOG):])
      if tv > v:
        d = str(t)
        v = tv

  if d is None:
    print('Could not find notes for version %d' % version)
    return

  print('<en>')
  print(android_resource_unescape(d))
  print('</en>')
  print('')

  # Now we know the latest English changelog, search for it in other files.
  v = CHANGELOG + str(v)
  for c, p in get_languages():
    if c == 'default':
      continue
    
    f = False
    for t in objectify.parse(p).getroot().iterchildren():
      if t.tag != 'string':
        continue
      if t.get('name') == v:
        print('<%s>' % c)
        print(android_resource_unescape(str(t)))
        print('</%s>' % c)
        print('')
        f = True
        break
    
    if not f:
      print('## No changelog for %s' % c)
      print('')
      

def main():
  parser = ArgumentParser()
  parser.add_argument('language', nargs='?',
    help='Language to show Market resources for. If not specified, shows a list of options. If "default", shows the default resources (English). If "notes", show the latest release notes for all translated languages.')

  parser.add_argument('-v', '--version', type=int,
    help='If used with "latest", will show the changelog for a specific version, instead of the latest.')

  options = parser.parse_args()

  if options.language:
    if options.language == 'notes':
      show_changelog(options.version)
    else:
      show_language(options.language)
  else:
    list_languages()

if __name__ == '__main__':
  main()

