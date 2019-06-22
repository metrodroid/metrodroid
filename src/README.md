# metrodroid: src/

This is the root of the Metrodroid code base.

Historically, it's just an Android project, but it is now _partially_ a [Kotlin multi-platform][kmp]
project.  There's still some work to be done, and we're partially blocked on the state of the Kotlin
multi-platform environment.

Module          | Description
--------------- | --------------------
`androidTest`   | Contains Android instrumented unit tests. These will eventually be migrated to `commonTest`.
`commonMain`    | Contains most of the business logic for Metrodroid.  All of the base NFC code is in there, as well as all the transit card reader implementation, serialisation code, and utility functions. Some platform abstractions are declared here (`expect class`).
`commonTest`    | Contains unit tests that should be run on all platforms, as well as test data.
`dev`           | Contains an alternate configuration of the Android application, so that you can run a second package ID with the development version.
`jvmCommonMain` | Contains implementations of platform abstractions that are used on both Android and the JVM (`actual class`)
`main`          | Contains Android-specific application and platform code. This is also where the MdST implementation lives.

<table>

<tr>
    <td colspan="11">
        <strong>androidTest, dev</strong>
    </td>
</tr>

<tr>
    <td colspan="11">
        <h3>main</h3>
        <ul>
            <li><strong>Android application</strong></li>
            <li>Android CardTransceiver</li>
            <li>Android Logging</li>
            <li>Android Localisation</li>
            <li>Card and key storage</li>
            <li>Country and currency information</li>
            <li>Leap Unlocker</li>
            <li>Preferences</li>
            <li>MdST implementation</li>
            <li>User interface</li>
            <li>XML parser</li>
        </ul>
    </td>
</tr>

<tr>
    <td colspan="4">
        <h3>jvmCommonMain</h3>
        <ul>
            <li>Timestamp and Timezone bindings for Java</li>
            <li>ZIP bindings for Java</li>
            <li>Collator binding for Java</li>
        </ul>
    </td>
    <td>Android platform libraries</td>
    <td>AndroidX</td>
    <td>KotlinX</td>
    <td>Leaflet</td>
    <td>nv-i18n</td>
    <td>Protobuf</td>
    <td>XML pull parser</td>
</tr>

<tr>
    <td colspan="3">
        <h3>commonMain</h3>
    </td>
    <td>Java platform</td>
</tr>

<tr>
    <td>Kotlin platform</td>
    <td>KotlinX</td>
    <td>PB and K</td>
</tr>

</table>

[kmp]: https://kotlinlang.org/docs/reference/multiplatform.html