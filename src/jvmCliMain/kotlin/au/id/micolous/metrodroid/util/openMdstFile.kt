package au.id.micolous.metrodroid.util

import au.id.micolous.metrodroid.multi.Log
import java.io.File
import java.io.ByteArrayInputStream
import java.io.InputStream

actual fun openMdstFile(dbName: String): InputStream? {
    Log.d("openMdstFile", "Opening $dbName")
    val b = Preferences::class.java.getResourceAsStream("/$dbName.mdst")?.readBytes() ?: return null
    return ByteArrayInputStream(b)
}
