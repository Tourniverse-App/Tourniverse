package com.example.tourniverse.fragments

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.example.tourniverse.models.Match
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.WriteBatch
import io.mockk.*
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class StandingsFragmentTest {

    private lateinit var standingsFragment: StandingsFragment
    private val mockFirestore = mockk<FirebaseFirestore>(relaxed = true)
    private val mockContext = mockk<Context>(relaxed = true)
    private val mockToast = mockk<Toast>(relaxed = true)

    @Before
    fun setUp() {
        // Mock Firestore and WriteBatch
        mockkStatic(FirebaseFirestore::class)
        val mockBatch = mockk<WriteBatch>(relaxed = true)
        val mockMatchesCollection = mockk<CollectionReference>(relaxed = true)
        val mockMatchDoc = mockk<DocumentReference>(relaxed = true)

        every { FirebaseFirestore.getInstance() } returns mockFirestore
        every { mockFirestore.batch() } returns mockBatch
        every { mockFirestore.collection("tournaments").document("testTournamentId").collection("matches") } returns mockMatchesCollection
        every { mockMatchesCollection.document("matchId1") } returns mockMatchDoc

        // Mock android.util.Log
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0

        // Initialize the fragment
        standingsFragment = StandingsFragment()

        // Access private fields
        val dbField = StandingsFragment::class.java.getDeclaredField("db")
        dbField.isAccessible = true
        dbField.set(standingsFragment, mockFirestore)

        val tournamentIdField = StandingsFragment::class.java.getDeclaredField("tournamentId")
        tournamentIdField.isAccessible = true
        tournamentIdField.set(standingsFragment, "testTournamentId")

        val fixturesField = StandingsFragment::class.java.getDeclaredField("fixtures")
        fixturesField.isAccessible = true
        fixturesField.set(
            standingsFragment,
            mutableListOf(
                Match("TeamA", "TeamB", 2, 3, id = "matchId1")
            )
        )
    }

    @Test
    fun `test saveScoresToFirestore updates match successfully`() {
        // Arrange
        val matchId = "matchId1"
        val mockBatch = mockFirestore.batch()
        val mockMatchesCollection = mockk<CollectionReference>(relaxed = true)
        val mockMatchDoc = mockk<DocumentReference>(relaxed = true)

        every { mockFirestore.collection("tournaments").document("testTournamentId").collection("matches") } returns mockMatchesCollection
        every { mockMatchesCollection.document(matchId) } returns mockMatchDoc

        // Act
        val saveScoresMethod = StandingsFragment::class.java.getDeclaredMethod("saveScoresToFirestore")
        saveScoresMethod.isAccessible = true
        saveScoresMethod.invoke(standingsFragment)

        // Assert
        verify(exactly = 1) { mockBatch.update(mockMatchDoc, mapOf("scoreA" to 2, "scoreB" to 3)) }
        verify(exactly = 1) { mockBatch.commit() }
    }

    /**
     * Helper function to return the expected match data for Firestore.
     */
    private fun matchData(): Map<String, Any> = mapOf(
        "scoreA" to 2,
        "scoreB" to 3
    )
}
