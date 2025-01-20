import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import com.example.tourniverse.R
import com.example.tourniverse.activities.RegisterActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RegisterActivityUITest {

    @get:Rule
    val activityRule = ActivityScenarioRule(RegisterActivity::class.java) // Start with the registration activity

    @Test
    fun testSuccessfulRegistrationNavigatesToHome() {
        // Fill in the registration form
        onView(withId(R.id.etUsername)).perform(typeText("testuser"), closeSoftKeyboard())
        onView(withId(R.id.etEmail)).perform(typeText("testuser@example.com"), closeSoftKeyboard())
        onView(withId(R.id.etPassword)).perform(typeText("password123"), closeSoftKeyboard())
        onView(withId(R.id.btnRegister)).perform(click())

        // Verify navigation to the HomeFragment
        // Check the HomeFragment's title "Tourniverse"
        onView(withId(R.id.appTitle)).check(matches(isDisplayed()))
        onView(withText("Tourniverse")).check(matches(isDisplayed()))

        // Check the "No Tournaments Yet" message (assuming no tournaments exist)
        onView(withId(R.id.noTournamentsView)).check(matches(isDisplayed()))
        onView(withText("No\nTournaments\nYet")).check(matches(isDisplayed()))

        // Check the "Join Tournament" button
        onView(withId(R.id.buttonJoinTournament)).check(matches(isDisplayed()))
    }
}
