import org.exapmle.database.DatabaseService
import java.sql.Connection
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class TestClass {
    private val url: String = "jdbc:sqlite:/home/kondzitsu/Projects/Kotlin/kotlinChatroomServer/test.db"
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

    }
}