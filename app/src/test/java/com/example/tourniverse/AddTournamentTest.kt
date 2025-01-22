package com.example.tourniverse

import androidx.lifecycle.MutableLiveData
import com.example.tourniverse.fragments.AddTournamentFragment
import com.example.tourniverse.viewmodels.AddTournamentViewModel
import org.junit.Test
import org.mockito.Mockito
import kotlin.test.assertEquals

class AddTournamentTest {

    @Test
    fun testAddTournamentSuccess() {
        // Mock ViewModel
        val mockViewModel = Mockito.mock(AddTournamentViewModel::class.java)

        // Mock success callback
        val successCallback: (Boolean, String?) -> Unit = { success, error ->
            assertEquals(true, success)
            assertEquals(null, error)
        }

        // Mock fetch tournament ID callback
        val fetchTournamentCallback: (String) -> Unit = { tournamentId ->
            assertEquals("123", tournamentId) // Assuming "123" is the expected tournament ID
        }

        // Test addTournament in ViewModel
        mockViewModel.addTournament(
            name = "Test Tournament",
            teamCount = 2,
            description = "This is a test tournament",
            privacy = "Public",
            teamNames = listOf("Team 1", "Team 2"),
            format = "Tables",
            onComplete = successCallback
        )

        // Verify that addTournament was called with correct parameters
        Mockito.verify(mockViewModel).addTournament(
            name = "Test Tournament",
            teamCount = 2,
            description = "This is a test tournament",
            privacy = "Public",
            teamNames = listOf("Team 1", "Team 2"),
            format = "Tables",
            onComplete = successCallback
        )
    }

    @Test
    fun testAddTournamentFailure() {
        // Mock ViewModel
        val mockViewModel = Mockito.mock(AddTournamentViewModel::class.java)

        // Mock failure callback
        val failureCallback: (Boolean, String?) -> Unit = { success, error ->
            assertEquals(false, success)
            assertEquals("Failed to add tournament", error)
        }

        // Simulate failure in addTournament
        mockViewModel.addTournament(
            name = "",
            teamCount = 0,
            description = "",
            privacy = "Public",
            teamNames = emptyList(),
            format = "Tables",
            onComplete = failureCallback
        )

        // Verify that addTournament was called
        Mockito.verify(mockViewModel).addTournament(
            name = "",
            teamCount = 0,
            description = "",
            privacy = "Public",
            teamNames = emptyList(),
            format = "Tables",
            onComplete = failureCallback
        )
    }
}
