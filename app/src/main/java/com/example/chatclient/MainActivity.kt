package com.example.chatclient

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.widget.Button
import android.widget.EditText
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import java.io.IOException

class MainActivity : AppCompatActivity() {
    private lateinit var viewModel: ChatViewModel
    private lateinit var messageAdapter: MessageAdapter
    private var mediaRecorder: MediaRecorder? = null
    private var voiceFile: File? = null
    private var isRecording = false

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        if (permissions.all { it.value }) {
            startRecording()
        } else {
            // Handle permission denial
        }
    }

    private val pickFileLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { viewModel.sendFile(it, contentResolver, "FILE") }
    }

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { viewModel.sendFile(it, contentResolver, "IMAGE") }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewModel = ViewModelProvider(this).get(ChatViewModel::class.java)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        messageAdapter = MessageAdapter()
        recyclerView.adapter = messageAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        viewModel.messages.observe(this) { messages ->
            messageAdapter.submitList(messages)
            recyclerView.scrollToPosition(messages.size - 1)
        }

        val messageInput = findViewById<EditText>(R.id.messageInput)
        val sendButton = findViewById<Button>(R.id.sendButton)
        val fileButton = findViewById<Button>(R.id.fileButton)
        val imageButton = findViewById<Button>(R.id.imageButton)
        val voiceButton = findViewById<Button>(R.id.voiceButton)

        sendButton.setOnClickListener {
            val message = messageInput.text.toString().trim()
            if (message.isNotEmpty()) {
                viewModel.sendMessage("Meow: $message")
                messageInput.text.clear()
            }
        }

        fileButton.setOnClickListener {
            pickFileLauncher.launch("*/*")
        }

        imageButton.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        voiceButton.setOnClickListener {
            if (isRecording) {
                stopRecording()
                voiceButton.text = "Voice"
            } else {
                requestAudioPermissions()
            }
        }

        val serverIp = intent.getStringExtra("SERVER_IP") ?: "192.168.1.100"
        viewModel.connectToServer(serverIp)
    }

    private fun requestAudioPermissions() {
        val permissions = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        if (permissions.all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }) {
            startRecording()
        } else {
            requestPermissionLauncher.launch(permissions)
        }
    }

    private fun startRecording() {
        voiceFile = File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), "voice_${System.currentTimeMillis()}.mp3")
        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(voiceFile?.absolutePath)
            try {
                prepare()
                start()
                isRecording = true
                findViewById<Button>(R.id.voiceButton).text = "Stop"
            } catch (e: IOException) {
                // Handle error
            }
        }
    }

    private fun stopRecording() {
        mediaRecorder?.apply {
            stop()
            release()
        }
        mediaRecorder = null
        isRecording = false
        voiceFile?.let { viewModel.sendVoiceFile(it) }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaRecorder?.release()
        viewModel.disconnect()
    }
}