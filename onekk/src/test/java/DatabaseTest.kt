import androidx.test.core.app.ApplicationProvider
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.wheatgenetics.onekk.database.OnekkDatabase

@RunWith(RobolectricTestRunner::class)
class DatabaseTest {

    private lateinit var database: OnekkDatabase

    @Before
    fun setup() {
        database = OnekkDatabase.buildMemoryDatabase(ApplicationProvider
            .getApplicationContext())
    }

    @Test
    fun databaseTest() {

        assert(::database.isInitialized)

        //insert database testing code
    }
}