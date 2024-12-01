package org.example.chatroom

import kotlinx.coroutines.delay
import org.example.database.DatabaseService
import org.example.url
import java.io.BufferedWriter
import java.time.LocalDateTime


data class Message(val chatterName: String, val message: String, val timestamp: LocalDateTime)

class Chatroom(name: String) {
    var chatters = mutableSetOf<Chatter>()
    var messages = mutableListOf<Message>()
    private val newMessages = arrayListOf<Message>()
    private val databaseService = DatabaseService(url)
    var serverUp = true

    fun startChatroom() {
        messages.addAll(databaseService.loadChatHistory())
    }

    fun getChatters(): List<String> {
        return chatters.map { it.name }
    }

    private fun broadcast(author: Chatter, message: String) {
        for (chatter in chatters) {
            if (chatter.name != author.name) {
                println("Sending $message to ${chatter.name}")
                chatter.output.println(message)
                println("Sending $message to ${chatter.name} sent")
            }
        }
    }

    fun addChatter(chatter: Chatter) {
        chatter.output.println("Welcome to the chatroom! Please provide your username")
        chatter.name = chatter.inputReader.readLine()!!
        try {
            println("chatter ${chatter.name} connected")

            chatters.add(chatter)
            sendChatHistory(chatter)
        } catch (e: Exception) {
            print("Client disconnected")
            chatters.remove(chatter)
        }
    }

    private fun sendChatHistory(chatter: Chatter) {
        println("Sending chat history to a chatter")
        chatter.output.println(messages.toString())
    }

    private fun BufferedWriter.println(message: String) {
        this.write(message)
        this.newLine()
        this.flush()
    }

    fun handleClientConnection(chatter: Chatter) {
        chatter.connection.use {
            while (serverUp) {
                if(!readMessageFromChatter(chatter)) {
                    break
                }
            }
        }
    }

    fun readMessageFromChatter(chatter: Chatter): Boolean {
        print("reading from ${chatter.name}")
        val response = chatter.inputReader.readLine()
        if (response == null) {
            println("Chatter ${chatter.name} disconnected")
            chatters.remove(chatter)
            return false
        }

        synchronized(messages) {
            synchronized(newMessages) {
                messages.add(Message(chatter.name, response, LocalDateTime.now()))
                newMessages.add(Message(chatter.name, response, LocalDateTime.now()))
            }
        }

        println(response)
        broadcast(chatter, response)
        return true
    }

    private fun persistNewMessages() {
        val localMessages = copyAndClearMessages()
        databaseService.persistMessages(localMessages)
    }

    @Synchronized
    fun copyAndClearMessages(): MutableList<Message> {
        val localMessages: MutableList<Message> = newMessages.toMutableList()
        newMessages.clear()
        return localMessages
    }

    suspend fun scheduleMessagePersistence() {
        while (true) {
            delay(5000)
            println("persisting new messages")
            persistNewMessages()
        }
    }
}
