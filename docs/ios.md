---
title: "Metrodroid: iOS"
permalink: /ios
---

## Introduction

On iOS before iOS 13 it was impossible to read NFC cards. However this changed
with extension of CoreNFC framework on iOS. Hence our iOS port is very young
and may have undiscovered issues. We don't strive to feature parity between
iOS and Android version, however we document the differences.

## Card support

iOS supports omits support for several cards for various reasons

* Mifare Classic based cards. iOS 13 doesn't allow to read generic Mifare
  Classic which precludes us from reading MFC-based transit cards. Hence MFC
  support is omitted including following components:
  
  1. Key management.
  2. Fallback reader. Hence old dumps of Smartrider/Myway can't be effectively
     imported into iOS Metrodroid.
  3. Card hiding possibility. It's currently used only by few Zolotaya Korona
     variants and its future is unclear on Android as well.
  4. Import of MFC files.
  5. Import of Mifare Classic Tool (MCT) files
  6. Pledge to submit unknown stations and card warning. Currently used only by
     SEQ Go and Troika cards.
  
* EMV cards. iOS 13 doesn't allow reading dynamic AIDs which precludes EMV
  support, hence EMV support is disabled on iOS version.

* CEPAS cards. CEPAS uses implicit application selections which is unsupported
  by iOS 13 and hence CEPAS support is disabled on iOS version.
  
* Beijing Municipal card. iOS 13 doesn't allow selection of very short AIDs
  which excludes Beijing Municipal card which uses either 2-byte or 4-byte ID
  depending on revision.
  
* Leap unlocking is not implemented yet.

* NFCV is not implemented yet.

## UX differences

* On Android when on main screen you can just tap the card. CoreNFC API doesn't
  allow this and hence you have to click "Scan card" button first.

* Toasts are replaced with alert prompts which is less optimal as requires the
  user to acknowledge every toast message
  
* The feature "Launch from background" that allows scanning the card without
  prior openning the metrodroid app is omitted as it's not supported by CoreNFC
  except for some NDEF cases.
  
* Preference screen misses preference explanations as it doesn't exist on iOS.
  This includes both detailed text explanations on preference screens and
  separate dialog opened for LocaleSpan explanation.

## UI differences

* General interface is revamped to suit iOS look and feel and to use standard
  iOS elements.

* Interface while scanning has to conform to CoreNFC limits and hence is very
  different to Android counterpart. Card image is omitted and the progress bar
  is replaced with text indicator showing percents.

* Map uses Apple maps instead of Leaflet.

* Android has 3 themes and they're selectable in preferences. iOS version has
  only 2 themes: light and dark and switched following system-wide preference.
  Farebot theme is missing but it would probably be very similar to light
  theme on iOS due to general look difference.
  
* Collapsed elements are not implented yet. This has following implications:

  1. In subscription view the details are always shown
  2. In raw view subtrees are not collapsible

## Accessibility

Some acessibility features are omitted from iOS version.

* TtsSpan that marks date/time and money text as such are omitted and you have
to rely on your screen reader being smart enough.

* HiddenSpan used in time and route indications to provide readable alternative
  to arrows is not implemented yet.
  
## Other missing features

* XML importing is not supported

* English regions are omitted:
  
  1. The places in regional places are not shortened. E.g. Australian
     localization shows "Sydney, Australia" rather than "Sydney" as on Android.
  2. "Cheque" is always spelled as "cheque" rather than "check" in some reqions

* Locales without real translations are omitted: Irish, Maori, Malay and
  Traditional Chinese.
  
* Currently the library tests don't run due to a compiler problem.

* Currently there are no iOS-specific tests.

## System requirements

* iOS 13 beta or later
* iPhone 7 or later

## Compilation

You need Android SDK due to technical reasons
You also need Xcode 11 beta or later. Then:

```shell
    ./gradlew generateLocalize iOSLanguages iOSMappedLanguages proto:mainSharedLibrary proto:mainStaticLibrary packForXCode 
```

Then open `native/metrodroid` in Xcode and compile.

