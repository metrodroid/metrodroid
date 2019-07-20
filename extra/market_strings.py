#!/usr/bin/env python3
# -*- mode: python; indent-tabs-mode: nil; tab-width: 4 -*-
"""
market_strings.py
Gets the current strings for the Android Market / Google Play Store in a
given language.

Copyright 2018-2019 Michael Farrell <micolous+git@gmail.com>

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
import argparse
import re
import os
import os.path
from typing import Iterator, Optional, Text, Tuple
from xml.etree import ElementTree

METRODROID_ROOT = os.path.join(os.path.dirname(__file__), '..')
RESOURCES_DIR = os.path.join(METRODROID_ROOT, 'src/main/res')
DEFAULT_LANG = 'values'
OTHER_LANG = 'values-'
RESC_FILE = 'market.xml'
DEFAULT_MARKET = os.path.join(RESOURCES_DIR, DEFAULT_LANG, RESC_FILE)
CHANGELOG = 'market_changelog_'
CHANGELOG_GENERIC = CHANGELOG + 'generic'
DOUBLE_NEWLINE = re.compile(r'^(?:\\n)*(.*)(?:\\n)*$', re.U | re.M)


def get_languages() -> Iterator[Tuple[Text, Text]]:
    """Gets a list of available Market language resources.

    Generates:
        (language code, full path)
    """
    for entry in os.scandir(RESOURCES_DIR):
        if not entry.is_dir():
            continue

        path = os.path.join(entry.path, RESC_FILE)
        if not os.path.exists(path):
            continue

        if entry.name == DEFAULT_LANG:
            yield ('default', path)

        if entry.name.startswith(OTHER_LANG):
            lang_code = entry.name[len(OTHER_LANG):]
            yield (lang_code, path)


def list_languages() -> None:
    """Prints a list of available languages."""
    print('Available languages:')
    for lang_code, path in get_languages():
        print(f'* {lang_code}: {path}')


def android_resource_unescape(value: Text) -> Text:
    """Unescapes an Android resource string."""
    value = '\n'.join(m.group(1) for m in DOUBLE_NEWLINE.finditer(value))

    return value.replace('\r', '')\
        .replace('\\n', '\n')\
        .replace('\\', '')


def show_language(lang: Optional[Text] = None) -> None:
    """Shows Market strings for a given language.

    Args:
        lang: language code to display strings for, or None for the default
              language.
    Raises:
        ValueError: on invalid input
        IOError: when the market resource file cannot be loaded
    """
    market_path = DEFAULT_MARKET

    if lang:
        if (os.sep in lang) or (os.altsep is not None and os.altsep in lang):
            raise ValueError('Path characters not allowed in language codes')

        market_path = os.path.join(RESOURCES_DIR, OTHER_LANG + lang, RESC_FILE)

    if not os.path.exists(market_path):
        raise IOError(f'No market resource found ({market_path})')

    if lang:
        print(f'Translations for {lang}')
    else:
        print('Default market information')

    tree = ElementTree.parse(market_path)
    title = None
    short_desc = None
    long_desc = None

    for tag in tree.iterfind('string'):
        name = tag.get('name')
        if name == 'market_title':
            title = tag.text
        elif name == 'market_short_desc':
            short_desc = tag.text
        elif name == 'market_long_desc':
            long_desc = tag.text

        if title and short_desc and long_desc:
            break

    print()
    print('Title:')
    print(title)

    print()
    print('Short description:')
    print(short_desc)

    print()
    print('Full description:')
    print(android_resource_unescape(long_desc))


def get_changelog_for_version(
        path: Text,
        version: Optional[int] = None) -> Tuple[Optional[int], Optional[Text]]:
    """Gets the changelog for a given version code.

    Args:
        path: path to market.xml file
        version: the version to fetch, or None for the latest

    Returns:
        tuple of (version_code, notes).
        If a generic changelog was returned, version_code = None.
        If no notes are available, notes = None.
    """

    version_tag = None if version is None else (CHANGELOG + str(version))

    notes = None
    generic = None
    for tag in ElementTree.parse(path).iterfind('string'):
        name = tag.get('name')
        if name == CHANGELOG_GENERIC:
            generic = tag.text
            continue

        if version_tag:
            # We have a specific target
            if name == version_tag:
                return version, android_resource_unescape(tag.text)
        else:
            if name.startswith(CHANGELOG):
                this_version = int(name[len(CHANGELOG):])
                if version is None or this_version > version:
                    notes = tag.text
                    version = this_version

    if notes:
        # Found notes, and this was the latest version.
        return version, android_resource_unescape(notes)

    if generic:
        # Didn't find notes for the specific version, but a generic
        # alternative is available.
        return None, android_resource_unescape(generic)

    # Nothing found at all
    return None, None


def show_changelog(version: Optional[int] = None) -> None:
    """Shows the changelog for each language.

    The default changelog will be shown if the latest version does not have
    an available changelog.

    Args:
        version: the version to display, or None for the latest.

    """
    # Figure out what the changelog is in English -- this gives the latest
    # version.

    version, english_notes = get_changelog_for_version(DEFAULT_MARKET, version)

    if version is None or english_notes is None:
        raise KeyError(
            'Could not find specific English notes for that version')

    print(f'## Release notes for version {version}')

    print('<en>')
    print(english_notes)
    print('</en>')
    print()

    # Now we know the latest English changelog, search for it in other files.
    for lang_code, path in get_languages():
        if lang_code == 'default':
            continue

        found_version, notes = get_changelog_for_version(path, version)

        if notes:
            if not found_version:
                print(f'## Generic changelog for {lang_code}:')
            print(f'<{lang_code}>')
            print(notes)
            print(f'</{lang_code}>')
        else:
            print(f'## No changelog for {lang_code}')

        print()


def main() -> None:
    """Main method."""
    parser = argparse.ArgumentParser()
    parser.add_argument(
        'language',
        nargs='?',
        help='Language to show Market resources for. If not specified, shows '
             'a list of options. If "default", shows the default resources '
             '(English). If "notes", show the latest release notes for all '
             'translated languages.')

    parser.add_argument(
        '-v',
        '--version',
        type=int,
        help='If used with "notes", will show the changelog for a specific '
             'version, instead of the latest.')

    options = parser.parse_args()

    if options.language:
        if options.language == 'notes':
            show_changelog(options.version)
        elif options.language == 'default':
            show_language()
        else:
            show_language(options.language)
    else:
        list_languages()


if __name__ == '__main__':
    main()
