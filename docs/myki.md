---
title: "Metrodroid: Myki"
permalink: /myki
---

Myki is the public transit ticket used in Victoria, Australia. Metrodroid
implements support only for reading the card number from the card.

Unfortunately, this card does not appear to have any freely readable data on the
card which contains anything useful, like the balance or recent trips.

This is the result of deliberate design choices by [PTV][ptv]. By comparison,
Sydney's Opal card allows limited read access to data on the card, without
compromising the security of the system as a whole.

Myki cards are MIFARE DESFire, which is currently resistant to a number
of attacks which would allow the protected sectors to become readable. Further
developments may come, but there's no saying how long this will take.

## What about other Myki apps?

There are other applications available on the Google Play Store which claim to
read your Myki.  They don't actually read the data from the card at all, apart
from the card number -- they then [scrape the Myki website][webscrape] to show
your balance and travel history.

We don't think this is a reasonable way to "read" the card, because:

* Myki online services require users to register (personalise) their card,
  handing over personal information to [PTV][ptv] in the process.

* such a reader would require updates in the event [PTV][ptv] changed their
  website, even in ways that would not be visible to a human.

* the data isn't _really_ being read from the card!

By comparison, every other reader implemented in Metrodroid works offline, and
does not require registration of the card.

_Unless significant changes are made by PTV/Keane to the card format,
Metrodroid and other applications like it will not be able to implement offline
reading functionality._

_We're happy to be proven wrong on all of this -- please provide evidence in
the Metrodroid issue tracker._

## Does this mean Myki is more secure than other cards?

_This doesn't make Myki more or less secure that other transit cards._

Some other cards keys can be cracked, but this is only possible because they
use older, less secure smartcard technology. However, they have other security
features in their system design which prevent fraud.

It is possible to allow read access to the card without compromising the
security of the system as a whole. For example, Opal in Sydney allow some (but
useful) read access to the card, and HSL in Finland go a step further and
[publicly document their smartcard format][hsl].

Without this, registration and personalisation of your card is _effectively
mandatory_ to get access to your Myki data.

## What can I do?

If you are a Myki user and want this sort of access to your Myki card, you
should [contact Public Transport Victoria][ptvmail] or the [Victorian Minister
for Public Transport][victrans].

[Return to the Metrodroid home page.]({{ '/' | relative_url }})

[hsl]: http://dev.hsl.fi/#travel-card
[ptv]: https://ptv.vic.gov.au/
[ptvmail]: https://ptv.vic.gov.au/customer-service/feedback-and-complaints/
[victrans]: http://www.vic.gov.au/contactsandservices/directory/?ea0_lfz99_120.&roleWithSubordinates&d4594914-a7cc-4cfe-83c5-ca64af7fa031
[webscrape]: https://en.wikipedia.org/wiki/Web_scraping
