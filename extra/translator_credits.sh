#!/bin/sh
# -*- mode: sh; indent-tabs-mode: nil; tab-width: 2 -*-
#
# translator_credits.sh
#
# This script scans through Metrodroid's resources directory to find who has
# actively used translations in Metrodroid, as well as their email address.
#
# Copyright 2018 Michael Farrell <micolous+git@gmail.com>
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>.
#
THIS_SCRIPT="`realpath $0`"
SCRIPTS="`dirname $THIS_SCRIPT`"
METRODROID_HOME="`dirname $SCRIPTS`"
RESDIR="${METRODROID_HOME}/src/main/res/"

# Get string resource directory listing
RESOURCES_DIRS="`find ${RESDIR} -type f -wholename '*/values-*/strings.xml' -exec dirname \{\} \; | sort`"

# TODO: figure out github usernames
function get_authors_with_email_for_file() {
  git annotate --incremental $1 | \
    egrep 'author(-mail)? ' | \
    sed -z 's/\nauthor-mail//g' | \
    sort | uniq | \
    sed 's/^author //'
}

# HACK: Whenever there are large changes done to translation files, they get
# added to `git blame`. Remove them from the list of translation contributors.
#
# Must manually audit before just taking them off.
function remove_non_translators() {
  grep -v "Michael Farrell"
}

for d in $RESOURCES_DIRS
do
  RES_LANG="`basename $d | sed 's/values-//'`"
  #echo "Credits for language: $RES_LANG"

  LANG_AUTHORS="`mktemp`"

  get_authors_with_email_for_file ${d}/strings.xml >> $LANG_AUTHORS

  if [ -f "${d}/market.xml" ]
  then
    get_authors_with_email_for_file ${d}/market.xml >> $LANG_AUTHORS
  fi

  #echo "result"
  echo -n " * **${RES_LANG}**: "
  sort $LANG_AUTHORS | uniq | remove_non_translators | sed -E 's/^(.+) <([^> ]+)>$/[\1](\2)/g' | sed -z 's/\n/, /g' | sed 's/, $//'
  echo ""
  rm $LANG_AUTHORS
done

