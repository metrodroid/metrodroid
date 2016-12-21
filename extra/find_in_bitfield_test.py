# -*- mode: python; indent-tabs-mode: nil; tab-width: 2 -*-
"""
find_in_bitfield_test.py
Unit tests for find_in_bitfield.py

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

from find_in_bitfield import *
import unittest

class KeyFilterTest(unittest.TestCase):
  def testTrues(self):
    for x in range(16):
      self.assertTrue(keyfilter(x*64*8))
      self.assertTrue(keyfilter(((x*64) + 0x10) * 8))
      self.assertTrue(keyfilter(((x*64) + 0x20) * 8))
    
    for x in range(8):
      self.assertTrue(keyfilter((2048 + (x*256)) * 8))
      self.assertTrue(keyfilter((2048 + (x*256) + 0xb0) * 8))

  def testFalses(self):
    for x in range(16):
      self.assertFalse(keyfilter(((x*64) + 0x30) * 8))

    for x in range(8):
      self.assertTrue(keyfilter((2048 + (x*256) + 0xc0) * 8))

