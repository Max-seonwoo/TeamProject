package com.example.teamproject

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.example.teamproject.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class LoginActivity : AppCompatActivity() {
    var auth : FirebaseAuth? = null
    val binding = ActivityLoginBinding.inflate(layoutInflater)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        auth = FirebaseAuth.getInstance()

        binding.signin.setOnClickListener {
            signInEmail()
        }

        binding.signup.setOnClickListener {
            startActivity(Intent(this, CreateAccountActivity::class.java))
        }
    }

    fun signInEmail() {
        auth?.signInWithEmailAndPassword(binding.email.text.toString(), binding.password.text.toString())
            ?.addOnCompleteListener {
                    task ->
                if(task.isSuccessful) {
                    //Login Success
                    transitionPage1(task.result?.user)
                }
                else {
                    //show the error message
                    Toast.makeText(this, task.exception?.message, Toast.LENGTH_LONG).show()
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