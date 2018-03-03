---
title: Metrodroid: Myki
permalink: /myki
---

Myki is the public transit ticket used in Victoria, Australia. Metrodroid
implements support only for reading the card number from the card.

Unfortunately, this card does not appear to have any freely readable data on the
card which contains anything useful, like the balance or recent trips.

The card technology is Mifare DESFire, which is currently resistant to a number
of attacks which would allow the protected sectors to become readable. Further
developments may come, but there's no saying how long this will take.

There are other applications available on the Google Play Store which claim to
read your Myki.  They don't actually read the data from the card at all, apart
from the card number.  They then use screen-scraping techniques to access the
Myki website in order to get this data.

We don't think this is a reasonable way to "read" the card, because it involves
talking to Public Transport Victoria's (PTV) servers to be able to get extra
information.  Every other reader implemented in Metrodroid works totally
offline, without talking any servers.

(We're happy to be proven wrong on all of this -- please provide evidence in the
Metrodroid issue tracker.

As a result, reading data from a Myki in the same way that other agencies allow
is pretty much a dead end.  Unless significant changes are made by PTV/Keane to
the card format, Metrodroid and other applications like it will not be able to
implement offline reading functionality.

_This doesn't make Myki more or less secure that other transit cards._

Some other cards keys can be cracked, but this is only possible because they
use older, less secure smartcard technology. However, they have other security
features in their system design which prevent fraud.

It is possible to publicly release specifications of your transit smartcard
format and allow read access, and not have the system become less secure. For
example, HSL in Finland [publicly document their smartcard
format](http://dev.hsl.fi/#travel-card), and Opal in Sydney allow limited read
access to the card.

Without this, access to your Myki data makes registration and personalisation of
your card _effectively mandatory._ As a comparison, Transport for NSW does not
require registration or internet access for Opal card data.

There is an opportunity for PTV to embrace open data in a way that is secure,
and to respect the privacy of its citizens.

If you are a Myki user and want this sort of access to your Myki card, you
should contact [Public Transport
Victoria](http://ptv.vic.gov.au/customer-service/feedback-and-complaints/) or
the [Victorian Minister for Public
Transport](http://www.vic.gov.au/contactsandservices/directory/?ea0_lfz99_120.&roleWithSubordinates&d4594914-a7cc-4cfe-83c5-ca64af7fa031).

[Return to the Metrodroid home page.](https://micolous.github.io/metrodroid/)

