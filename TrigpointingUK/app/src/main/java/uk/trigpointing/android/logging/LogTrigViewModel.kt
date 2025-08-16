package uk.trigpointing.android.logging

import android.app.Application
import android.database.Cursor
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import uk.trigpointing.android.DbHelper
import android.net.Uri

class LogTrigViewModel(app: Application) : AndroidViewModel(app) {
    private val db = DbHelper(app)
    private val repo = PhotosRepository(app)
    private val _photos = MutableStateFlow<List<UiPhoto>>(emptyList())
    val photos: StateFlow<List<UiPhoto>> get() = _photos

    fun loadPhotos(trigId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                db.open()
                val list = mutableListOf<UiPhoto>()
                val c: Cursor? = db.fetchPhotos(trigId)
                if (c != null) {
                    do {
                        val id = c.getLong(c.getColumnIndex(uk.trigpointing.android.DbHelper.PHOTO_ID))
                        val icon = c.getString(c.getColumnIndex(uk.trigpointing.android.DbHelper.PHOTO_ICON))
                        list.add(UiPhoto(id = id, thumbnailPath = icon))
                    } while (c.moveToNext())
                    c.close()
                }
                _photos.value = list
            } finally {
                db.close()
            }
        }
    }

    fun addPhotos(trigId: Long, uris: List<Uri>) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.addPhotosFromUris(trigId, uris)
            loadPhotos(trigId)
        }
    }
}


