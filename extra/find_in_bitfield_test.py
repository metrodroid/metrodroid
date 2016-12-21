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

