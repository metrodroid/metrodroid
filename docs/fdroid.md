---
title: "Metrodroid: F-Droid"
permalink: /fdroid
---

## tl;dr:

The [version of Metrodroid on F-Droid][fd] is quite old.

I encourage people to _instead_ either install from the [Google Play Store][gps],
[GitHub releases][apk] or build from source until this issue is resolved.

The newer versions have [better card support and bug fixes][changelog].

I'm still working with F-Droid on getting things up to date, but it's taking quite some time.

## Why is it out of date?

One of the requirements of publishing on F-Droid is that [they are able to build your application
from source][fdsrc].  This allows F-Droid to attest that the source code available allows one to
reproduce your published APK.

As a supporter of free software, I'm quite happy to comply with this requirement.

However, there have been issues with F-Droid's handling of the [Gradle Wrapper][gw], and F-Droid
building with old versions of Gradle that are
[not supported with current versions of the Android SDK][agradle] (new SDK -> new Build Tools -> new
Android Gradle plugin -> new Gradle).

[gps]: https://play.google.com/store/apps/details?id=au.id.micolous.farebot
[fd]: https://f-droid.org/repository/browse/?fdid=au.id.micolous.farebot
[apk]: {{ site.github.repository_url }}/releases/latest
[fdsrc]: https://f-droid.org/en/docs/FAQ_-_App_Developers/#will-my-app-be-built-from-source
[gw]: https://docs.gradle.org/current/userguide/gradle_wrapper.html
[agradle]: https://developer.android.com/studio/releases/gradle-plugin
[changelog]: https://github.com/micolous/metrodroid/wiki/Changelog
