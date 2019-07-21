@file:JvmName("StationTableReaderActualKt")
package au.id.micolous.metrodroid.util

import android.content.res.AssetManager
import au.id.micolous.metrodroid.MetrodroidApplication
import java.io.InputStream

actual fun openMdstFile(dbName: String): InputStream? {
    val context = MetrodroidApplication.instance
    return context.assets.open("$dbName.mdst", AssetManager.ACCESS_RANDOM)
}
