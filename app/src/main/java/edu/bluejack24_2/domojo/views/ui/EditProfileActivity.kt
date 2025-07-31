package edu.bluejack24_2.domojo.views.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import edu.bluejack24_2.domojo.R
import edu.bluejack24_2.domojo.databinding.ActivityEditProfileBinding
import edu.bluejack24_2.domojo.models.User
import edu.bluejack24_2.domojo.viewmodels.EditProfileViewModel
import java.io.File
import java.io.FileOutputStream

class EditProfileActivity : BaseActivity() {
    private lateinit var binding: ActivityEditProfileBinding
    private lateinit var viewModel: EditProfileViewModel
    private var selectedImageUri: Uri? = null
    private val PICK_IMAGE_REQUEST = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_edit_profile)
        binding.user = User()
        viewModel = ViewModelProvider(this).get(EditProfileViewModel::class.java)

        binding.viewModel = viewModel
        binding.lifecycleOwner = this

        // Load current user data
        viewModel.loadCurrentUser()

        // Set click listeners
        binding.changeAvatarButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

        binding.saveChangesButton.setOnClickListener {
            val imageFile = selectedImageUri?.let { uri -> getRealFileFromUri(this, uri) }
            viewModel.onEditProfileClicked(this, imageFile)
        }

        binding.backButton.setOnClickListener {
            onBackPressed()
        }

        // Observe LiveData
        viewModel.usernameError.observe(this) { error ->
            binding.usernameErrorTv.apply {
                text = error
                visibility = if (error != null) View.VISIBLE else View.INVISIBLE
            }
        }

        viewModel.passwordError.observe(this) { error ->
            binding.passwordErrorTv.apply {
                text = error
                visibility = if (error != null) View.VISIBLE else View.INVISIBLE
            }
        }

        viewModel.confirmPasswordError.observe(this) { error ->
            binding.confirmPasswordErrorTv.apply {
                text = error
                visibility = if (error != null) View.VISIBLE else View.INVISIBLE
            }
        }

        viewModel.avatarError.observe(this) { error ->
            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.isLoading.observe(this) { isLoading ->
            binding.saveChangesButton.isEnabled = !isLoading
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.navigateToProfile.observe(this) { shouldNavigate ->
            if (shouldNavigate) {
                Handler().postDelayed({
                    val intent = Intent(this, ProfileActivity::class.java)
                    startActivity(intent)
                }, 2500)
            }
        }

        viewModel.currentUser.observe(this) { user ->
            user?.let {
                Glide.with(this)
                    .load(it.avatar)
                    .placeholder(R.drawable.default_avatar)
                    .into(binding.profileImageView)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                selectedImageUri = uri
                Glide.with(this)
                    .load(uri)
                    .into(binding.profileImageView)
            }
        }
    }

    fun getRealFileFromUri(context: Context, uri: Uri): File {
        val inputStream = context.contentResolver.openInputStream(uri)
        val tempFile = File.createTempFile("upload_", ".jpg", context.cacheDir)
        val outputStream = FileOutputStream(tempFile)
        inputStream?.copyTo(outputStream)
        inputStream?.close()
        outputStream.close()
        return tempFile
    }
}