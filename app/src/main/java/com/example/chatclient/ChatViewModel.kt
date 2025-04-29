package com.example.chatclient

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.*
import java.net.Socket
import java.util.concurrent.CopyOnWriteArrayList

class ChatViewModel : ViewModel() {
    private val _messages = MutableLiveData<List<String>>(emptyList())
    val messages: LiveData<List<String>> = _messages
    private var socket: Socket? = null
    private var writer: DataOutputStream? = null
    private var reader: DataInputStream? = null
    private val messageList = CopyOnWriteArrayList<String>()

    fun connectToServer(serverIp: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                socket = Socket(serverIp, 12347)
                writer = DataOutputStream(socket?.getOutputStream())
                reader = DataInputStream(socket?.getInputStream())
                listenForMessages()
            } catch (e: Exception) {
                messageList.add("Connection error: ${e.message}")
                _messages.postValue(messageList.toList())
            }
        }
    }

    private fun listenForMessages() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                while (true) {
                    val type = reader?.readUTF() ?: break
                    if (type.startsWith("TEXT:")) {
                        val message = type.substring(5)
                        messageList.add(message)
                        _messages.postValue(messageList.toList())
                    } else if (type.startsWith("FILE:") || type.startsWith("IMAGE:") || type.startsWith("VOICE:")) {
                        val fileName = reader?.readUTF() ?: break
                        val fileSize = reader?.readLong() ?: break
                        val fileData = ByteArray(fileSize.toInt())
                        var bytesRead = 0
                        while (bytesRead < fileSize) {
                            bytesRead += reader?.read(fileData, bytesRead, fileSize.toInt() - bytesRead) ?: break
                        }
                        saveFile(fileName, fileData, type.substring(0, type.length - 1))
                        messageList.add("${type.substring(0, type.length - 1)} received: $fileName")
                        _messages.postValue(messageList.toList())
                    }
                }
            } catch (e: Exception) {
                messageList.add("Error receiving message: ${e.message}")
                _messages.postValue(messageList.toList())
            } finally {
                disconnect()
            }
        }
    }

    fun sendMessage(message: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                writer?.writeUTF("TEXT:$message")
                writer?.flush()
                messageList.add(message)
                _messages.postValue(messageList.toList())
            } catch (e: Exception) {
                messageList.add("Error sending message: ${e.message}")
                _messages.postValue(messageList.toList())
            }
        }
    }

    fun sendFile(uri: Uri, contentResolver: ContentResolver, type: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val inputStream = contentResolver.openInputStream(uri)
                val fileName = uri.lastPathSegment ?: "file_${System.currentTimeMillis()}"
                val fileData = inputStream?.readBytes() ?: byteArrayOf()
                inputStream?.close()
                writer?.writeUTF("$type:")
                writer?.writeUTF(fileName)
                writer?.writeLong(fileData.size.toLong())
                writer?.write(fileData)
                writer?.flush()
                messageList.add("Sent $type: $fileName")
                _messages.postValue(messageList.toList())
            } catch (e: Exception) {
                messageList.add("Error sending file: ${e.message}")
                _messages.postValue(messageList.toList())
            }
        }
    }

    fun sendVoiceFile(file: File) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val fileData = file.readBytes()
                writer?.writeUTF("VOICE:")
                writer?.writeUTF(file.name)
                writer?.writeLong(fileData.size.toLong())
                writer?.write(fileData)
                writer?.flush()
                messageList.add("Sent VOICE: ${file.name}")
                _messages.postValue(messageList.toList())
            } catch (e: Exception) {
                messageList.add("Error sending voice: ${e.message}")
                _messages.postValue(messageList.toList())
            }
        }
    }

    private fun saveFile(fileName: String, fileData: ByteArray, type: String) {
        // Save file to app-specific storage (implement as needed)
        // For simplicity, just log receipt in the UI
    }

    fun disconnect() {
        try {
            reader?.close()
            writer?.close()
            socket?.close()
        } catch (e: Exception) {
            // Ignore
        }
    }
}