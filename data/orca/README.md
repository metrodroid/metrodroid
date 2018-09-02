# ORCA station database

It is expected that this database is very small, as most bus services record the
Coach ID or Machine ID.

MdST Reader IDs shown here are a packed, big endian integer:

* the upper 4 bits is an agency ID
* the lower 16 bits is a stop ID (coach ID)

If you have a stop ID from somewhere else, convert it to the MdST Reader ID
format with:

```python
reader_id = (agency_id << 16) | stop_id
```

If using LibreOffice, and assuming the agency ID is in B2, and the stop ID is in
C2:

```
=BITOR(BITLSHIFT(B2, 16), C2)
```

To decompose an MdST Reader ID into component parts:

```python
agency_id = mdst_id >> 16
stop_id = mdst_id & 0xffff
```

Or this formula (for agency ID and stop ID respectively):

```
=BITRSHIFT(A2, 16)
=BITAND(A2, 65535)
```


