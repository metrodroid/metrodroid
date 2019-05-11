# Recover pattern for generating Tpouch'n'go keys from keys for
# a single card
#
# Usage: python3 tng2json.py <proxmark key file> <UID>
import sys
import json

keystream = bytearray(open(sys.argv[1], 'rb').read())
tagId = bytes.fromhex(sys.argv[2])

pattern = [
        tagId[1] ^ tagId[2] ^ tagId[3],
        tagId[1],
        tagId[2],
        ((tagId[0] + tagId[1] + tagId[2] + tagId[3]) % 0x100) ^ tagId[3],
        0,
        0
]

reskeys = []
for sector in range(40):
        key = bytearray(keystream[sector * 6 + j] ^ pattern[j] for j in range(6)).hex()
        reskeys += [{
                'key': key,
                'type': 'KeyA',
                'sector': sector,
                'transform': 'touchngo'
        }]

print (json.dumps({
        "KeyType": "MifareClassicStatic",
        "Description": "Touchngo key pattern",
        "keys": reskeys
        }))
