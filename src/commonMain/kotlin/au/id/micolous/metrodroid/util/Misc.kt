package au.id.micolous.metrodroid.util

operator fun StringBuilder.plusAssign(other: String) {
    this.append(other)
}