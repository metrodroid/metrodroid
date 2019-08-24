---
title: "Metrodroid: Go card"
permalink: /seqgo
---

**Go Card** is the public transport smartcard used in South East Queensland,
Australia. This includes Brisbane, Gold Coast and Sunshine Coast.

These cards use a Mifare Classic 1K card, and have unique encryption keys. You
need a phone which supports Mifare Classic (see Metrodroid's about screen), and
the encryption keys for your card. This means [you need to crack your card's
keys](https://github.com/micolous/metrodroid/wiki/Cracking-keys):

* _Cards with the serial number printed in black:_ These are MIFARE Classic
  cards. They are vulnerable to the Darkside and Nested attacks, and can be
  cracked without sniffing any reader communication.

* _Cards with the serial number engraved in brown:_ These are MIFARE Plus cards
  in Classic compatibility mode. They are only vulnerable to the Hardnested
  attacks, and require sniffing one key from a legitimate reader.

Metrodroid _only reads data on your card_, and does not require that you
register your Go card or use online services. It implements enough of the card
format to be able to read your balance and recent trips.

**Metrodroid cannot write to your Go card, and does not allow your phone to
emulate a Go card.**

## Known issues

There are a number of caveats with Go card support:

* _Special cards not supported (Go Access, Explore, SEEQ)._

  These special cards are untested and may not work with Metrodroid. This
  includes staff cards.

* _Not all stops are known._

  Metrodroid has a small stop database. Once you have cracked your card's keys,
  you can contribute additional stop IDs from the app.

  Newer versions of Metrodroid have more stops. All fare information can be read
  from v2.9.29 and later (regardless of whether the stop is known).

* _Single trips are sometimes shown as two distinct trips._

  This is normally because of data on the card -- there is a "journey ID", there
  is a continuation flag, and these aren't always stored in a useful way.

  In cases where your fare exceeds the [default fare for that
  mode](https://translink.com.au/tickets-and-fares/fares-and-zones/fixed-fare)
  (eg: train trips of 5 or more zones, or trips <em>to</em> an Airtrain station
  from a non-Airtrain station), you may see a charge listed for the default
  fare, and then a second charge for the difference between your final fare and
  your default fare.

* _Last trip shown with one station and a negative amount._

  Only the last 12 touch events are stored on the card. If a trip is in
  progress, or a default fare was applied to your card, then you might see a
  negative amount with a single station or stop (where you tapped off) in the
  last slot. This will be for the difference between the [default fare for that
  mode](https://translink.com.au/tickets-and-fares/fares-and-zones/fixed-fare)
  and your final fare.

* _Negative balances not shown._

  This is fixed in Metrodroid 2.9.32.

[Return to the Metrodroid home page.]({{ '/' | relative_url }})
