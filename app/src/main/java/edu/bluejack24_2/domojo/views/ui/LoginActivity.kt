package edu.bluejack24_2.domojo.views.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import edu.bluejack24_2.domojo.R
import edu.bluejack24_2.domojo.databinding.ActivityLoginBinding
import edu.bluejack24_2.domojo.viewmodels.LoginViewModel

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var viewModel: LoginViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_login)
        viewModel = ViewModelProvider(this).get(LoginViewModel::class.java)

        binding.viewModel = viewModel
        binding.lifecycleOwner = this

        viewModel.setActivity(this)

        binding.loginBtn.setOnClickListener {
            viewModel.onLoginClicked()
        }

        binding.signupLink.setOnClickListener {
            binding.emailErrorTv.visibility = View.GONE
            binding.passwordErrorTv.visibility = View.GONE

            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

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

        viewModel.navigateToHome.observe(this, Observer { navigateToHome ->
            if (navigateToHome) {
                // Navigate to HomeActivity
                Handler().postDelayed({
                    val intent = Intent(this, LandingActivity::class.java)
                    startActivity(intent)
                }, 2500)
            }
        })
    }
}