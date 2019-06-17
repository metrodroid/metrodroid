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

[kmp]: https://kotlinlang.org/docs/reference/multiplatform.html