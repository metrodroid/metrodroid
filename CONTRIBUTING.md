# Contributing and bug reports

**Please note:** I work on Metrodroid as a hobby, primarily when I travel (for other reasons), and I'm not being paid for this.  As a result, it is infeasible for me to travel haphazardly in order to add new cities to the list.

In order to make sure this project is "fun" for as long as possible, I want to deal with as little nonsense as possible, and will strictly guard against it. All "+1" and other useless comments will be deleted, and you may also be blocked from this project indefinitely. Use [GitHub reaction emojis](https://github.com/blog/2119-add-reactions-to-pull-requests-issues-and-comments) instead.

This document attempts to answer the common cases of issues filed here, what information is useful, and what things are known or I don't care about.

## "Support my transit network"

Please see [supporting new cards](https://github.com/micolous/metrodroid/wiki/New-cards) on the wiki before filing.  This is a very difficult, time consuming process, involving travel to the city in question.

**If you are not interested in writing code or understanding the format yourself,** please find someone else who lives in your city who is interested, instead of reporting the issue here.

**If you have useful docs to share,** feel free to add them in an issue, though the issue may be later closed if it appears nobody is actively working on it.

**If you are interested in working on this,** please also:

- Attempt to understand the card format on your own first.
  - Sometimes it may be similar to an existing format that is already supported.  This makes it significantly easier to read!
  - There are some tools in the `extras` folder of this repository that help decode unknown formats.
  - `vbindiff` is your friend.
- Share notes about what you have found out.
- Share annotated card dumps, from multiple cards, at multiple points in time:
  - This includes information like balance, travel history (when and where you used the card)
  - Each of the dumps should have 1 change -- eg: before tapping on at the start station, after tapping on at the start station, after tapping off at the destination station.

If you have notes and annotated card dumps which reliably explain the data format, but don't understand how to turn that into code, I'm happy to help write the code to make the last part happen.  For an example of this, please see [the Nextfare](https://github.com/micolous/metrodroid/wiki/Cubic-Nextfare-MFC) and [Opal](https://github.com/micolous/metrodroid/wiki/Opal) documentation in the wiki.

@micolous is working on Australian public transit cards first, as these are easiest for him to travel for.

Some networks are unsupportable, there'll be [details in the Wiki](https://github.com/micolous/metrodroid/wiki/) if it is known bad.

## "I want to send you cards!"

I don't have the facility to accept cards right now, sorry.

If I did, I might not be able to send them back.

## "I found a bug!"

Make sure you're testing on the current version of Metrodroid please check the [open issues first](https://github.com/micolous/farebot/issues) to see if it has already been reported.

Please note that some phones will not support MIFARE Classic cards.  This is because MIFARE Classic is not standard NFC.  You need a phone with a NXP chipset in order to read them.  The Nexus 4, 5, 6, 7 (all versions) and 10 cannot read them.  The Galaxy Nexus, Nexus 5X, 6P, Pixel (2016) can read them.

Whether your phone supports MIFARE Classic is shown in the About screen.

Make sure to include all the information in the about screen when reporting, and [verify card reads with NXP TagInfo](https://play.google.com/store/apps/details?id=com.nxp.taginfolite).

## Known buggy phones

Some phones have generally buggy NFC stacks, and I can't work around the bug.  Please contact the manufacturer if you have these issues:

* The [OnePlus 5 has broken MIFARE Classic support](https://github.com/micolous/metrodroid/issues/31) where they can't read cards that have a key of `00 00 00 00 00 00`.  This looks like a bug in the OnePlus NFC stack.

* The [LG G3 and G Stylo have totally broken MIFARE Classic support](https://github.com/micolous/metrodroid/issues/26) and can't authenticate with the card properly.  This looks like a bug in several LG phones' NFC stack.

## "I have a patch!"

You're my new best friend.  Make sure you clearly describe what the issue is and how it fixes it.

If your patch is incomplete, that's fine, just don't expect me to fix it if it requires access to a particular network's cards.

## "It says my card is encrypted / fully locked!"

See: [Cracking keys](https://github.com/micolous/metrodroid/wiki/Cracking-keys).


