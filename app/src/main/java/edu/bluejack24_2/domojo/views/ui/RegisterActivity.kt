package edu.bluejack24_2.domojo.views.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import edu.bluejack24_2.domojo.R
import edu.bluejack24_2.domojo.databinding.ActivityRegisterBinding
import edu.bluejack24_2.domojo.utils.CloudinaryClient
import edu.bluejack24_2.domojo.viewmodels.RegisterViewModel

class RegisterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterBinding
    private lateinit var viewModel: RegisterViewModel

    private var selectedImageUri: Uri? = null
    private val PICK_IMAGE_REQUEST = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_register)
        viewModel = ViewModelProvider(this).get(RegisterViewModel::class.java)

        binding.viewModel = viewModel
        binding.lifecycleOwner = this

        viewModel.setActivity(this)

        binding.selectFileButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

        binding.signupBtn.setOnClickListener {
            CloudinaryClient.uploadImage(
                context = this,
                uri = selectedImageUri!!,
                onSuccess = { imageUrl ->
                    viewModel.onRegisterClicked(imageUrl)
                },
                onError = { message ->
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                }
            )
        }

        binding.loginLink.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

        viewModel.profilePicError.observe(this, Observer { error ->
            if (error != null) {
                binding.profileErrorTv.text = error
                binding.profileErrorTv.visibility = View.VISIBLE
            } else {
                binding.profileErrorTv.error = null
                binding.profileErrorTv.visibility = View.INVISIBLE
            }
        })

        viewModel.usernameError.observe(this, Observer { error ->
            if (error != null) {
                binding.usernameErrorTv.text = error
                binding.usernameErrorTv.visibility = View.VISIBLE
            } else {
                binding.usernameErrorTv.error = null
                binding.usernameErrorTv.visibility = View.INVISIBLE
            }
        })

        viewModel.emailError.observe(this, Observer { error ->
            if (error != null) {
                binding.emailErrorTv.text = error
                binding.emailErrorTv.visibility = View.VISIBLE
            } else {
                binding.emailErrorTv.error = null
                binding.emailErrorTv.visibility = View.INVISIBLE
            }
        })

        viewModel.passwordError.observe(this, Observer { error ->
            if (error != null) {
                binding.passwordErrorTv.text = error
                binding.passwordErrorTv.visibility = View.VISIBLE
            } else {
                binding.passwordErrorTv.error = null
                binding.passwordErrorTv.visibility = View.INVISIBLE
            }
        })

        viewModel.confirmPasswordError.observe(this, Observer { error ->
            if (error != null) {
                binding.confirmErrorTv.text = error
                binding.confirmErrorTv.visibility = View.VISIBLE
            } else {
                binding.confirmErrorTv.error = null
                binding.confirmErrorTv.visibility = View.INVISIBLE
            }
        })

        viewModel.navigateToHome.observe(this, Observer { navigateToHome ->
            if (navigateToHome) {
                // Navigate to HomeActivity
                Handler().postDelayed({
                    val intent = Intent(this, LoginActivity::class.java)
                    startActivity(intent)
                }, 2500)
            }
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK) {
            selectedImageUri = data?.data
            binding.profileImageView.setImageURI(selectedImageUri)
        }
    }
}