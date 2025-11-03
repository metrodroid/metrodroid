package au.id.micolous.metrodroid.util

import android.content.res.AssetManager
import au.id.micolous.metrodroid.MetrodroidApplication
import java.io.InputStream

actual object ResourceAccessor {
    actual fun openMdstFile(dbName: String): InputStream? {
        val context = MetrodroidApplication.instance
        return context.assets.open("$dbName.mdst", AssetManager.ACCESS_RANDOM)
    }

    actual fun stationTableReaderList(): List<String> =
        MetrodroidApplication.instance.assets.list("")?.mapNotNull {
            it.substringAfterLast("/").substringBefore(
                ".mdst",
                ""
            ).ifEmpty { null }
        }.orEmpty()
}
