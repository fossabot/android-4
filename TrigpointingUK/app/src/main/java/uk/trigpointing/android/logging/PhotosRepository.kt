package uk.trigpointing.android.logging

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uk.trigpointing.android.DbHelper
import uk.trigpointing.android.common.FileCache
import java.io.File
import java.io.InputStream

class PhotosRepository(private val context: Context) {

    private fun getCacheDir(): File = FileCache(context, "logphotos").cacheDir

    suspend fun addPhotosFromUris(trigId: Long, uris: List<Uri>): List<Long> = withContext(Dispatchers.IO) {
        val db = DbHelper(context)
        val createdIds = mutableListOf<Long>()
        try {
            db.open()
            for (uri in uris) {
                val photoId = db.createPhoto(trigId, "", "", "", "", uk.trigpointing.android.types.PhotoSubject.NOSUBJECT, 0)
                val cacheDir = getCacheDir().absolutePath
                val photoPath = "$cacheDir/${photoId}_I.jpg"
                val thumbPath = "$cacheDir/${photoId}_T.jpg"
                val (thumb, photo) = decodeScaledBitmaps(uri)
                uk.trigpointing.android.common.Utils.saveBitmapToFile(thumbPath, thumb, 50)
                uk.trigpointing.android.common.Utils.saveBitmapToFile(photoPath, photo, 85)
                db.updatePhoto(photoId, trigId, "", "", thumbPath, photoPath, uk.trigpointing.android.types.PhotoSubject.NOSUBJECT, 0)
                createdIds.add(photoId)
            }
        } finally {
            db.close()
        }
        createdIds
    }

    private fun decodeScaledBitmaps(uri: Uri): Pair<Bitmap, Bitmap> {
        val resolver = context.contentResolver
        // Load full-size target first (~640px)
        val full = decodeAndRotate(resolver.openInputStream(uri)!!, 640)
        // Load thumb (~100px) from the decoded full image to preserve orientation
        val thumb = Bitmap.createScaledBitmap(full, 100.coerceAtMost(full.width), (full.height * (100f / full.width)).toInt().coerceAtLeast(1), true)
        return thumb to full
    }

    private fun decodeAndRotate(input: InputStream, requiredSize: Int): Bitmap {
        // Decode with inSampleSize
        val bytes = input.use { it.readBytes() }
        val optsBounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
        android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size, optsBounds)

        var width = optsBounds.outWidth
        var height = optsBounds.outHeight
        var scale = 1
        while (width / 2 >= requiredSize && height / 2 >= requiredSize) {
            width /= 2
            height /= 2
            scale *= 2
        }

        val opts = android.graphics.BitmapFactory.Options().apply { inSampleSize = scale }
        val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
            ?: throw IllegalStateException("Failed to decode bitmap")
        return bitmap
    }
}


