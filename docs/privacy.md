---
title: "Metrodroid Privacy and Permissions Statement"
permalink: /privacy
---

Metrodroid requires some permissions to function correctly:

* **Internet access:**

  1. Used by the map viewer to show the start and end location(s) of a trip, if available.

     _By default,_ this uses tiles from [StackPtr][], but Metrodroid can be configured to use an
     arbitrary map tile server of your choice.

  2. _With your consent_, it is used for [challenge-response authentication][crauth] to Leap
     cards, in order to read data from it. This requires sending some information from your Leap
     card to Transport for Ireland (TfI).

     There is no freely readable data available from Leap cards -- it is only accessible after
     authentication.

     A snapshot of the data read from the card is saved on your device, and you can read this
     snapshot without contacting TfI servers again.

     You can grant or revoke this at any time in Metrodroid's settings, and it is _disabled by
     default_.

* **NFC:** Required to read NFC cards.

* **Photos / Media / Files:** Used to load and save scanned cards from/to a user-specified file.
  Only needed on Android 4.3 and _earlier_.  _Later_ versions of Android use the [Storage Access
  Framework][saf].

Metrodroid does not share card data with third parties without consent.

Metrodroid does not communicate with your card's issuing agency without your consent. Metrodroid's
card readers operate entirely offline.

Google Play may also collect other analytics, and forward crash logs to the developers. These crash
logs can contain raw card data.

This statement only applies to [Metrodroid distributed via the Google Play Store][gps].

If in doubt, on Android 6.0 and later, you can selectively revoke permissions.

[Return to the Metrodroid home page.](https://micolous.github.io/metrodroid/)

[crauth]: https://en.wikipedia.org/wiki/Challenge%E2%80%93response_authentication
[gps]: https://play.google.com/store/apps/details?id=au.id.micolous.farebot
[stackptr]: https://stackptr.com
[saf]: https://developer.android.com/guide/topics/providers/document-provider
