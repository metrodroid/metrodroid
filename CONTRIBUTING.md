# Contributing and bug reports

I work on Metrodroid as a hobby, primarily when I travel (for other reasons), and I'm not being
paid for this. As a result, it is infeasible for me to travel haphazardly in order to add new
cities to the list.

I've been working on this for about 5 years now, and I'd like this to remain fun for as long as
possible!

### Fun things :)

* documentation, specifications and useful information
* pull requests containing bug fixes and enhancements
* new research work about a card
* questions that aren't [answered in the documentation][wiki]
* reporting actionable issues

### Not-fun things :(

* [registering an "expression of interest" in seeing a card supported][new-cards]
* [cryptocurrency, and blockchain in general][blockchain]
* defrauding transit agencies (eg: hack my card to get free rides)
* issues that are not about this project
* comments that do not contribute meaningful information (eg: "+1", "please fix")
* using "reviews" to report bugs and/or complain

If you make things _seriously not-fun:_

* your issues and/or comments will be locked and/or deleted
* you may be blocked from this project indefinitely
* issues that impact _you_ will be moved to the _bottom_ of my priority list

## Common issues

* [Supporting new cards][new-cards]
* [Cracking locked cards][cracking]
* ["I want to send you physical cards!"](#i-want-to-send-you-physical-cards)
* ["I found a bug!"](#i-found-a-bug)
* [Known buggy phones](#known-buggy-phones)
* ["I have a patch!"](#i-have-a-patch)
* [Translations][]

### "I want to send you physical cards!"

I don't have the facility to accept cards right now, sorry.

If I did, I might not be able to send them back.

### "I found a bug!"

Make sure you're testing on the current version of Metrodroid please check the [open issues][] to
see if it has already been reported.

**MIFARE Classic** is not supported by all devices, because it is not "standard" NFC. You need a
phone with a NXP chipset in order to read them. The Nexus 4, 5, 6, 7 (all versions) and 10 cannot
read them. The Galaxy Nexus, Nexus 5X, 6P, Pixel 1, 2, 3 and 3A _can_ read them.

Whether your phone supports MIFARE Classic is shown in the About screen. But sometimes manufacturers
screw that up, and so you'll get false negatives and false positives.

**CEPAS (Singapore)** cards use ISO 14443B, which some devices can't read, and some of _those_ cards
are buggy. If your phone doesn't make a sound when trying to scan it, then your device doesn't
support it!

Make sure to include all the information in the about screen when reporting, and
verify card reads with [NXP TagInfo][].

### Known buggy phones

Some phones have generally buggy NFC stacks, and I can't work around the bug. Please contact the
manufacturer if you have these issues:

* [LG G3 and G Stylo have totally broken MIFARE Classic support][lgg3] and can't authenticate
  with the card properly. This looks like a bug in several LG phones' NFC stack.

* [OnePlus 5 has broken MIFARE Classic support][oneplus] where they can't read cards that have a key
  of `00 00 00 00 00 00`. This looks like a bug in the OnePlus NFC stack.

### "I have a patch!"

You're my new best friend.  Make sure you clearly describe what the issue is and how it fixes it.

If your patch is incomplete, that's fine, just don't expect me to fix it if it requires access to a
particular network's cards.

[blockchain]: https://davidgerard.co.uk/blockchain/book/
[cracking]: https://github.com/metrodroid/metrodroid/wiki/Cracking-keys
[lgg3]: https://github.com/metrodroid/metrodroid/issues/26
[new-cards]: https://github.com/metrodroid/metrodroid/wiki/New-cards
[NXP TagInfo]: https://play.google.com/store/apps/details?id=com.nxp.taginfolite
[open issues]: https://github.com/metrodroid/metrodroid/issues
[oneplus]: https://github.com/metrodroid/metrodroid/issues/31
[Translations]: https://github.com/metrodroid/metrodroid/wiki/Translating
[wiki]: https://github.com/metrodroid/metrodroid/wiki
