import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import com.example.tourniverse.fragments.AddTournamentFragment
import com.example.tourniverse.utils.FirebaseWrapper
import com.example.tourniverse.viewmodels.AddTournamentViewModel
import org.junit.Rule
import org.junit.Test
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule

class AddTournamentFragmentTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val mockitoRule: MockitoRule = MockitoJUnit.rule()

    @Mock
    private lateinit var mockViewModel: AddTournamentViewModel

    @Mock
    private lateinit var mockFirebaseWrapper: FirebaseWrapper

    @InjectMocks
    private lateinit var fragment: AddTournamentFragment

    @Test
    fun testAddTournament() {
        // Mock FirebaseWrapper and currentUser
        `when`(mockFirebaseWrapper.currentUserId).thenReturn("testUserId")

        // Mock ViewModel's addTournament method
        val successLiveData = MutableLiveData<Boolean>()
        successLiveData.value = true
        `when`(mockViewModel.addTournament(anyString(), anyInt(), anyString(), anyString(), anyList(), anyString(), any())).thenAnswer {
            val callback = it.getArgument<(Boolean, String?) -> Unit>(6)
            callback(true, null)
        }

        // Simulate user input using reflection to access private fields
        fragment.javaClass.getDeclaredField("etTournamentName").apply {
            isAccessible = true
            (get(fragment) as EditText).setText("Test Tournament")
        }
        fragment.javaClass.getDeclaredField("etDescription").apply {
            isAccessible = true
            (get(fragment) as EditText).setText("A friendly tournament")
        }
        fragment.javaClass.getDeclaredField("spinnerPrivacy").apply {
            isAccessible = true
            (get(fragment) as Spinner).setSelection(0) // Public
        }
        fragment.javaClass.getDeclaredField("spinnerTournamentType").apply {
            isAccessible = true
            (get(fragment) as Spinner).setSelection(0) // Tables
        }
        fragment.javaClass.getDeclaredField("spinnerNumTeams").apply {
            isAccessible = true
            (get(fragment) as Spinner).setSelection(3) // 4 teams
        }
        fragment.javaClass.getDeclaredField("layoutTeamNames").apply {
            isAccessible = true
            val layout = get(fragment) as LinearLayout
            layout.addView(EditText(fragment.requireContext()).apply { setText("Team 1") })
            layout.addView(EditText(fragment.requireContext()).apply { setText("Team 2") })
            layout.addView(EditText(fragment.requireContext()).apply { setText("Team 3") })
            layout.addView(EditText(fragment.requireContext()).apply { setText("Team 4") })
        }

        // Call handleSubmit using reflection
        fragment.javaClass.getDeclaredMethod("handleSubmit").apply {
            isAccessible = true
            invoke(fragment)
        }

        // Verify the behavior
        verify(mockViewModel).addTournament(
            eq("Test Tournament"),
            eq(4),
            eq("A friendly tournament"),
            eq("Public"),
            eq(listOf("Team 1", "Team 2", "Team 3", "Team 4")),
            eq("Tables"),
            any()
        )
    }
}