import org.junit.Test
import org.mockito.Mockito.*
import com.example.tourniverse.utils.FirebaseHelper
import kotlin.test.assertTrue

class AddTournamentFragmentTest {

    @Test
    fun `test successful tournament creation`() {
        val tournamentName = "Friendly Tournament"
        val teamNames = listOf("Team 1", "Team 2", "Team 3", "Team 4")
        val format = "Tables"
        val privacy = "Public"

        FirebaseHelper.addTournament(
            name = tournamentName,
            teamCount = teamNames.size,
            description = "A test tournament",
            privacy = privacy,
            teamNames = teamNames,
            format = format
        ) { success, _ ->
            assertTrue(success)
        }
    }
}
