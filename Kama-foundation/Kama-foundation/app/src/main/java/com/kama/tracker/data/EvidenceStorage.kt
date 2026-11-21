package com.kama.tracker.data

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Photos live in app-private storage: filesDir/evidence/<habitId>/<day>_<timestamp>.jpg */
class EvidenceStorage(private val context: Context) {

    private val root: File get() = File(context.filesDir, "evidence").also { it.mkdirs() }

    fun resolve(relativePath: String): File = File(root, relativePath)

    /** Temp file + content Uri to hand to the camera app (TakePicture contract). */
    fun newCameraTarget(): Pair<File, Uri> {
        val dir = File(context.cacheDir, "camera").also { it.mkdirs() }
        val file = File.createTempFile("capture_", ".jpg", dir)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        return file to uri
    }

    /** Copies the picked/captured image into private storage and returns its relative path. */
    suspend fun import(source: Uri, habitId: Long, day: Long): String = withContext(Dispatchers.IO) {
        val relative = "$habitId/${day}_${System.currentTimeMillis()}.jpg"
        val target = resolve(relative)
        target.parentFile?.mkdirs()
        val input = context.contentResolver.openInputStream(source)
            ?: throw IllegalStateException("Could not open the selected image.")
        input.use { ins -> target.outputStream().use { out -> ins.copyTo(out) } }
        if (target.length() == 0L) {
            target.delete()
            throw IllegalStateException("The selected image was empty.")
        }
        relative
    }

    suspend fun delete(relativePath: String) = withContext(Dispatchers.IO) {
        resolve(relativePath).delete()
        Unit
    }

    suspend fun deleteAll(relativePaths: List<String>) = withContext(Dispatchers.IO) {
        relativePaths.forEach { resolve(it).delete() }
    }

    fun clearCameraCache() {
        File(context.cacheDir, "camera").listFiles()?.forEach { it.delete() }
    }
}
