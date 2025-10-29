package au.id.micolous.metrodroid.multi

actual interface Parcelable

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.PROPERTY)
actual annotation class IgnoredOnParcel actual constructor()

actual annotation class VisibleForTesting actual constructor()
