import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.ext.junit.rules.ActivityScenarioRule
import org.junit.Rule
import org.junit.Test
import org.junit.Before
import org.junit.runner.RunWith
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.PerformException
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.idling.CountingIdlingResource
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.espresso.util.HumanReadables
import androidx.test.espresso.util.TreeIterables
import com.example.tourniverse.R
import com.example.tourniverse.activities.LoginActivity
import com.example.tourniverse.activities.MainActivity
import com.example.tourniverse.activities.TournamentActivity
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher
import org.junit.BeforeClass
import java.util.concurrent.TimeoutException

@RunWith(AndroidJUnit4::class)
class TournamentUITest {

    @get:Rule
    var activityScenarioRule = ActivityScenarioRule(LoginActivity::class.java)

    private val idlingResource = CountingIdlingResource("TournamentFragment")

    companion object {
        var isLogged = false
    }

    @Before
    fun setUp() {
        if(!isLogged) {
            isLogged = true
            // Perform login only once before each test
            onView(withId(R.id.etEmail))
                .perform(typeText("gidirabi@gmail.com"), closeSoftKeyboard())
            onView(withId(R.id.etPassword))
                .perform(typeText("123456"), closeSoftKeyboard())
            onView(withId(R.id.btnLogin))
                .perform(click())

            // Wait for MainActivity to be launched
            onView(isRoot()).perform(waitForActivityToBe<MainActivity>(10000))

        }
    }

    @Test
    fun testCreateTournamentAndNavigateToSocialTab() {
        // Register the idling resource
        IdlingRegistry.getInstance().register(idlingResource)

        // Navigate to Add Tournament page
        onView(withId(R.id.nav_add))
            .perform(click())

        // Fill out tournament details
        onView(withId(R.id.etTournamentName))
            .perform(typeText("Test Tournament 1"), closeSoftKeyboard())
        onView(withId(R.id.spinnerNumTeams))
            .perform(click())
        onView(withText("2"))
            .perform(click())
        // Fill out team names using childAtPosition
        onView(childAtPosition(withId(R.id.layoutTeamNames), 0))
            .perform(typeText("Team 1"), closeSoftKeyboard())
        onView(childAtPosition(withId(R.id.layoutTeamNames), 1))
            .perform(typeText("Team 2"), closeSoftKeyboard())
        onView(withId(R.id.etDescription))
            .perform(typeText("Hello"), closeSoftKeyboard())
        onView(withId(R.id.btnSubmitTournament))
            .perform(click())

        Thread.sleep(5000)

        // Verify Tournament Details
        onView(withId(R.id.tvTournamentName))
            .check(matches(withText("Test Tournament 1")))
        onView(withId(R.id.tvTournamentDescription))
            .check(matches(withText("Hello")))

        // Unregister the idling resource
        IdlingRegistry.getInstance().unregister(idlingResource)
    }

    @Test
    fun testMissingTeamNameStaysOnAddTournamentPage() {
        // Register the idling resource
        IdlingRegistry.getInstance().register(idlingResource)

        Thread.sleep(2000)

        // Navigate to Add Tournament page
        onView(withId(R.id.nav_add))
            .perform(click())

        // Fill out tournament details but leave one team name blank
        onView(withId(R.id.etTournamentName))
            .perform(typeText("fail Tournament"), closeSoftKeyboard())
        onView(withId(R.id.spinnerNumTeams))
            .perform(click())
        onView(withText("2"))
            .perform(click())
        onView(childAtPosition(withId(R.id.layoutTeamNames), 0))
            .perform(typeText("Team 1"), closeSoftKeyboard())
        // Leave the second team name blank
        onView(withId(R.id.btnSubmitTournament))
            .perform(click())

        // Verify that the AddTournamentFragment is still active
        onView(withId(R.id.etTournamentName))
            .check(matches(isDisplayed()))
        onView(withId(R.id.layoutTeamNames))
            .check(matches(isDisplayed()))
        onView(withId(R.id.spinnerNumTeams))
            .check(matches(isDisplayed()))

        // Unregister the idling resource
        IdlingRegistry.getInstance().unregister(idlingResource)
    }

    private fun typeTextAtChild(parentId: Int, childIndex: Int, text: String): ViewAction {
        return object : ViewAction {
            override fun getConstraints(): Matcher<View> = isAssignableFrom(LinearLayout::class.java)
            override fun getDescription(): String = "Type text into child view at index $childIndex"
            override fun perform(uiController: UiController, view: View) {
                val parent = view.findViewById<LinearLayout>(parentId)
                val child = parent.getChildAt(childIndex) as EditText
                child.setText(text)
            }
        }
    }

    private inline fun <reified T : AppCompatActivity> waitForActivityToBe(timeout: Long): ViewAction {
        return object : ViewAction {
            override fun getConstraints(): Matcher<View> = isRoot()
            override fun getDescription(): String =
                "Wait for activity of type ${T::class.java.simpleName} to be active within $timeout ms."

            override fun perform(uiController: UiController, view: View) {
                val endTime = System.currentTimeMillis() + timeout
                var currentActivity: AppCompatActivity? = null

                do {
                    currentActivity = getCurrentActivity()
                    if (currentActivity is T) {
                        return // Activity of type T is active
                    }
                    uiController.loopMainThreadForAtLeast(50)
                } while (System.currentTimeMillis() < endTime)

                throw PerformException.Builder()
                    .withCause(TimeoutException("Activity of type ${T::class.java.simpleName} was not active within $timeout ms."))
                    .build()
            }
        }
    }

    private fun getCurrentActivity(): AppCompatActivity? {
        return try {
            val activityThreadClass = Class.forName("android.app.ActivityThread")
            val activityThread = activityThreadClass.getMethod("currentActivityThread").invoke(null)
            val activitiesField = activityThreadClass.getDeclaredField("mActivities")
            activitiesField.isAccessible = true
            val activities = activitiesField.get(activityThread) as Map<*, *>
            for (activityRecord in activities.values) {
                val activityRecordClass = activityRecord!!.javaClass
                val pausedField = activityRecordClass.getDeclaredField("paused")
                pausedField.isAccessible = true
                if (!(pausedField.get(activityRecord) as Boolean)) {
                    val activityField = activityRecordClass.getDeclaredField("activity")
                    activityField.isAccessible = true
                    return activityField.get(activityRecord) as AppCompatActivity
                }
            }
            null
        } catch (e: Exception) {
            null // Return null if any error occurs
        }
    }

    private fun logViewHierarchy(): ViewAction {
        return object : ViewAction {
            override fun getConstraints(): Matcher<View> = isRoot()
            override fun getDescription(): String = "Logs the view hierarchy"
            override fun perform(uiController: UiController, view: View) {
                TreeIterables.breadthFirstViewTraversal(view).forEach {
                    println("TestLogsHere: " + HumanReadables.describe(it))
                }
            }
        }
    }

    fun childAtPosition(
        parentMatcher: Matcher<View>,
        position: Int
    ): Matcher<View> {
        return object : TypeSafeMatcher<View>() {
            override fun describeTo(description: Description) {
                description.appendText("Child at position $position in parent ")
                parentMatcher.describeTo(description)
            }

            public override fun matchesSafely(view: View): Boolean {
                val parent = view.parent
                return parent is ViewGroup && parentMatcher.matches(parent) && parent.getChildAt(position) == view
            }
        }
    }
}
