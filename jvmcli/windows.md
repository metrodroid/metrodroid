# Metrodroid jvmCli on Windows

## Using a PC/SC smartcard reader

Smartcard readers with WHQL-verified drivers should "just work" in Windows 10 (and probably all the
way back to Windows 7), and automatically download drivers from the internet.

You should get a smart card device appearing in Device Manager, and Windows should automatically
start the `Smart Card` service.

## Character sets

Metrodroid internally uses the same encoding as Java (UTF-16), but [Java doesn't support Unicode
console output properly on Windows out-of-the-box][jwin-unicode]. A symptom of this issue is that
some station or line names may appear as squares or question marks.

If you want to render station names with non-ASCII character sets:

1. Press <kbd>Start</kbd> and type `Font settings`

   (also available via <kbd>Settings</kbd> -> <kbd>Personalization</kbd> -> <kbd>Fonts</kbd>)

2. Click <kbd>Download fonts for all languages</kbd>.  You'll get a confirmation message saying it
   will take some time and disk space to download: click <kbd>OK</kbd>.

3. Change the active code page to `UTF-8`: `chcp 65001`

4. Change Java's codepage setting for Metrodroid:

   * In `cmd`: `set METRODROID_CLI_OPTS="-Dfile.encoding=UTF-8"`
   * In PowerShell: `$ENV:METRODROID_CLI_OPTS="-Dfile.encoding=UTF-8"`
   * In System control panel: <kbd>Advanced</kbd> tab, <kbd>Environment Variables</kbd>,
     <kbd>System Variables</kbd>, <kbd>New...</kbd>: `METRODROID_CLI_OPTS` = `-Dfile.encoding=UTF-8`

5. Read a card with Metrodroid (`metrodroid-cli parse ...` or `metrodroid-cli smartcard`)

6. Adjust the font to one that has the needed characters. (not required for the
   [new Windows Terminal][new-terminal])

   For example, `MS Gothic` has appropriate characters for Japanese.

Note: Switching the console language may display `¥` (yen) or `₩` (won) instead of backslash, eg:
`C:¥Users¥me>`. Continue to use the <kbd>\\</kbd> key as you normally would.

[jwin-unicode]: https://bugs.openjdk.java.net/browse/JDK-4153167
[new-terminal]: https://www.microsoft.com/en-us/p/windows-terminal-preview/9n0dx20hk701
