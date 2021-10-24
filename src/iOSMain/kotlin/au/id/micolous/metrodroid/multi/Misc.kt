package au.id.micolous.metrodroid.multi

import kotlin.native.concurrent.freeze

actual interface Parcelable
actual annotation class Parcelize actual constructor()
actual annotation class VisibleForTesting actual constructor()
actual fun Any?.nativeFreeze() = this.freeze()
