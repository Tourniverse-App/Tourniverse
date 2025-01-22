import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.ext.junit.rules.ActivityScenarioRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.PerformException
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.idling.CountingIdlingResource
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.espresso.util.HumanReadables
import androidx.test.espresso.util.TreeIterables
import com.example.tourniverse.R
import com.example.tourniverse.activities.*
import org.hamcrest.TypeSafeMatcher
import java.util.concurrent.TimeoutException
import org.hamcrest.Matcher

@RunWith(AndroidJUnit4::class)
class RegisterUITest {

    @get:Rule
    var activityScenarioRule = ActivityScenarioRule(RegisterActivity::class.java)

    private val idlingResource = CountingIdlingResource("HomeFragment")

    @Test
    fun testSuccessfulRegistrationNavigatesToMainActivity() {
        // Register the idling resource
        IdlingRegistry.getInstance().register(idlingResource)

        // Enter username
        onView(withId(R.id.etUsername))
            .perform(typeText("testuser"), closeSoftKeyboard())

        // Enter email
        onView(withId(R.id.etEmail))
            .perform(typeText("testuser27@example.com"), closeSoftKeyboard())

        // Enter password
        onView(withId(R.id.etPassword))
            .perform(typeText("123456"), closeSoftKeyboard())

        // Click the register button
        onView(withId(R.id.btnRegister))
            .perform(click())

        // Wait for MainActivity to be launched
        onView(isRoot()).perform(waitForActivityToBe<MainActivity>(15000))

        // Debugging: Log the view hierarchy
        onView(isRoot()).perform(logViewHierarchy())

        // Unregister the idling resource
        IdlingRegistry.getInstance().unregister(idlingResource)
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

    @Test
    fun testShortPasswordStaysOnSameActivity() {
        // Enter valid username and email, but a short password
        onView(withId(R.id.etUsername))
            .perform(typeText("testuser"), closeSoftKeyboard())
        onView(withId(R.id.etEmail))
            .perform(typeText("testuser28@example.com"), closeSoftKeyboard())
        onView(withId(R.id.etPassword))
            .perform(typeText("123"), closeSoftKeyboard()) // Short password

        // Click the register button
        onView(withId(R.id.btnRegister)).perform(click())

        // Check that the RegisterActivity UI elements are still displayed
        onView(withId(R.id.etUsername)).check(matches(isDisplayed()))
        onView(withId(R.id.etEmail)).check(matches(isDisplayed()))
        onView(withId(R.id.etPassword)).check(matches(isDisplayed()))
        onView(withId(R.id.btnRegister)).check(matches(isDisplayed()))
    }


}