package com.example.tourniverse

import com.example.tourniverse.activities.RegisterActivity
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import io.mockk.*
import org.junit.Test
import org.robolectric.annotation.Config
import kotlin.test.assertTrue

@Config(manifest = Config.NONE, sdk = [30]) // Ensure Robolectric is configured
class RegisterActivityTest {

    @Test
    fun testSuccessfulRegistration() {
        // Mock dependencies
        val mockAuth = mockk<FirebaseAuth>(relaxed = true)
        val mockFirestore = mockk<FirebaseFirestore>(relaxed = true)
        val mockTask = mockk<Task<AuthResult>>(relaxed = true)

        // Spy on the RegisterActivity
        val registerActivity = spyk(RegisterActivity(), recordPrivateCalls = true)
        registerActivity.auth = mockAuth // Inject mocked FirebaseAuth

        // Mock FirebaseAuth behavior
        every {
            mockAuth.createUserWithEmailAndPassword(any(), any())
        } returns mockTask

        every { mockTask.addOnCompleteListener(any()) } answers {
            val listener = arg<(Task<AuthResult>) -> Unit>(0)
            listener.invoke(mockTask) // Simulate onComplete
            mockTask
        }

        every { mockTask.isSuccessful } returns true

        // Call registerUser
        val username = "testuser"
        val email = "testuser@example.com"
        val password = "123456"

        registerActivity.registerUser(username, email, password)

        // Verify method interactions
        verify { mockAuth.createUserWithEmailAndPassword(email, password) }
        verify { mockFirestore.collection("users").document(any()) }
    }

    @Test
    fun testRegistrationFailsForShortPassword() {
        // Spy on the RegisterActivity
        val registerActivity = spyk(RegisterActivity(), recordPrivateCalls = true)

        // Mock the showToast method
        every { registerActivity.showToast(any()) } just Runs

        // Call registerUser with a short password
        val username = "testuser"
        val email = "testuser@example.com"
        val password = "123" // Invalid password

        registerActivity.registerUser(username, email, password)

        // Verify that the showToast method was called with the correct message
        verify { registerActivity.showToast("Password must be at least 6 characters") }

        // Ensure no FirebaseAuth interaction occurred
        verify { registerActivity.auth wasNot Called }
    }

}
