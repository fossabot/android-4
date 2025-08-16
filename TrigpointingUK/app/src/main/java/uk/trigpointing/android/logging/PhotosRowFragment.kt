package uk.trigpointing.android.logging

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import uk.trigpointing.android.DbHelper
import android.net.Uri
import android.widget.Toast

class PhotosRowFragment : Fragment() {

    private lateinit var viewModel: LogTrigViewModel

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val ctx = requireActivity().application
        viewModel = ViewModelProvider(requireActivity()).get(LogTrigViewModel::class.java)

        val trigId = arguments?.getLong(ARG_TRIG_ID) ?: 0L
        viewModel.loadPhotos(trigId)

        return ComposeView(requireContext()).apply {
            setContent {
                val photos by viewModel.photos.collectAsState(initial = emptyList())
                LogPhotosRow(
                    photos = photos,
                    onAddClick = {
                        // Use system Photo Picker directly here for a modern flow
                        pickPhotos()
                    },
                    onPhotoClick = { id ->
                        val a = activity ?: return@LogPhotosRow
                        val i = Intent(a, LogPhotoActivity::class.java)
                        i.putExtra(DbHelper.PHOTO_ID, id)
                        a.startActivityForResult(i, 2)
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val trigId = arguments?.getLong(ARG_TRIG_ID) ?: return
        viewModel.loadPhotos(trigId)
    }

    private fun pickPhotos() {
        val intent = Intent("android.provider.action.PICK_IMAGES").apply {
            putExtra("android.provider.extra.PICK_IMAGES_MAX", 10)
        }
        if (intent.resolveActivity(requireContext().packageManager) == null) {
            val fallback = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "image/*"
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                addCategory(Intent.CATEGORY_OPENABLE)
            }
            startActivityForResult(fallback, REQ_PICK)
        } else {
            startActivityForResult(intent, REQ_PICK)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_PICK && resultCode == android.app.Activity.RESULT_OK && data != null) {
            val trigId = arguments?.getLong(ARG_TRIG_ID) ?: return
            val uris = mutableListOf<Uri>()
            val cd = data.clipData
            if (cd != null) {
                for (i in 0 until cd.itemCount) {
                    uris.add(cd.getItemAt(i).uri)
                }
            } else {
                data.data?.let { uris.add(it) }
            }
            if (uris.isNotEmpty()) {
                viewModel.addPhotos(trigId, uris)
                (activity as? LogTrigActivity)?.reloadPhotos()
                Toast.makeText(requireContext(), "Added ${uris.size} photo(s)", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        private const val REQ_PICK = 1010
        private const val ARG_TRIG_ID = "trig_id"
        fun newInstance(trigId: Long): PhotosRowFragment {
            val f = PhotosRowFragment()
            val b = Bundle()
            b.putLong(ARG_TRIG_ID, trigId)
            f.arguments = b
            return f
        }
    }
}


