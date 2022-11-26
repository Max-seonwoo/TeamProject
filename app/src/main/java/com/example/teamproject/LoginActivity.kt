package com.example.teamproject

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.example.teamproject.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class LoginActivity : AppCompatActivity() {
    var auth : FirebaseAuth? = null
    val binding by lazy {ActivityLoginBinding.inflate(layoutInflater)}
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        auth = FirebaseAuth.getInstance()

        binding.signin.setOnClickListener {
            val email = binding.email.text.toString()
            val password = binding.password.text.toString()
            signInEmail(email, password)
        }

        binding.signup.setOnClickListener {
            startActivity(Intent(this, CreateAccountActivity::class.java))
        }
    }

    fun signInEmail(email: String, password: String) {
        Firebase.auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) {
                if(it.isSuccessful) {
                    //Login Success
                    transitionPage1(it.result?.user)
                } else { //show the error message
                    Log.w("LoginActivity", "signInWithEmail", it.exception)
                    Toast.makeText(this, it.exception?.message, Toast.LENGTH_LONG).show()
                }
            }
    }

    fun transitionPage1(user:FirebaseUser?) {
        if(user != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish() //LoginActivity's terminated and MainActivity starts
        }
    }
    override fun onStart() { //automatic login function
        super.onStart()
        transitionPage1(auth?.currentUser)
    }

}