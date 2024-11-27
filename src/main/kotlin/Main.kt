package org.exapmle

import com.sun.security.ntlm.Client
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket
import java.net.http.HttpRequest
import java.sql.Connection
import java.time.LocalDateTime
import java.sql.SQLException
import java.sql.DriverManager
import java.time.format.DateTimeFormatter


//Next time repair the get message history, add chatter names, add database
data class Chatter(val name: String, val connection: Socket)
data class Message(val chatterName: String, val message: String, val timestamp: LocalDateTime)

val chatters = mutableListOf<Chatter>()
val messages = arrayListOf<Message>()
val newMessages = arrayListOf<Message>()
const val url: String = "jdbc:sqlite:/home/kondzitsu/Projects/Kotlin/kotlinChatroomServer/test.db"
val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

fun main() = runBlocking {
    val server = ServerSocket(8080)
    println("Starting server")

    val connection = prepareDatabase()
    loadChatHistory(connection)

    while (true) {
        println("Waiting for connection...")

        val clientConnection = withContext(Dispatchers.IO) { server.accept() }
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

        launch(Dispatchers.IO) {
            scheduleMessagePersistance(connection)
        }

        launch(Dispatchers.IO) {
            clientConnection.use {
                clientConnection.handleClientConnection(chatter)
            }
        }
    }
}

fun prepareDatabase(): Connection {
    val connection = DriverManager.getConnection(url)!!

    if (connection.metaData.getTables(null, null, "MESSAGES", arrayOf("MESSAGES")).next()) {
        println("messages table missing")
        connection.createStatement().use { statement ->
            statement.execute(
                "CREATE TABLE MESSAGES(" +
                        "id integer PRIMARY KEY," +
                        "username TEXT," +
                        "message TEXT, " +
                        "timestamp TEXT" +
                        ");"
            )
        }
        println("MESSAGES table created")
    }
    println("Connection to SQLite has been established.")

    return connection;
}

fun loadChatHistory(connection: Connection) {
    println("Loading chat history from database")
    connection.createStatement().use { statement ->
        statement.executeQuery("SELECT username, message, timestamp FROM MESSAGES").use { resultSet ->
            while (resultSet.next()) {
                messages.add(
                    Message(
                        resultSet.getString(1),
                        resultSet.getString(2),
                        LocalDateTime.parse(resultSet.getString(3), dateTimeFormatter)
                    )
                )
            }
        }
    }
    println("Chat history loaded")
}

fun persistNewMessages(connection: Connection) {
    val localMessages = copyAndClearMessages()

    val preparedStatement =
        connection.prepareStatement("INSERT INTO MESSAGES(username, message, timestamp) values (?, ?, ?)")

    for (message in localMessages) {
        preparedStatement.setString(1, message.chatterName)
        preparedStatement.setString(2, message.message)
        preparedStatement.setString(3, message.timestamp.format(dateTimeFormatter))
        preparedStatement.addBatch()
    }

    val resultArray = preparedStatement.executeBatch()
    if (resultArray.sum() == resultArray.size) {
        println("Added new messages to the history. Cleared new messages list")
    } else {
        println("Database update failed")
    }
    println("Exiting persisting new messages")
}

@Synchronized fun copyAndClearMessages() :MutableList<Message> {
    val localMessages: MutableList<Message> = newMessages.toMutableList()
    newMessages.clear()
    return localMessages
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

    while (true) {
        val response = inputReader.readLine()
        if (response == null) {
            println("client disconnected")
            break
        }

        synchronized(messages) {
            synchronized(newMessages) {
                messages.add(Message(chatter.name, response, LocalDateTime.now()))
                newMessages.add(Message(chatter.name, response, LocalDateTime.now()))
            }
        }
        println(response)
        output.println("$messages")
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

suspend fun scheduleMessagePersistance(connection: Connection) {
    while (true) {
        delay(5000)
        println("persisting new messages")
        persistNewMessages(connection)
    }
}
