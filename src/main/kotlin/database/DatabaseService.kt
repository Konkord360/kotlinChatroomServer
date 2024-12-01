package org.exapmle.database

import org.exapmle.chatroom.Message
import java.sql.Connection
import java.sql.DriverManager
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class DatabaseService(dbPath: String) {
    private val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    val connection = DriverManager.getConnection(dbPath)!!

    fun prepareDatabase(): Connection {
        if (!requiredTablesExist()) {
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
        println("Connection to SQLite has been established and Database is ready")

        return connection
    }

    private fun requiredTablesExist(): Boolean {
        val preparedStatement = connection.prepareStatement("SELECT name FROM sqlite_master WHERE type='table' AND name=?")
        preparedStatement.setString(1, "MESSAGES")

        return preparedStatement.executeQuery().next()
    }

    fun persistMessages(messages: List<Message>) {
        val preparedStatement =
            connection.prepareStatement("INSERT INTO MESSAGES(username, message, timestamp) values (?, ?, ?)")

        for (message in messages) {
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

    fun loadChatHistory(): List<Message> {
        val messages = mutableListOf<Message>()
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
        return messages
    }
}