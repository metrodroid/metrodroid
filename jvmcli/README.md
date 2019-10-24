# Metrodroid JVM (CLI)

This contains build rules for a command-line version of Metrodroid's card parser that runs on a
regular JRE (Java Runtime Environment).

**This is interface is highly experimental, and subject to change!**

## Features

* Identifies and parses Metrodroid JSON, XML and ZIP files
* Lists supported cards
* Supports reading DESFire, FeliCa and ISO 7816 cards with [PC/SC-compatible][pcsc-compat]
  contactless/contact readers
* Optionally saves card dumps from PC/SC

### Limitations

* No GUI
* Not localised
* **PC/SC support limitations:**

  * Only tested on macOS and Windows 10. It probably works on other systems.
  * Only tested with ACS [ACR122U][] (contactless), [ACR122T][] (contactless) and [ACR38U][]
    (contact) readers. It probably works with devices; however these ACS readers are cheap and
    readily available.
  * Reading using contact readers requires `-U` (`--no-uid`) option.
  * **Does not support** CEPAS, MIFARE Classic, MIFARE Ultralight or Vicinity cards. These _might_
    be supportable in future.
  * Reading from a DESFire card doesn't work the second time around.
  * Reading some EMV cards doesn't work.
  * **libnfc will not work.**  We opted for PC/SC as this is available on more platforms.

## Requirements

* [Java SE (JRE) version 9 or later (version 11 recommended)][adoptopenjdk].

**For smartcard support, you _also_ need:**

* A PC/SC-compatible implementation: macOS and Windows have an in-built PC/SC implementation, Linux
  needs `pcsclite`.
* A PC/SC IFD Handler (driver) for your smartcard reader.

See [PC/SC Workgroup Compatible Products][pcsc-compat] for suggestions.

**Additional _build-time only_ requirements:**

* [OpenJDK 9 (OpenJDK 11 recommended)][adoptopenjdk]
* [Android SDK][android-sdk] (will be removed in future)
* `git`

Like the rest of Metrodroid, you [must `git clone --recursive` (with submodules)][main-readme], or
**the build will fail**.

**Note:** [The Android SDK tools use APIs that are deprecated and disabled in OpenJDK 9, and deleted
in OpenJDK 11][android-java-11]. To work around this, set these environment variables:

```
AVDMANAGER_OPTS="--add-modules java.se.ee"
SDKMANAGER_OPTS="--add-modules java.se.ee"
```

This will be [fixed in a future release of the SDK][android-java-11-fix].

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
<dd>Shows the current in-built help file. You can also pass <code>--help</code> to other
subcommands for more information about those.</dd>

<dt><code>metrodroid-cli identify file.json</code></dt>
<dd>Identifies the card(s) in <code>file.json</code>.</dd>

<dt><code>metrodroid-cli parse file.json</code></dt>
<dd>Parses all card(s) information from <code>file.json</code>.</dd>

<dt><code>metrodroid-cli unrecognized file.json</code></dt>
<dd>Lists all unsupported or unrecognized cards in <code>file.json</code>.</dd>

<dt><code>metrodroid-cli supported</code></dt>
<dd>Lists all supported cards in this build of Metrodroid.</dd>

<dt><code>metrodroid-cli smartcard</code></dt>
<dd>Reads the smartcard attached to a PC/SC connected reader.</dd>

<dt><code>metrodroid-cli smartcard -l</code></dt>
<dd>Lists detected PC/SC connected readers.</dd>

</dl>

### Reading an existing card dump

There are card dump JSON and XML files in `./src/commonTest/assets/`, which can be used to try it
out:

```
$ ./metrodroid-cli identify src/commonTest/assets/opal/opal-transit-litter.xml
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

### Use a connected PC/SC reader

You can get a list of connected devices with `-l`:

```
$ ./metrodroid-cli smartcard -l
Found 3 card terminal(s):
#0: Yubico Yubikey 4 U2F+CCID (card present) (ignored)
#1: ACS ACR122U (card missing)
#2: ACS ACR38U-CCID (card missing)
```

When trying to read, Metrodroid will automatically ignore security keys (like Yubikey), and try to
use the reader that has a card inserted or in range:

```
$ ./metrodroid-cli smartcard
Terminal: ACS ACR122U
Reading MIFARE DESFire card…
Dumping: (0 / 1)
Reading Opal…
Card type: Opal
Dumping: (0 / 1)
Dumping: (1 / 9)
Dumping: (2 / 9)
Dumping: (3 / 9)
Dumping: (4 / 9)
Dumping: (5 / 9)
Dumping: (6 / 9)
Dumping: (7 / 9)
Dumping: (8 / 9)
card UID = <04512492b23a80>
   name = Opal
   serial = 3085 2200 7856 2242
[...]
```

Contact readers generally require the `--no-uid` option.

You can also select a specific reader with `-r`:

```
$ ./metrodroid-cli smartcard -r 'ACS ACR38U-CCID' --no-uid
Terminal: ACS ACR38U-CCID
Probing ISO/IEC 7816 card…
Dumping: (0 / 32)
[...]
Reading Calypso card…
Dumping: (0 / 48)
Dumping: (0 / 79)
[...]
Reading Mobib…
Card type: Mobib
Dumping: (7 / 79)
[...]
card UID = <>
   name = Mobib
   serial = XXXXXX / XXXXXXXXXXXX / X
[...]
```

The `-r` option _also_ allows you to connect to security keys; but Metrodroid doesn't support them
yet.

You can also save this to a file, and instruct Metrodroid to not parse the card data:

```
$ ./metrodroid-cli smartcard --no-parse -o /tmp
Terminal: ACS ACR122U
Reading MIFARE DESFire card…
Dumping: (0 / 1)
Reading Opal…
Card type: Opal
Dumping: (0 / 1)
[..]
Dumping: (8 / 9)
Wrote card data to: /tmp/Metrodroid-048e5ac2db4e80-20190929-070349.json
```

In addition to accepting a directory name, you can also specify an explicit filename to write to.

[adoptopenjdk]: https://adoptopenjdk.net/
[android-sdk]: https://developer.android.com/studio
[android-java-11]: https://issuetracker.google.com/issues/67495440
[android-java-11-fix]: https://issuetracker.google.com/issues/67495440#comment16
[main-readme]: ../README.md
[pcsc-compat]: https://www.pcscworkgroup.com/specifications/compatible-products/
[shadow]: https://imperceptiblethoughts.com/shadow/
[shadow-app]: https://imperceptiblethoughts.com/shadow/application-plugin/

[ACR122U]: https://www.acs.com.hk/en/products/3/acr122u-usb-nfc-reader/
[ACR122T]: https://www.acs.com.hk/en/products/109/acr122t-usb-tokens-nfc-reader/
[ACR38U]: https://www.acs.com.hk/en/driver/160/acr38u-n1-pocketmate-smart-card-reader-usb-type-a/
