package org.exapmle

import com.sun.security.ntlm.Client
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket
import java.net.http.HttpRequest
import java.time.LocalDateTime

//Next time repair the get message history, add chatter names, add database
data class Chatter(val name: String, val connection: Socket)
data class Message(val chatter: Chatter, val message: String, val timestamp: LocalDateTime)

val chatters = mutableListOf<Chatter>()
val messages = arrayListOf<Message>()

fun main() = runBlocking {
    val server = ServerSocket(8080)

    println("Starting server")
    while (true) {
        println("Waiting for connection...")

        val clientConnection = withContext(Dispatchers.IO) {server.accept()}
        val inputReader = BufferedReader(InputStreamReader(clientConnection.inputStream))
        val output = BufferedWriter(OutputStreamWriter(clientConnection.outputStream))
//        val output = PrintWriter(clientConnection.getOutputStream(), true)

        output.println("Welcome to the chatroom! Please provide your username")
        println("Reading username from the client")
        val customerName = inputReader.readLine()

        if (customerName == null) {
            println("Chatter disconnected")
            break
        }

        val chatter = Chatter(customerName, clientConnection)
        chatters.add(chatter)
        println("new connection from $customerName. Launching a coroutine")
        sendChatHistory(messages, chatter)

        launch (Dispatchers.IO) {
            clientConnection.use {
                clientConnection.handleClientConnection(chatter)
            }
        }
    }
}

fun BufferedWriter.println(message: String) {
    this.write(message)
    this.newLine()
    this.flush()
}

fun Socket.handleClientConnection(chatter: Chatter) {
    val inputReader = BufferedReader(InputStreamReader(this.getInputStream()))
    val output = PrintWriter(this.getOutputStream(), true)
    println("message history: $messages")

    while(true) {
        val response = inputReader.readLine()
        if (response == null) {
            println("client disconnected")
            break
        }

        synchronized(messages) {
            messages.add(Message(chatter, response, LocalDateTime.now()))
        }

        println(response)
        output.println("$response back")
    }
}

fun infinite(body: () -> Unit) {
    while (true) {
        body()
    }
}

fun sendChatHistory(history: List<Message>, client: Chatter) {
    println("Sending chat history to a client")
    val output = PrintWriter(client.connection.getOutputStream(), true)
    output.println(history)
}
