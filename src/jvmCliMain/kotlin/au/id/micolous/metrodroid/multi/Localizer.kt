package au.id.micolous.metrodroid.multi

actual data class StringResource(val id:String, val english: String)
actual data class DrawableResource(val id:String)
actual data class PluralsResource(val id: String, val englishOne: String, val englishMany: String)

actual object Localizer : LocalizerInterface {
    override fun localizeString(res: StringResource, vararg v: Any?): String = res.english.format(*v)
    override fun localizePlural(res: PluralsResource, count: Int, vararg v: Any?) = res.englishMany.format(*v)
}
