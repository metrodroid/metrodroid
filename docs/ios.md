---
title: "Metrodroid: iOS"
permalink: /ios
---

## Introduction

In iOS 12 and earlier, there was only limited support for reading NDEF NFC tags. Unfortunately, this
isn't enough to read the majority of transit cards.

iOS 13 has [significantly expanded the CoreNFC API][corenfc], such that it is possible to read some
NFC transit cards on iPhone 7 and later.

It is fairly early days for both Metrodroid on iOS and NFC support on iOS in general, so we expect
to see many issues. There are still significant limitations imposed by iOS, but we aim to get as
close to feature parity with the Android version as we can.

* [Requirements](#requirements)
* [Getting Metrodroid for iOS](#getting-metrodroid-for-ios)
* [Known differences and issues](#known-differences-and-issues)
* [Report iOS-specific issues][ios-issue]
* [Build Metrodroid for iOS from source](#build-metrodroid-for-ios-from-source)

## Requirements

* iOS 13.0 beta 7 or later
* iPhone 7 or later

**Note:** other iOS devices, including those supported by Apple Pay, do not support the new CoreNFC
APIs available in iOS 13.

## Getting Metrodroid for iOS

We're currently working on getting Metrodroid available to interested testers via [TestFlight][].
Watch this page for further updates.

If you are a member of the [Apple Developer Program][apple-dev], you can [build Metrodroid for iOS
from source](#build-metrodroid-for-ios-from-source) and deploy it to your device today!

## Known differences and issues

* [Card support, and unsupported cards on iOS](#card-support)
* [UX differences](#ux-differences)
* [UI differences](#ui-differences)
* [Accessibility](#accessibility)
* [Other missing features](#other-missing-features)

### Card support

Metrodroid for iOS **does not support**:

* **Apple Pay**: _like the Android version,_ Metrodroid does not read virtual cards (HCE) from the
  device it is running on.

* **Beijing Municipal Card**: this card uses very short 2 or 4 byte AIDs (depending on revision),
  which are not allowed on iOS.

* **CEPAS (Singapore) cards**: the SS-518 protocol requires implicit AID selection, which is not
  allowed on iOS.

* **EMV cards**: the EMV protocol requires dynamic AID selection, which is not allowed on iOS.

* **FeliCa cards with more than one system not supported**: [this appears to be an iOS
  bug][ios-felica]. This impacts Hu Tong Xing (互通行) cards, as well as some Hayakaken, PASMO and
  Suica cards. ICOCA and nimoca cards both appear fine.

* **Leap**: unlocking Leap cards is not implemented.

* **MIFARE Classic based cards**: iOS 13 does not support the proprietary [Crypto-1][] algorithm
  used by MIFARE Classic, so we cannot read any such transit card. MFC support is omitted, including
  the following components:

  * Key management
  * Fallback reader option (ie: dumps of SmartRider and MyWay can't be imported from old versions of
    Metrodroid for Android)
  * Importing (binary) MFC dump files
  * Importing MIFARE Classic Tool (MCT) files
  * "submit unknown stations" prompt and card warning are removed (only used by Brisbane Go Card
    and Troika)

### UX differences

* **Must press "scan card" on home screen**: iOS only allows scanning from a modal pop-up created
  by CoreNFC.

  By comparison, Android lets foreground applications scan at all times.

* **Toasts are replaced with alert prompts.** Unfortunately, the user must acknowledge every toast
  message.

* **Launch from background is not supported**: not possible to implement in CoreNFC for the cards
  we support -- only NDEF.

* **Preferences screen misses detailed help text**: this doesn't exist on iOS.

### UI differences

* **Metrodroid interface follows iOS look and feel.** This is working as intended. ;)

* **Card image and progress bar is not displayed while reading cards**: we can only display a
  textual message using the CoreNFC API, and a percentage.

* **Apple Maps used instead of Leaflet.**

* **Theming follows system-wide preference only**: The "Farebot" theme is missing as a result (but
  this would look very similar to light theme on iOS anyway).

* **Some collapsed elements are not yet implemented.**  As a result:

  1. In subscription view the details are always shown
  2. In raw view subtrees are not collapsible

### Accessibility

Some accessibility features are not available from iOS version:

* `TtsSpan` that marks date/time and money text as such are omitted and you have
  to rely on your screen reader being smart enough.

* `HiddenSpan` used in time and route indications to provide readable
  alternative to arrows is not implemented yet.
  
### Other missing features

* Locales without real translations are omitted: Irish, Maori, Malay and Traditional Chinese.
  
* Currently the library tests don't run due to a compiler problem.

* Currently there are no iOS-specific tests.

## Build Metrodroid for iOS from source

**Note:** We're working on making this available via [TestFlight][].

Requirements:

* macOS 10.14 Mojave or later
* Android SDK (for technical reasons)
* Xcode 11.0 or later
* Java runtime environment

**Note:** If you want to deploy your build to a physical device [and use NFC support][dev-caps], you
must also enroll in the [Apple Developer Program][apple-dev]. _This has an annual membership fee._

Open `./native/metrodroid` in Xcode, and you should be able to compile and deploy as normal.  This
will invoke Gradle to generate any needed resources.

### Using in the simulator

**Note:** This works even if you are not enrolled in the [Apple Developer Program][apple-dev].

Once you've deployed to the simulator, you can load card dumps by dragging Metrodroid card JSON
files from Finder to the simulator window.

### Deploying to a physical device

**Note:** you must be enrolled in the [Apple Developer Program][apple-dev] to deploy to a physical
device [with NFC support][dev-caps].

You will need to [modify the signing configuration][signing-workflow] in Xcode before you can
deploy:

1. Open the Project Navigator (<kbd>⌘1</kbd>)

2. Select `metrodroid` (the project) at the root of the tree.

3. In the projects and targets list, pick the `metrodroid` target. This has a green Metrodroid logo.

4. Click `Signing & Capabilities`.

   You should see a `Team` of `Unknown Name` appear in red, and the errors `No account for team` and
   `No profiles for 'org.metrodroid.ios' were found`. We'll resolve these issues in the next steps.

5. Change the `Team` to your Apple Developer Program Team's name. This is either your full name, or
   your organisation's name.

   If you don't see a team name in the list, you need to [set up your Apple ID in
   Xcode][xcode-setup] first.

   You should now see the error `Failed to register bundle identifier.`  We'll resolve that next.

6. Set a [unique bundle identifier][bundle-id].  For example, `com.example.metrodroid.ios.dev`.

   If you don't have your own domain name, but have a GitHub account, use something like
   `io.github.your_name_here.metrodroid.ios`.

You should now see `Waiting to repair`, `Creating provisioning profile`, and then the errors should
disappear.

At this point, you can deploy to your device!

**Note:** when submitting pull requests, please ensure that your developer ID and bundle name
changes have been removed from `native/metrodroid/metrodroid.xcodeproj/project.pbxproj`.

[apple-dev]: https://developer.apple.com/programs/enroll/
[bundle-id]: https://help.apple.com/xcode/mac/current/#/deve70ea917b
[corenfc]: https://developer.apple.com/documentation/corenfc
[Crypto-1]: https://en.wikipedia.org/wiki/Crypto-1
[dev-caps]: https://help.apple.com/developer-account/#/dev21218dfd6
[ios-felica]: https://github.com/metrodroid/metrodroid/issues/613
[ios-issue]: https://github.com/metrodroid/metrodroid/issues/new?assignees=&labels=bug&template=bug.md&title=%5BBUG%5D
[signing-workflow]: https://help.apple.com/xcode/mac/current/#/dev60b6fbbc7
[TestFlight]: https://developer.apple.com/testflight/
[xcode-setup]: https://help.apple.com/xcode/mac/current/#/devaf282080a
