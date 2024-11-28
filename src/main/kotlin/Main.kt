package org.exapmle

import kotlinx.coroutines.*
import org.exapmle.chatroom.Chatroom
import org.exapmle.chatroom.Chatter
import org.exapmle.database.DatabaseService
import java.net.ServerSocket


//Next time repair the get message history, add chatter names, add database
val chatrooms = arrayListOf(Chatroom("global"))
const val url: String = "jdbc:sqlite:./main.db"

fun main() = runBlocking {
    val server = ServerSocket(8080)
    println("Starting server")
    DatabaseService(url).prepareDatabase().close()

    chatrooms[0].startChatroom()
    while (true) {
        println("Waiting for connection...")

        val clientConnection = withContext(Dispatchers.IO) { server.accept() }
        launch(Dispatchers.IO) {
            chatrooms[0].addChatter(Chatter(clientConnection))
        }
    }
}