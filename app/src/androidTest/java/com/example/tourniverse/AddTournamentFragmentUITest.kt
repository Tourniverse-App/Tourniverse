import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.example.tourniverse.R
import com.example.tourniverse.fragments.AddTournamentFragment
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AddTournamentFragmentUITest {

    @Test
    fun testCreatingTablesTournament() {
        // Launch the fragment in a test container
        val scenario = launchFragmentInContainer<AddTournamentFragment>()

        // Interact with the UI
        onView(withId(R.id.etTournamentName)).perform(typeText("Friendly Tournament"), closeSoftKeyboard())
        onView(withId(R.id.spinnerTournamentType)).perform(click())
        onView(withText("Tables")).perform(click())
        onView(withId(R.id.spinnerNumTeams)).perform(click())
        onView(withText("4")).perform(click())

        onView(withId(R.id.layoutTeamNames)).perform(scrollTo())
        onView(withHint("Team 1")).perform(typeText("Team 1"), closeSoftKeyboard())
        onView(withHint("Team 2")).perform(typeText("Team 2"), closeSoftKeyboard())
        onView(withHint("Team 3")).perform(typeText("Team 3"), closeSoftKeyboard())
        onView(withHint("Team 4")).perform(typeText("Team 4"), closeSoftKeyboard())

        onView(withId(R.id.btnSubmitTournament)).perform(click())

        onView(withText("Tournament created successfully!")).check(matches(isDisplayed()))
    }
}