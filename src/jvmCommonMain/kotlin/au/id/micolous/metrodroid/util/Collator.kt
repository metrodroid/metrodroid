package au.id.micolous.metrodroid.util

actual object Collator {
    actual val collator: Comparator<in String>
        get () {
            return java.text.Collator.getInstance()
        }
}