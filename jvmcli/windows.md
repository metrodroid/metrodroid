# Metrodroid jvmCli on Windows

## Using a PC/SC smartcard reader

Smartcard readers with WHQL-verified drivers should "just work" in Windows 10 (and probably all the
way back to Windows 7), and automatically download drivers from the internet.

You should get a smart card device appearing in Device Manager, and Windows should automatically
start the `Smart Card` service.

## Character sets

Metrodroid internally uses UTF-8 encoding, but Windows `cmd.exe` uses ASCII. A symptom of this issue
is that some station or line names may appear as squares or question marks.

If you want to render station names with non-ASCII character sets, you'll need to do some tweaking:

1. [Install Language Packs][win-language-pack] appropriate for the cards you want to read.  You only
   need to install the Supplemental Fonts (if available) and Language Pack itself.

   For example, if you want to see Japanese-language station names for Suica/IC cards, you'll need
   to install the Japanese (日本語) pack.

2. Change the active code page in `cmd.exe`: `chcp 65001`

   This changes the active code page to `UTF-8`.

Then, provided you're using `installShadowJar` and the (generated) `metrodroid-cli.bat` launcher
script:

1. Change Java's codepage setting: `set METRODROID_CLI_OPTS="-Dfile.encoding=UTF-8"`

2. Read a card with Metrodroid (`metrodroid-cli parse ...` or `metrodroid-cli smartcard`)

3. Adjust the font in `cmd.exe` to one that has the needed characters.

   For example, `MS Gothic` has appropriate characters for Japanese.

Note: Switching the console language may display `¥` (yen) or `₩` (won) instead of backslash, eg:
`C:¥Users¥me>`. Continue to use the <kbd>\\</kbd> key as you normally would.

[win-language-pack]: https://support.microsoft.com/en-us/help/14236/language-packs

