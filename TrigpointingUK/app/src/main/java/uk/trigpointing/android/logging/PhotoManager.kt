package uk.trigpointing.android.logging

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uk.trigpointing.android.DbHelper
import uk.trigpointing.android.R
import uk.trigpointing.android.common.FileCache
import uk.trigpointing.android.common.Utils
import uk.trigpointing.android.types.PhotoSubject
import uk.trigpointing.android.types.TrigPhoto
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

/**
 * Unified photo management system for the TrigpointingUK app.
 * Handles photo selection, metadata, storage, and display.
 */
class PhotoManager(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val TAG = "PhotoManager"
    private val db = DbHelper(context)
    
    companion object {
        const val MAX_PHOTO_SIZE = 1024 // Max dimension for full photo
        const val THUMB_SIZE = 200 // Thumbnail size
        const val JPEG_QUALITY = 85 // JPEG compression quality
        
        // Photo metadata dialog result codes
        const val RESULT_PHOTO_SAVED = 1001
        const val RESULT_PHOTO_DELETED = 1002
    }
    
    /**
     * Data class for photo metadata
     */
    data class PhotoMetadata(
        val subject: PhotoSubject = PhotoSubject.TRIGPOINT,
        val caption: String = "",
        val isPublic: Boolean = true
    )
    
    /**
     * Process selected photos from picker
     */
    fun processSelectedPhotos(
        trigId: Long,
        uris: List<Uri>,
        onPhotoAdded: (Long) -> Unit,
        onComplete: () -> Unit,
        onError: (String) -> Unit
    ) {
        Log.d(TAG, "processSelectedPhotos() called with trigId=$trigId, ${uris.size} URIs")
        scope.launch {
            try {
                Log.d(TAG, "Opening database for photo processing")
                db.open()
                var successCount = 0
                
                Log.d(TAG, "Starting to process ${uris.size} photos")
                for ((index, uri) in uris.withIndex()) {
                    Log.d(TAG, "Processing photo ${index + 1}/${uris.size}: $uri")
                    try {
                        val photoId = processPhoto(trigId, uri)
                        if (photoId != null) {
                            successCount++
                            Log.i(TAG, "Photo processed successfully with ID: $photoId")
                            withContext(Dispatchers.Main) {
                                Log.d(TAG, "Calling onPhotoAdded callback for photo ID: $photoId")
                                onPhotoAdded(photoId)
                            }
                        } else {
                            Log.w(TAG, "processPhoto returned null for URI: $uri")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to process photo at index $index: ${uri}", e)
                    }
                }
                
                Log.d(TAG, "Photo processing complete. Success count: $successCount out of ${uris.size}")
                withContext(Dispatchers.Main) {
                    if (successCount > 0) {
                        Log.i(TAG, "Calling onComplete callback - $successCount photos processed successfully")
                        onComplete()
                    } else {
                        Log.e(TAG, "No photos were processed successfully")
                        onError("Failed to process any photos")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing photos", e)
                withContext(Dispatchers.Main) {
                    Log.e(TAG, "Calling onError callback with message: ${e.message}")
                    onError("Error: ${e.message}")
                }
            } finally {
                Log.d(TAG, "Closing database after photo processing")
                db.close()
            }
        }
    }
    
    /**
     * Process a single photo URI
     */
    private suspend fun processPhoto(trigId: Long, uri: Uri): Long? = withContext(Dispatchers.IO) {
        Log.d(TAG, "processPhoto() called for URI: $uri, trigId: $trigId")
        try {
            // Create database record
            Log.d(TAG, "Creating database record for photo...")
            val photoId = db.createPhoto(
                trigId, 
                "", // name - will be set in metadata dialog
                "", // description - will be set in metadata dialog
                "", // icon path - will be set below
                "", // photo path - will be set below
                PhotoSubject.NOSUBJECT, 
                1 // default to public
            )
            
            Log.i(TAG, "Created photo record with ID: $photoId for trigId: $trigId")
            
            // Process and save images
            val cacheDir = FileCache(context, "logphotos").cacheDir
            Log.d(TAG, "Cache directory: ${cacheDir.absolutePath}")
            
            val photoPath = File(cacheDir, "${photoId}_I.jpg").absolutePath
            val thumbPath = File(cacheDir, "${photoId}_T.jpg").absolutePath
            Log.d(TAG, "Photo will be saved to: $photoPath")
            Log.d(TAG, "Thumbnail will be saved to: $thumbPath")
            
            // Decode and save images
            Log.d(TAG, "Decoding photo bitmap from URI...")
            val photoBitmap = Utils.decodeUri(context, uri, MAX_PHOTO_SIZE)
            Log.d(TAG, "Photo bitmap decoded: ${photoBitmap.width}x${photoBitmap.height}")
            
            Log.d(TAG, "Decoding thumbnail bitmap from URI...")
            val thumbBitmap = Utils.decodeUri(context, uri, THUMB_SIZE)
            Log.d(TAG, "Thumbnail bitmap decoded: ${thumbBitmap.width}x${thumbBitmap.height}")
            
            Log.d(TAG, "Saving photo bitmap to file...")
            Utils.saveBitmapToFile(photoPath, photoBitmap, JPEG_QUALITY)
            Log.d(TAG, "Saving thumbnail bitmap to file...")
            Utils.saveBitmapToFile(thumbPath, thumbBitmap, 50)
            
            Log.i(TAG, "Successfully saved photo files - Photo: $photoPath, Thumb: $thumbPath")
            
            // Update database with file paths
            Log.d(TAG, "Updating database record with file paths...")
            val updateResult = db.updatePhoto(
                photoId,
                trigId,
                "", // name
                "", // description
                thumbPath,
                photoPath,
                PhotoSubject.NOSUBJECT,
                1 // public
            )
            Log.i(TAG, "Database update result: $updateResult for photo ID: $photoId")
            
            Log.i(TAG, "Photo processing completed successfully, returning photoId: $photoId")
            photoId
        } catch (e: Exception) {
            Log.e(TAG, "Failed to process photo from URI: $uri", e)
            Log.e(TAG, "Exception details: ${e.javaClass.simpleName}: ${e.message}")
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Show metadata dialog for a photo
     */
    fun showMetadataDialog(
        activity: FragmentActivity,
        photoId: Long,
        onSave: (PhotoMetadata) -> Unit,
        onDelete: () -> Unit
    ) {
        val dialog = PhotoMetadataDialog.newInstance(photoId)
        dialog.setCallbacks(onSave, onDelete)
        dialog.show(activity.supportFragmentManager, "photo_metadata")
    }
    
    /**
     * Update photo metadata in database
     */
    fun updatePhotoMetadata(
        photoId: Long,
        metadata: PhotoMetadata,
        onComplete: () -> Unit
    ) {
        scope.launch {
            try {
                db.open()
                val cursor = db.fetchPhoto(photoId)
                if (cursor != null && cursor.moveToFirst()) {
                    val trigId = cursor.getLong(cursor.getColumnIndexOrThrow(DbHelper.PHOTO_TRIG))
                    val iconPath = cursor.getString(cursor.getColumnIndexOrThrow(DbHelper.PHOTO_ICON))
                    val photoPath = cursor.getString(cursor.getColumnIndexOrThrow(DbHelper.PHOTO_PHOTO))
                    cursor.close()
                    
                    db.updatePhoto(
                        photoId,
                        trigId,
                        metadata.caption,
                        "", // description not used in new design
                        iconPath,
                        photoPath,
                        metadata.subject,
                        if (metadata.isPublic) 1 else 0
                    )
                }
                
                withContext(Dispatchers.Main) {
                    onComplete()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update photo metadata", e)
            } finally {
                db.close()
            }
        }
    }
    
    /**
     * Delete a photo and its files
     */
    fun deletePhoto(
        photoId: Long,
        onComplete: () -> Unit
    ) {
        scope.launch {
            try {
                db.open()
                
                // Get file paths before deleting from database
                val cursor = db.fetchPhoto(photoId)
                if (cursor != null && cursor.moveToFirst()) {
                    val iconPath = cursor.getString(cursor.getColumnIndexOrThrow(DbHelper.PHOTO_ICON))
                    val photoPath = cursor.getString(cursor.getColumnIndexOrThrow(DbHelper.PHOTO_PHOTO))
                    cursor.close()
                    
                    // Delete files
                    File(iconPath).delete()
                    File(photoPath).delete()
                }
                
                // Delete from database
                db.deletePhoto(photoId)
                
                withContext(Dispatchers.Main) {
                    onComplete()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete photo", e)
            } finally {
                db.close()
            }
        }
    }
    
    /**
     * Get all photos for a trigpoint
     */
    fun getPhotosForTrigpoint(trigId: Long): List<TrigPhoto> {
        val photos = mutableListOf<TrigPhoto>()
        try {
            db.open()
            val cursor = db.fetchPhotos(trigId)
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    val photo = TrigPhoto()
                    photo.setLogID(cursor.getLong(cursor.getColumnIndexOrThrow(DbHelper.PHOTO_ID)))
                    photo.setIconURL(cursor.getString(cursor.getColumnIndexOrThrow(DbHelper.PHOTO_ICON)))
                    photo.setPhotoURL(cursor.getString(cursor.getColumnIndexOrThrow(DbHelper.PHOTO_PHOTO)))
                    photo.setName(cursor.getString(cursor.getColumnIndexOrThrow(DbHelper.PHOTO_NAME)))
                    photo.setDescr(cursor.getString(cursor.getColumnIndexOrThrow(DbHelper.PHOTO_DESCR)))
                    photos.add(photo)
                } while (cursor.moveToNext())
                cursor.close()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get photos for trigpoint", e)
        } finally {
            db.close()
        }
        return photos
    }
}
