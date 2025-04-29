package com.example.chatclient

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class HomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        val ipInput = findViewById<EditText>(R.id.ipInput)
        val connectButton = findViewById<Button>(R.id.connectButton)

        connectButton.setOnClickListener {
            val serverIp = ipInput.text.toString().trim()
            if (serverIp.isNotEmpty() && serverIp.matches(Regex("^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}\$"))) {
                val intent = Intent(this, MainActivity::class.java)
                intent.putExtra("SERVER_IP", serverIp)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Please enter a valid IP address", Toast.LENGTH_SHORT).show()
            }
        }
    }
}