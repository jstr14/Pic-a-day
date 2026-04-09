import app.cash.turbine.test
import com.jstr14.picaday.domain.model.User
import com.jstr14.picaday.domain.repository.AuthRepository
import com.jstr14.picaday.ui.auth.AuthState
import com.jstr14.picaday.ui.auth.AuthViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var authRepository: AuthRepository
    private lateinit var viewModel: AuthViewModel
    private val userFlow = MutableStateFlow<User?>(null)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        userFlow.value = null
        authRepository = mockk(relaxed = true)
        every { authRepository.currentUser } returns userFlow
        viewModel = AuthViewModel(authRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `signInWithGoogle updates state to Loading and then Success`() = runTest {
        //Given
        val fakeUser =
            User(id = "1", email = "test@test.com", displayName = "User", photoUrl = null)

        coEvery { authRepository.signInWithGoogle(any()) } coAnswers {
            delay(100)
            userFlow.value = fakeUser
            Result.success(fakeUser)
        }

        //When
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        //Then
        viewModel.uiState.test {
            assertEquals(AuthState.Idle, awaitItem())

            viewModel.signInWithGoogle("fake_token")

            val loadingState = awaitItem()
            assertTrue(
                "Expected Loading state but found $loadingState",
                loadingState is AuthState.Loading
            )

            val successState = awaitItem()
            assertTrue(
                "Expected Success state but found $successState",
                successState is AuthState.Success
            )

            val actualUser = (successState as AuthState.Success).user
            assertEquals("User in Success state does not match", fakeUser, actualUser)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `signInWithGoogle updates state to Loading and then Error when repository fails`() =
        runTest {
            //Given
            val errorMessage = "Network Error"
            val expectedFullMessage = "Google Sign-In failed: $errorMessage"

            coEvery { authRepository.signInWithGoogle(any()) } coAnswers {
                delay(100)
                Result.failure(Exception(errorMessage))
            }

            //When
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect { }
            }

            //Then
            viewModel.uiState.test {
                assertEquals(AuthState.Idle, awaitItem())

                viewModel.signInWithGoogle("fake_token")

                val loadingState = awaitItem()
                assertTrue(
                    "Expected Loading state but found $loadingState",
                    loadingState is AuthState.Loading
                )

                val errorState = awaitItem()
                assertTrue(
                    "Expected Error state but found $errorState",
                    errorState is AuthState.Error
                )
                assertEquals(
                    "Error message should match the repository exception",
                    expectedFullMessage,
                    (errorState as AuthState.Error).message
                )

                coVerify(exactly = 1) { authRepository.signInWithGoogle("fake_token") }

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `signOut should call repository and update state to Idle`() = runTest {
        val fakeUser = User("1", "t@t.com", "User", null)
        userFlow.value = fakeUser

        backgroundScope.launch { viewModel.uiState.collect { } }

        viewModel.uiState.test {
            val firstItem = awaitItem()
            assertTrue("Expected Success but was $firstItem", firstItem is AuthState.Success)

            viewModel.signOut()
            userFlow.value = null

            assertEquals(AuthState.Idle, awaitItem())
        }
    }

    @Test
    fun `should start in Success state if user is already in repository`() = runTest {
        val loggedUser = User("1", "t@t.com", "User", null)
        userFlow.value = loggedUser

        val autoLoginViewModel = AuthViewModel(authRepository)
        backgroundScope.launch { autoLoginViewModel.uiState.collect { } }

        autoLoginViewModel.uiState.test {
            val state = awaitItem()
            if (state is AuthState.Idle) {
                assertTrue(awaitItem() is AuthState.Success)
            } else {
                assertTrue(state is AuthState.Success)
            }
        }
    }

    @Test
    fun `isInitializing should be true initially and false after delay`() = runTest {
        assertTrue(viewModel.isInitializing.value)

        advanceTimeBy(1000)
        runCurrent()

        assertFalse(viewModel.isInitializing.value)
    }

    @Test
    fun `signInWithGoogle updates state to Error if token is blank`() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }

        viewModel.uiState.test {
            assertEquals(AuthState.Idle, awaitItem())

            viewModel.signInWithGoogle("") // Token vacío

            val errorState = awaitItem()
            assertTrue(errorState is AuthState.Error)
            assertEquals("Invalid Token", (errorState as AuthState.Error).message)

            coVerify(exactly = 0) { authRepository.signInWithGoogle(any()) }
        }
    }
}
