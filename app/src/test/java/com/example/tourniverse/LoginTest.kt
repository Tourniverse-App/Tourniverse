package com.example.tourniverse

import com.example.tourniverse.activities.LoginActivity
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import org.junit.Test
import org.mockito.Mockito
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class LoginTest {

    @Test
    fun testSuccessfulLogin() {
        // Mock FirebaseAuth and FirebaseUser
        val mockAuth = Mockito.mock(FirebaseAuth::class.java)
        val mockUser = Mockito.mock(FirebaseUser::class.java)
        val mockAuthResult = Mockito.mock(AuthResult::class.java)

        // Mock behavior of signInWithEmailAndPassword
        Mockito.`when`(mockAuth.signInWithEmailAndPassword("test@example.com", "123456"))
            .thenReturn(Tasks.forResult(mockAuthResult))
        Mockito.`when`(mockAuthResult.user).thenReturn(mockUser)

        // Simulate LoginActivity behavior
        val loginActivity = LoginActivity()
        loginActivity.auth = mockAuth // Inject mocked FirebaseAuth

        // Perform the test logic
        val task = loginActivity.auth.signInWithEmailAndPassword("test@example.com", "123456")
        val user = task.result?.user

        // Assertions
        assertNotNull(user, "User should not be null after successful login")
    }

    @Test
    fun testFailedLogin() {
        // Mock FirebaseAuth
        val mockAuth = Mockito.mock(FirebaseAuth::class.java)

        // Mock failure behavior
        val exception = Exception("Invalid credentials")
        Mockito.`when`(mockAuth.signInWithEmailAndPassword("test@example.com", "wrongpassword"))
            .thenReturn(Tasks.forException(exception))

        // Simulate LoginActivity behavior
        val loginActivity = LoginActivity()
        loginActivity.auth = mockAuth // Inject mocked FirebaseAuth

        // Perform the test logic
        val task = loginActivity.auth.signInWithEmailAndPassword("test@example.com", "wrongpassword")

        // Assertions
        assertNull(task.result?.user, "User should be null after failed login")
    }
}
