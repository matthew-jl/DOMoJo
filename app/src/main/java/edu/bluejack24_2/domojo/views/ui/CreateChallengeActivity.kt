package edu.bluejack24_2.domojo.views.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import edu.bluejack24_2.domojo.R
import edu.bluejack24_2.domojo.databinding.ActivityCreateChallengeBinding
import edu.bluejack24_2.domojo.viewmodels.CreateChallengeViewModel
import java.io.File
import java.io.FileOutputStream

class CreateChallengeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCreateChallengeBinding
    private lateinit var viewModel: CreateChallengeViewModel

    private var selectedIconUri: Uri? = null
    private var selectedBannerUri: Uri? = null
    private val PICK_ICON_REQUEST = 1001
    private val PICK_BANNER_REQUEST = 1002

    fun getRealFileFromUri(context: Context, uri: Uri): File {
        val inputStream = context.contentResolver.openInputStream(uri)
        val tempFile = File.createTempFile("upload_", ".jpg", context.cacheDir)
        val outputStream = FileOutputStream(tempFile)
        inputStream?.copyTo(outputStream)
        inputStream?.close()
        outputStream.close()
        return tempFile
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_challenge)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_create_challenge)
        viewModel = ViewModelProvider(this).get(CreateChallengeViewModel::class.java)

        binding.viewModel = viewModel
        binding.lifecycleOwner = this

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { onBackPressed() }

        binding.selectIconButton.setOnClickListener{
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, PICK_ICON_REQUEST)
        }

        binding.selectBannerButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, PICK_BANNER_REQUEST)
        }

        binding.createChallengeBtn.setOnClickListener {
            binding.challengeTitleErrorTv.visibility = View.GONE
            binding.challengeCategoryErrorTv.visibility = View.GONE
            binding.challengeDescriptionErrorTv.visibility = View.GONE
            binding.iconErrorTv.visibility = View.GONE
            binding.bannerErrorTv.visibility = View.GONE
            if(selectedIconUri == null) {
                binding.iconErrorTv.text = "Please select a icon picture"
                binding.iconErrorTv.visibility = View.VISIBLE
            }else if(selectedBannerUri == null) {
                binding.bannerErrorTv.text = "Please select a banner picture"
                binding.bannerErrorTv.visibility = View.VISIBLE
            } else {
                val iconFile = getRealFileFromUri(this, selectedIconUri!!)
                val bannerFile = getRealFileFromUri(this, selectedBannerUri!!)

                viewModel.onCreateClicked(this, iconFile, bannerFile)
            }
        }

        viewModel.challengeTitleError.observe(this, Observer { error ->
            if (error != null) {
                binding.challengeTitleErrorTv.text = error
                binding.challengeTitleErrorTv.visibility = View.VISIBLE
            } else {
                binding.challengeTitleErrorTv.error = null
                binding.challengeTitleErrorTv.visibility = View.INVISIBLE
            }
        })

        viewModel.challengeCategoryError.observe(this, Observer { error ->
            if (error != null) {
                binding.challengeCategoryErrorTv.text = error
                binding.challengeCategoryErrorTv.visibility = View.VISIBLE
            } else {
                binding.challengeCategoryErrorTv.error = null
                binding.challengeCategoryErrorTv.visibility = View.INVISIBLE
            }
        })

        viewModel.challengeDescriptionError.observe(this, Observer { error ->
            if (error != null) {
                binding.challengeDescriptionErrorTv.text = error
                binding.challengeDescriptionErrorTv.visibility = View.VISIBLE
            } else {
                binding.challengeDescriptionErrorTv.error = null
                binding.challengeDescriptionErrorTv.visibility = View.INVISIBLE
            }
        })

        viewModel.isLoading.observe(this, Observer { isLoading ->
            binding.createChallengeBtn.isEnabled = !isLoading
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        })

        viewModel.navigateToChallenge.observe(this, Observer { navigateToChallenge ->
            if (navigateToChallenge) {
                Handler().postDelayed({
                    val intent = Intent(this, ChallengeActivity::class.java)
                    startActivity(intent)
                }, 2500)
            }
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_ICON_REQUEST && resultCode == RESULT_OK) {
            selectedIconUri = data?.data
            binding.iconImageView.setImageURI(selectedIconUri)
        }else if (requestCode == PICK_BANNER_REQUEST && resultCode == RESULT_OK) {
            selectedBannerUri = data?.data
            binding.bannerImageView.setImageURI(selectedBannerUri)
        }
    }
}