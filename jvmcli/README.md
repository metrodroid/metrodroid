# Metrodroid JVM (CLI)

This contains build rules for a command-line version of Metrodroid's card parser that runs on a
regular JRE (Java Runtime Environment).

**This is interface is highly experimental, and subject to change!**

## Features

* Identifies and parses Metrodroid JSON, XML and ZIP files
* Lists supported cards

**Note:** This does not yet have a GUI, support NFC hardware or localization.

## Requirements

* [OpenJDK 9][openjdk9] ([OpenJDK 11 recommended][openjdk11])
* Android SDK (for building only; will be removed in future)
* `git`

Like the rest of Metrodroid, you [must `git clone --recursive` (with submodules)][main-readme], or
**the build will fail**.

**Note:** [The Android SDK tools use APIs that are deprecated and disabled in OpenJDK 9, and deleted
in OpenJDK 11][android-java-11].

## Building and running tests

The build process uses [Shadow][] [application targets][shadow-app].

From the root of Metrodroid's source directory, run:

```sh
./gradlew :jvmcli:installShadowDist jvmCliTest
```

This will "install" to `./jvmcli/build/install/jvmcli-shadow/`.

## Using and running

You can start Metrodroid with `./jvmcli/build/install/jvmcli-shadow/bin/metrodroid-cli`.  You can
create a shell alias or symlink to it, and that'll still work.

<dl>

<dt><code>metrodroid-cli --help</code></dt>
<dd>Shows the current in-built help file</dd>

<dt><code>metrodroid-cli identify file.json</code></dt>
<dd>Identifies the card(s) in <code>file.json</code>.</dd>

<dt><code>metrodroid-cli parse file.json</code></dt>
<dd>Parses all card(s) information from <code>file.json</code>.</dd>

<dt><code>metrodroid-cli unrecognized file.json</code></dt>
<dd>Lists all unsupported or unrecognized cards in <code>file.json</code>.</dd>

<dt><code>metrodroid-cli supported</code></dt>
<dd>Lists all supported cards in this build of Metrodroid.</dd>

</dl>

There are card dump JSON and XML files in `./src/commonTest/assets/`, which can be used to try it
out:

```
$ metrodroid-cli identify src/commonTest/assets/opal/opal-transit-litter.xml
card UID = <04512492b23a80>
   name = Opal
   serial = 3085 2200 7856 2242

$ ./metrodroid-cli parse src/commonTest/assets/opal/opal-transit-litter.xml
card UID = <04512492b23a80>
   name = Opal
   serial = 3085 2200 7856 2242
   balance = -$1.82
   info
      General: null
      Weekly trips: 2
      Last transaction: null
      Transaction counter: 21
      Date: 27 July 2015
      Time: 6:24:00 pm
      Vehicle type: Rail
      Transaction type: Journey completed (distance fare)
   raw
      Checksum: 12644
```

[android-java-11]: https://issuetracker.google.com/issues/67495440
[main-readme]: ../README.md
[openjdk9]: https://jdk.java.net/9/
[openjdk11]: https://jdk.java.net/11/
[shadow]: https://imperceptiblethoughts.com/shadow/
[shadow-app]: https://imperceptiblethoughts.com/shadow/application-plugin/
