package org.exapmle.chatroom

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import org.exapmle.Message
import org.exapmle.database.DatabaseService
import java.io.BufferedWriter
import java.time.LocalDateTime

const val url: String = "jdbc:sqlite:/home/kondzitsu/Projects/Kotlin/kotlinChatroomServer/test.db"

class Chatroom(name: String) {
    private var chatters = mutableSetOf<Chatter>()
    private var messages = mutableListOf<Message>()
    private val newMessages = arrayListOf<Message>()
    private val databaseService = DatabaseService(url)

    fun startChatroom() {
        messages.addAll(databaseService.loadChatHistory())
    }

    private fun broadcast(author: Chatter, message: String) {
        for (chatter in chatters) {
            if (chatter.name != author.name) {
                println("Sending $message to ${chatter.name}")
                chatter.output.println(message)
            }
        }
    }

    suspend fun addChatter(chatter: Chatter) = coroutineScope {
        chatter.output.println("Welcome to the chatroom! Please provide your username")
        chatter.name = chatter.inputReader.readLine()
        println("chatter ${chatter.name} connected")

        chatters.add(chatter)
        sendChatHistory(chatter)
        chatter.connection.use {
            handleClientConnection(chatter)
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

    private fun handleClientConnection(chatter: Chatter) {
        while (true) {
            val response = chatter.inputReader.readLine()
            if (response == null) {
                println("Chatter ${chatter.name} disconnected")
                chatters.remove(chatter)
                break
            }

            synchronized(messages) {
                synchronized(newMessages) {
                    messages.add(Message(chatter.name, response, LocalDateTime.now()))
                    newMessages.add(Message(chatter.name, response, LocalDateTime.now()))
                }
            }

            println(response)
            broadcast(chatter, response)
        }
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