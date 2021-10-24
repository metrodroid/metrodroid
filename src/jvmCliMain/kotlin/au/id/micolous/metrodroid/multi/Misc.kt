@file:JvmName("MiscKtActual")

package au.id.micolous.metrodroid.multi

actual interface Parcelable
actual annotation class Parcelize actual constructor()
actual annotation class VisibleForTesting actual constructor()
actual fun Any?.nativeFreeze(): Any? = this