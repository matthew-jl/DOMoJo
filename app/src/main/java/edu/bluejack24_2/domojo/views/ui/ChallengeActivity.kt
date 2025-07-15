package edu.bluejack24_2.domojo.views.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.google.firebase.firestore.FirebaseFirestore
import edu.bluejack24_2.domojo.R
import edu.bluejack24_2.domojo.adapters.ChallengeAdaptor
import edu.bluejack24_2.domojo.databinding.ActivityChallengeBinding
import edu.bluejack24_2.domojo.models.Challenge
import edu.bluejack24_2.domojo.viewmodels.LoginViewModel

class ChallengeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChallengeBinding
    private lateinit var viewModel: LoginViewModel
    private lateinit var challengeList: ArrayList<Challenge>
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_challenge)

        db = FirebaseFirestore.getInstance()

        binding = DataBindingUtil.setContentView(this, R.layout.activity_challenge)

        binding.addChallengeFab.setOnClickListener{
            val intent = Intent(this, CreateChallengeActivity::class.java)
            startActivity(intent)
            finish()
        }

        db.collection("challenges")
            .get()
            .addOnSuccessListener { documents ->
                if(documents.isEmpty) {
                    Toast.makeText(
                        this,
                        "No challenges available at the moment.",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@addOnSuccessListener
                }else{
                    challengeList = ArrayList()
                    for (document in documents) {
                        val challenge = document.toObject(Challenge::class.java)
                        if(challenge != null){
                            challengeList.add(challenge)
                        }
                    }
                    val challengeAdapter = ChallengeAdaptor(challengeList)
                    binding.challengesRecyclerView.adapter = challengeAdapter
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(
                    this,
                    "Failed to load challenges: ${exception.toString()}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }
}