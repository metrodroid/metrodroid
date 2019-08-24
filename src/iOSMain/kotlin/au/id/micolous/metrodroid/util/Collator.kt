package au.id.micolous.metrodroid.util

import platform.Foundation.*

object StandardComparator : Comparator<String> {
    override fun compare(a: String, b: String): Int = (a as NSString).localizedStandardCompare(b).toInt()
}

actual object Collator {
    actual val collator: Comparator<in String>
       get() = StandardComparator
}
