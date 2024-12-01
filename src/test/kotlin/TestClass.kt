import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.example.*
import org.example.chatroom.Chatroom
import org.example.database.DatabaseService
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.Socket
import java.sql.Connection
import kotlin.test.*

class TestClass {
    private val url: String = "jdbc:sqlite::memory:"
    private lateinit var connection: Connection

    @BeforeTest
    fun setUp() {
//        startServer()
        connection = DatabaseService(url).connection
    }

    @AfterTest
    fun afterTest() {
    }

    @Test
    fun `after receiving connection on a socket server asks client for a name and adds him to the chatroom`() =
        runBlocking {
            val chatroom = Chatroom("test")
            val chatrooms = arrayListOf(chatroom)
            val server = Server(chatrooms)
            server.startServer()

            val job = launch {
                server.acceptChatter()
            }

            Socket("127.0.0.1", 8080).use { client ->
                val input = BufferedReader(InputStreamReader(client.getInputStream()))
                val output = BufferedWriter(OutputStreamWriter(client.getOutputStream()))
                output.println("Kondzitsu")
                job.join()
            }
            assertEquals(1, chatroom.chatters.size)
            server.stopServer()
        }

    @Test
    fun `after receiving multiple connections on a socket server asks clients for username and adds them to the chatroom`() =
        runBlocking {
            val chatroom = Chatroom("test")
            val chatrooms = arrayListOf(chatroom)
            val server = Server(chatrooms)
            server.startServer()

            Socket("127.0.0.1", 8080).use { client ->
                Socket("127.0.0.1", 8080).use { client2 ->
                    val output = BufferedWriter(OutputStreamWriter(client.getOutputStream()))
                    val output2 = BufferedWriter(OutputStreamWriter(client2.getOutputStream()))
                    output.println("Kondzitsu")
                    output2.println("Malario")
                    val job = launch {
                        server.acceptChatter()
                    }
                    val job2 = launch {
                        server.acceptChatter()
                    }

                    job.join()
                    job2.join()
                }
            }
            assertEquals(2, chatroom.chatters.size)
            assertContains(chatroom.getChatters(), "Kondzitsu")
            assertContains(chatroom.getChatters(), "Malario")
            server.stopServer()
        }

    @Test
    fun `with multiple chatters online, ones message is broadcast to all but the broadcaster`() = runBlocking {
        val chatroom = Chatroom("test")
        val chatrooms = arrayListOf(chatroom)
        val server = Server(chatrooms)
        server.startServer()

        Socket("127.0.0.1", 8080).use { client ->
            Socket("127.0.0.1", 8080).use { client2 ->
                Socket("127.0.0.1", 8080).use { client3 ->
                    val output = BufferedWriter(OutputStreamWriter(client.getOutputStream()))
                    val output2 = BufferedWriter(OutputStreamWriter(client2.getOutputStream()))
                    val output3 = BufferedWriter(OutputStreamWriter(client3.getOutputStream()))

                    val inputChatter1 = BufferedReader(InputStreamReader(client.getInputStream()))
                    val inputChatter2 = BufferedReader(InputStreamReader(client2.getInputStream()))
                    val inputChatter3 = BufferedReader(InputStreamReader(client3.getInputStream()))

                    output.println("Kondzitsu")
                    output2.println("Malario")
                    output3.println("Konradio")

                    val job = launch {
                        server.acceptChatter()

                    }
                    val job2 = launch {
                        server.acceptChatter()
                    }
                    val job3 = launch {
                        server.acceptChatter()
                    }

                    job.join()
                    job2.join()
                    job3.join()
                    val chatters = chatroom.chatters

                    val chat1 = launch {
                        chatroom.readMessageFromChatter(chatters.elementAt(0))
                    }
                    output.println("testMessage")
                    chat1.join()

                    assertEquals(1, chatroom.messages.size)
                    //pleaseProvideName
                    inputChatter1.readLine()
                    inputChatter2.readLine()
                    inputChatter3.readLine()
                    //chatHistory
                    inputChatter1.readLine()
                    inputChatter2.readLine()
                    inputChatter3.readLine()
                    //broadcasted message
                    var broadcastedMessageBack = inputChatter2.readLine()
                    var broadcastedMessageBack2 = inputChatter3.readLine()
                    var broadcasterMessageBack = client.getInputStream().available()

                    assertEquals("testMessage", broadcastedMessageBack)
                    assertEquals("testMessage", broadcastedMessageBack2)
                    assertEquals(0, broadcasterMessageBack)
                }
            }
        }
    }

    private fun BufferedWriter.println(message: String) {
        this.write(message)
        this.newLine()
        this.flush()
    }
}
