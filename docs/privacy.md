---
title: "Metrodroid Privacy and Permissions Statement"
permalink: /privacy
---

This statement only applies to
[Metrodroid distributed via the Google Play Store][gps].

## Use of your personal information

Metrodroid allows you to read many types of transit and payment cards.

Metrodroid itself does not centrally collect or share any information about how
you use the software, or the contents of cards you scan.

In normal operation, Metrodroid will store a timestamped snapshot of the card's
content on your device:

* if you scan the same card multiple times, Metrodroid will create a new
  snapshot each time

* you can delete card snapshots from your device at any time

* you can export card snapshots from your device to a regular file, in a variety
  of file formats

* you can import card snapshots from files back into Metrodroid, even if the
  snapshots were created on another device

Metrodroid does not communicate with the card's issuer or third parties without
your consent, and the card readers operate entirely offline unless otherwise
noted below ([Leap](#leap)).

When [Metrodroid is installed from Google Play][gps], Google collects other
technical and statistical data about Metrodroid's operation from your device,
and share it in aggregate with Metrodroid's developers. Metrodroid's developers
have *no control* over this. More details can be found in
[Google Play's Data Access for Developers policy][da].

[da]: https://support.google.com/googleplay/android-developer/answer/9959470

### Leap

**Background:** Leap cards are fully locked MIFARE DESFire cards. No data is
readable without using [challenge-response authentication][crauth].

Crafting a response for Leap card's challenge-response authentication requires
three things:

1. a single-use challenge (pseudo-random data) from your Leap card
2. your Leap card's unique hardware identifiers
3. a secret key known only to the card issuer -
   [Transport for Ireland (TfI)][tfi]

TfI operates a web service which can craft these responses, but requires
transmitting both the challenge itself and your Leap card's unique hardware
identifiers. This information is encrypted in transit, and handled according
to [TfI's privacy statement][tfi-privacy].

Due to the potentially-sensitive nature of this data, Metrodroid will only
communicate with TfI's web service with your consent.

Metrodroid does not "phone home" during this process – it will only communicate
with TfI.

**By default:** Metrodroid will not attempt to unlock these cards, and *they
will be unreadable*.

**You can provide (or revoke) consent at any time in Metrodroid's preferences**:
under `Advanced Options`, select `Near Field Communication`, then tick (or
un-tick) `Retrieve keys for Leap cards`.

**If you provide consent,** and only when reading a Leap card, Metrodroid will:

1. Automatically send the authentication challenge and your Leap
   card's unique hardware identifiers to a web service operated by TfI, and
   fetch the challenge response.

2. Use the challenge response to temporarily unlock the files on the card.

3. Take a snapshot of the unlocked files, as normal.

**If you revoke consent,** you will only be able to read snapshots of Leap cards
collected while consenting.

[tfi]: https://www.transportforireland.ie/
[tfi-privacy]: https://www.transportforireland.ie/privacy/

## Permissions

Metrodroid requires some permissions to function correctly:

* **Internet access:**

  1. Used by the map viewer to show the start and end location(s) of a trip, if available.

     _By default,_ this uses tiles from [StackPtr][], but Metrodroid can be configured to use an
     arbitrary map tile server of your choice.

  2. _With your consent_, it is used for [challenge-response authentication for Leap cards](#leap).

     You can grant or revoke this at any time in Metrodroid's settings, and it is _disabled by
     default_.

* **NFC:** Required to read NFC cards.

* **Photos / Media / Files:** Used to load and save scanned cards from/to a user-specified file.
  Only needed on Android 4.3 and _earlier_.  _Later_ versions of Android use the [Storage Access
  Framework][saf].

If in doubt, on Android 6.0 and later, you can selectively revoke permissions.

[Return to the Metrodroid home page.]({{ '/' | relative_url }})

[crauth]: https://en.wikipedia.org/wiki/Challenge%E2%80%93response_authentication
[gps]: https://play.google.com/store/apps/details?id=au.id.micolous.farebot
[stackptr]: https://stackptr.com
[saf]: https://developer.android.com/guide/topics/providers/document-provider
