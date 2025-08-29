package uk.trigpointing.android.logging

import android.app.Dialog
import android.content.Context
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import uk.trigpointing.android.DbHelper
import uk.trigpointing.android.R
import uk.trigpointing.android.types.PhotoSubject
import java.io.File

/**
 * Dialog for editing photo metadata
 */
class PhotoMetadataDialog : DialogFragment() {
    
    private lateinit var imageView: ImageView
    private lateinit var subjectSpinner: Spinner
    private lateinit var captionEditText: EditText
    private lateinit var publicCheckBox: CheckBox
    
    private var photoId: Long = 0
    private var photoPath: String? = null
    private var thumbPath: String? = null
    private var trigId: Long = 0
    private var appContext: Context? = null
    
    private var onSaveCallback: ((PhotoManager.PhotoMetadata) -> Unit)? = null
    private var onDeleteCallback: (() -> Unit)? = null
    
    companion object {
        private const val ARG_PHOTO_ID = "photo_id"
        
        fun newInstance(photoId: Long): PhotoMetadataDialog {
            return PhotoMetadataDialog().apply {
                arguments = Bundle().apply {
                    putLong(ARG_PHOTO_ID, photoId)
                }
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        photoId = arguments?.getLong(ARG_PHOTO_ID) ?: 0
        // Cache application context to survive transient detach/attach during dialog lifecycle
        appContext = context?.applicationContext
    }
    
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val inflater = LayoutInflater.from(requireContext())
        val view = inflater.inflate(R.layout.dialog_photo_metadata, null)
        
        setupViews(view)
        loadPhotoData()
        
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle("Photo Details")
            .setView(view)
            .setPositiveButton("Save") { _, _ ->
                saveMetadata()
            }
            .setNegativeButton("Cancel", null)
            .setNeutralButton("Delete") { _, _ ->
                confirmDelete()
            }
            .create()
    }
    
    private fun setupViews(view: View) {
        imageView = view.findViewById(R.id.photo_preview)
        subjectSpinner = view.findViewById(R.id.subject_spinner)
        captionEditText = view.findViewById(R.id.caption_edit)
        publicCheckBox = view.findViewById(R.id.public_checkbox)
        
        // Setup subject spinner
        val subjects = PhotoSubject.values().filter { it != PhotoSubject.NOSUBJECT }
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            subjects.map { getSubjectDisplayName(it) }
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        subjectSpinner.adapter = adapter
        
        // Set default selection to TRIGPOINT
        val defaultIndex = subjects.indexOf(PhotoSubject.TRIGPOINT)
        if (defaultIndex >= 0) {
            subjectSpinner.setSelection(defaultIndex)
        }
    }
    
    private fun getSubjectDisplayName(subject: PhotoSubject): String {
        return when (subject) {
            PhotoSubject.TRIGPOINT -> "Trigpoint"
            PhotoSubject.FLUSHBRACKET -> "Flush Bracket"
            PhotoSubject.LANDSCAPE -> "Landscape"
            PhotoSubject.PEOPLE -> "People"
            PhotoSubject.NOSUBJECT -> "Other"
            else -> subject.toString()
        }
    }
    
    private fun loadPhotoData() {
        val ctx = appContext ?: context ?: return
        val db = DbHelper(ctx)
        try {
            db.open()
            val cursor = db.fetchPhoto(photoId)
            if (cursor != null && cursor.moveToFirst()) {
                trigId = cursor.getLong(cursor.getColumnIndexOrThrow(DbHelper.PHOTO_TRIG))
                thumbPath = cursor.getString(cursor.getColumnIndexOrThrow(DbHelper.PHOTO_ICON))
                photoPath = cursor.getString(cursor.getColumnIndexOrThrow(DbHelper.PHOTO_PHOTO))
                val caption = cursor.getString(cursor.getColumnIndexOrThrow(DbHelper.PHOTO_NAME))
                val subjectCode = cursor.getString(cursor.getColumnIndexOrThrow(DbHelper.PHOTO_SUBJECT))
                val isPublic = cursor.getInt(cursor.getColumnIndexOrThrow(DbHelper.PHOTO_ISPUBLIC)) > 0
                cursor.close()
                
                // Load thumbnail
                thumbPath?.let { path ->
                    if (File(path).exists()) {
                        val bitmap = BitmapFactory.decodeFile(path)
                        imageView.setImageBitmap(bitmap)
                    }
                }
                
                // Set caption
                captionEditText.setText(caption)
                
                // Set subject
                val subject = PhotoSubject.fromCode(subjectCode)
                val subjects = PhotoSubject.values().filter { it != PhotoSubject.NOSUBJECT }
                val subjectIndex = subjects.indexOf(subject)
                if (subjectIndex >= 0) {
                    subjectSpinner.setSelection(subjectIndex)
                }
                
                // Set public checkbox
                publicCheckBox.isChecked = isPublic
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            db.close()
        }
    }
    
    private fun saveMetadata() {
        val ctx = appContext ?: context ?: return
        val subjects = PhotoSubject.values().filter { it != PhotoSubject.NOSUBJECT }
        val selectedSubject = subjects[subjectSpinner.selectedItemPosition]
        
        val metadata = PhotoManager.PhotoMetadata(
            subject = selectedSubject,
            caption = captionEditText.text.toString(),
            isPublic = publicCheckBox.isChecked
        )
        
        // Update database
        val db = DbHelper(ctx)
        try {
            db.open()
            db.updatePhoto(
                photoId,
                trigId,
                metadata.caption,
                "", // description not used
                thumbPath ?: "",
                photoPath ?: "",
                metadata.subject,
                if (metadata.isPublic) 1 else 0
            )
        } finally {
            db.close()
        }
        
        onSaveCallback?.invoke(metadata)
    }
    
    private fun confirmDelete() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Photo")
            .setMessage("Are you sure you want to delete this photo? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                deletePhoto()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun deletePhoto() {
        val ctx = appContext ?: context ?: return
        val db = DbHelper(ctx)
        try {
            db.open()
            
            // Delete files
            thumbPath?.let { File(it).delete() }
            photoPath?.let { File(it).delete() }
            
            // Delete from database
            db.deletePhoto(photoId)
        } finally {
            db.close()
        }
        
        onDeleteCallback?.invoke()
        // Close the dialog after deletion to prevent interacting with stale UI
        dismissAllowingStateLoss()
    }
    
    fun setCallbacks(
        onSave: (PhotoManager.PhotoMetadata) -> Unit,
        onDelete: () -> Unit
    ) {
        onSaveCallback = onSave
        onDeleteCallback = onDelete
    }
}
