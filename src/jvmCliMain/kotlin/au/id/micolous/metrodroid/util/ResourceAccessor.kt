package au.id.micolous.metrodroid.util

import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.util.*
import java.util.zip.ZipFile

actual object ResourceAccessor {
    private fun openFile(path: String): InputStream? =
        ResourceAccessor::class.java.getResourceAsStream("/$path")?.readBytes()
            ?.let { ByteArrayInputStream(it) }

    actual fun openMdstFile(dbName: String): InputStream? = openFile("$dbName.mdst")
    private fun listJarFile(jarFile: String, filt: (String) -> Boolean): List<String> =
        ZipFile(jarFile).entries()?.let { Collections.list(it) }?.map { it.name }?.filter(filt).orEmpty()

    private fun listResources(filt: (String) -> Boolean): List<String> {
        val loader = ResourceAccessor::class.java.classLoader ?: return emptyList()
        val resList = loader.getResources("")?.let { Collections.list(it) } ?: return emptyList()
        val ret = mutableListOf<String>()
        val srcJar = ResourceAccessor::class.java.protectionDomain?.codeSource?.location?.path
        if (srcJar?.endsWith(".jar") == true)
            ret += listJarFile(srcJar, filt)
        for (res in resList) {
            if (filt(res.path))
                ret += res.path
            if (res.path.endsWith("/") && res.protocol == "file") {
                val f = File(res.toURI() ?: continue)
                ret += f.list { _, name -> filt(name) }?.toList().orEmpty()
            }

            if (res.path.endsWith("/") && res.protocol == "jar") {
                val jarFile = res.path.removePrefix("file:").substringBeforeLast("!")
                ret += listJarFile(jarFile, filt)
            }
        }
        return ret.map {
            it.substringAfterLast("/")
        }
    }

    actual fun stationTableReaderList(): List<String> =
        listResources { it.endsWith(".mdst") }.map {
            it.substringBefore(".mdst")
        }
}