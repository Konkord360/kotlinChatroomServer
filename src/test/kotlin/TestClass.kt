import org.exapmle.chatroom.Chatroom
import org.exapmle.database.DatabaseService
import java.net.Socket
import java.sql.Connection
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class TestClass {
    private val url: String = "jdbc:sqlite:./test.db"
    private lateinit var connection: Connection

    @BeforeTest
    fun setUp() {
        connection = DatabaseService(url).prepareDatabase()
    }

    @AfterTest
    fun afterTest() {
    }

    @Test
    fun `test `() {
        val chatroom = Chatroom("test")
        assertEquals(chatroom.messages.isEmpty(), true)
//        chatroom.
//        Socket("127.0.0.1", 8080).use{ connection ->
//
//        }
    }
}