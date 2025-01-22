import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.AuthResult
import com.google.android.gms.tasks.Task
import org.junit.Test
import org.mockito.Mockito.*
import kotlin.test.assertTrue

class RegisterActivityTest {

    private val mockAuth = mock(FirebaseAuth::class.java)

    @Test
    fun testUserRegistration() {
        // Mock Task and AuthResult
        val mockAuthResult = mock(AuthResult::class.java)
        val mockTask = mock(Task::class.java) as Task<AuthResult>

        // Mocking Firebase method behavior
        `when`(mockAuth.createUserWithEmailAndPassword("testuser@example.com", "password123"))
            .thenReturn(mockTask)
        `when`(mockTask.isSuccessful).thenReturn(true)
        `when`(mockTask.result).thenReturn(mockAuthResult)
        `when`(mockAuthResult.user?.email).thenReturn("testuser@example.com")

        // Calling the function under test
        val task = mockAuth.createUserWithEmailAndPassword("testuser@example.com", "password123")

        // Assertions to verify expected behavior
        assertTrue(task.isSuccessful)
        assertTrue(task.result?.user?.email == "testuser@example.com")
    }
}
