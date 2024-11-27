package org.exapmle.chatroom

import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.Socket

data class Chatter(val connection: Socket) {
    val inputReader = BufferedReader(InputStreamReader(connection.inputStream))
    val output = BufferedWriter(OutputStreamWriter(connection.outputStream))
    var name: String = ""
}
