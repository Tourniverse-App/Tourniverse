package com.example.tourniverse

import com.example.tourniverse.viewmodels.AddTournamentViewModel
import io.mockk.*
import org.junit.Test
import kotlin.test.assertEquals

class AddTournamentTest {

    @Test
    fun testAddTournamentSuccess() {
        // Mock the ViewModel
        val mockViewModel = mockk<AddTournamentViewModel>()

        // Mock behavior for the success scenario
        every {
            mockViewModel.addTournament(
                name = any(),
                teamCount = any(),
                description = any(),
                privacy = any(),
                teamNames = any(),
                format = any(),
                onComplete = captureLambda()
            )
        } answers {
            lambda<(Boolean, String?) -> Unit>().invoke(true, null) // Simulate success
        }

        // Call the method under test
        mockViewModel.addTournament(
            name = "Test Tournament",
            teamCount = 2,
            description = "This is a test tournament",
            privacy = "Public",
            teamNames = listOf("Team 1", "Team 2"),
            format = "Tables"
        ) { success, error ->
            // Assert that the callback indicates success
            assertEquals(true, success)
            assertEquals(null, error)
        }

        // Verify that the method was called with the expected arguments
        verify {
            mockViewModel.addTournament(
                name = "Test Tournament",
                teamCount = 2,
                description = "This is a test tournament",
                privacy = "Public",
                teamNames = listOf("Team 1", "Team 2"),
                format = "Tables",
                onComplete = any()
            )
        }
    }

    @Test
    fun testAddTournamentFailure() {
        // Mock the ViewModel
        val mockViewModel = mockk<AddTournamentViewModel>()

        // Mock behavior for the failure scenario
        every {
            mockViewModel.addTournament(
                name = any(),
                teamCount = any(),
                description = any(),
                privacy = any(),
                teamNames = any(),
                format = any(),
                onComplete = captureLambda()
            )
        } answers {
            lambda<(Boolean, String?) -> Unit>().invoke(false, "Failed to add tournament") // Simulate failure
        }

        // Call the method under test
        mockViewModel.addTournament(
            name = "",
            teamCount = 0,
            description = "",
            privacy = "Public",
            teamNames = emptyList(),
            format = "Tables"
        ) { success, error ->
            // Assert that the callback indicates failure
            assertEquals(false, success)
            assertEquals("Failed to add tournament", error)
        }

        // Verify that the method was called with the expected arguments
        verify {
            mockViewModel.addTournament(
                name = "",
                teamCount = 0,
                description = "",
                privacy = "Public",
                teamNames = emptyList(),
                format = "Tables",
                onComplete = any()
            )
        }
    }
}
