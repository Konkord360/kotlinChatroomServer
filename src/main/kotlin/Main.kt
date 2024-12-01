package org.exapmle

import kotlinx.coroutines.*
import org.exapmle.chatroom.Chatroom
import org.exapmle.chatroom.Chatter
import org.exapmle.database.DatabaseService
import java.net.ServerSocket


//Next time repair the get message history, add chatter names, add database
val chatrooms = arrayListOf(Chatroom("global"))
const val url: String = "jdbc:sqlite:./main.db"
var serverUp = true
val server = ServerSocket(8080)

data class Server(var chatrooms: List<Chatroom>) {}

fun main() = runBlocking {
    DatabaseService(url).prepareDatabase().close()
    val server = Server(chatrooms)
    server.startServer()
    while (serverUp) {
        println("Waiting for connection...")
        val newChatter = server.acceptChatter()
        launch {
            chatrooms[0].handleClientConnection(newChatter)
        }
    }
//    while (serverUp) {
//        acceptChatter()
//    }
}

fun Server.startServer() = runBlocking {
    DatabaseService(url).prepareDatabase().close()
    println("Starting server")
    chatrooms.forEach { chatroom -> chatroom.startChatroom()}
//        .startChatroom()

}

fun Server.acceptChatter(): Chatter {
    val clientConnection =  server.accept()
    val chatter = Chatter(clientConnection)
    chatrooms[0].addChatter(chatter)
    return chatter
}

fun Server.stopServer() {
    serverUp = false
    for(chatroom in chatrooms) {
        chatroom.serverUp = false
    }
}

fun Server.getChatrooms(): List<Chatroom> {
    return this.chatrooms;
}

fun Server.setChatrooms(chatrooms: List<Chatroom>) {
    this.chatrooms = chatrooms
}