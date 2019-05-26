package au.id.micolous.metrodroid.multi

actual class FormattedString actual constructor(private val input: String) {
    actual val unformatted: String
        get() = input

    actual companion object {
        actual fun monospace(input: String) = FormattedString (input)
    }
}
