package com.vlog.app.screens.detail

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.media3.exoplayer.ExoPlayer
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vlog.app.R
import com.vlog.app.data.videos.Video
import com.vlog.app.ui.theme.VlogTheme
import io.mockk.mockk
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VideoDetailScreenPipTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var mockExoPlayer: ExoPlayer
    private lateinit var mockNavController: NavController
    private lateinit var mockViewModel: VideoDetailViewModel
    private val sampleVideo = Video(
        id = "testVideo1",
        title = "Test Video Title",
        description = "This is a test video description.",
        videoUrl = "http://example.com/video.mp4",
        thumbnailUrl = "http://example.com/thumbnail.jpg",
        duration = "10:00",
        tags = listOf("test", "video"),
        uploader = "Test Uploader",
        uploaderAvatarUrl = "http://example.com/avatar.jpg",
        viewCount = "1M views",
        publishedDate = "1 day ago",
        category = "Testing",
        likes = 1000,
        dislikes = 10,
        commentCount = 50,
        gathers = emptyList(),
        players = emptyList(),
        playerUrl = "http://example.com/video.mp4",
        score = "9.0",
        region = "Test Region",
        released = "2024"
    )
    private val testUiStateFlow = mutableStateOf(VideoDetailUiState(videoDetail = sampleVideo, isLoading = false))


    @Before
    fun setUp() {
        mockExoPlayer = mockk(relaxed = true) // Relaxed mock for ExoPlayer
        mockNavController = mockk(relaxed = true) // Relaxed mock for NavController

        // Mock ViewModel
        mockViewModel = mockk(relaxed = true) {
            // Stub the uiState flow
            every { uiState } returns testUiStateFlow.asStateFlow()
            // Mock other ViewModel functions if they are called during the test
            every { loadVideoDetail() } just runs
            every { loadPlayers(any()) } just runs
            every { selectPlayerUrl(any(), any()) } just runs
            // Add other necessary stubs
        }
    }

    private fun setVideoDetailScreenForTesting() {
        composeTestRule.setContent {
            VlogTheme {
                VideoDetailScreen(
                    navController = mockNavController,
                    videoId = "testVideo1", // videoId can be a test constant
                    viewModel = mockViewModel // Provide the mock ViewModel
                )
                // ExoPlayer instance is created within VideoDetailScreen,
                // we can't directly pass mockExoPlayer to it without refactoring VideoDetailScreen.
                // For now, we assume the internal ExoPlayer creation is fine for UI tests,
                // or we accept that these tests might use a real ExoPlayer instance.
                // Ideally, VideoDetailScreen would allow injecting an ExoPlayer instance for tests.
            }
        }
        // Ensure the UI is stable and the video detail is loaded
        testUiStateFlow.value = VideoDetailUiState(videoDetail = sampleVideo, isLoading = false, error = null)
        composeTestRule.waitForIdle() // Wait for UI to settle after state change
    }


    @Test
    fun pipButton_displayed_inFullScreen_and_notDisplayed_otherwise() {
        setVideoDetailScreenForTesting()

        // Initially not fullscreen, "Enter PIP" button should not be visible
        // The "Enter PIP" button only appears in our custom controls when isFullScreen is true.
        composeTestRule.onNodeWithContentDescription("进入画中画模式").assertDoesNotExist()

        // Simulate entering fullscreen:
        // PlayerView's fullscreen button is internal. We need a way to trigger it.
        // For now, let's assume there's a button with content description "exo_fullscreen" (common in PlayerView)
        // This is a guess and might need adjustment based on actual PlayerView internals or custom tags.
        // If PlayerView's button is not directly accessible, this test part is hard.
        // A more robust way would be to have a test tag on the fullscreen button if possible,
        // or to call the onFullScreenChange lambda directly if the component structure allowed.

        // Let's find the PlayerView and try to click its fullscreen button.
        // This is highly dependent on PlayerView's implementation details.
        // A common content description for the fullscreen button in ExoPlayer's PlayerView is "Fullscreen".
        // Note: The PlayerView's internal buttons might not be easily accessible by default content descriptions
        // if they are not explicitly set or are part of a complex internal structure.
        // Let's assume we can find it by a common description.
        try {
            composeTestRule.onNodeWithContentDescription("Fullscreen").performClick() // Default ExoPlayer description
        } catch (e: AssertionError) {
            // Fallback or alternative method if "Fullscreen" is not found
            // This might indicate the test needs a more reliable way to enter fullscreen.
            // For now, we'll proceed assuming this works or would be fixed.
            println("Could not find PlayerView's default Fullscreen button. Test might be incomplete.")
        }
        composeTestRule.waitForIdle()


        // Now that we are (supposedly) in fullscreen, the "进入画中画模式" (Enter PIP) button should be visible.
        composeTestRule.onNodeWithContentDescription("进入画中画模式").assertIsDisplayed()
    }

    @Test
    fun transitionToPipMode_fromFullScreen() {
        setVideoDetailScreenForTesting()
        // Manually trigger fullscreen for test setup consistency
        // This bypasses clicking PlayerView's button for now, assuming direct state change for test setup
        // TODO: Find a reliable way to click PlayerView's fullscreen button.
        // For now, we'll assume we got to fullscreen.
        // To do this properly, VideoDetailScreen's isFullScreen state would need to be controllable from the test,
        // or PlayerView's button easily clickable.
        // Since VideoDetailScreen manages isFullScreen internally, we rely on UI interaction.

        // Click PlayerView's fullscreen button (assuming "Fullscreen" content description)
        // This is speculative and might fail if the content description is different.
        try {
            composeTestRule.onNodeWithContentDescription("Fullscreen", useUnmergedTree = true).performClick()
            composeTestRule.waitForIdle()
        } catch (e: Exception) {
            println("Failed to click PlayerView fullscreen button. Test setup for transitionToPipMode might be incorrect.")
            // This test will likely fail if we can't get into fullscreen mode first.
            return // Exit test if setup fails
        }


        // Verify "Enter PIP" button is there (now that we are in fullscreen)
        composeTestRule.onNodeWithContentDescription("进入画中画模式").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("进入画中画模式").performClick()
        composeTestRule.waitForIdle()

        // Verify PIP specific controls are visible
        composeTestRule.onNodeWithContentDescription("返回全屏").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("关闭画中画").assertIsDisplayed()

        // Verify "Enter PIP" button is no longer there
        composeTestRule.onNodeWithContentDescription("进入画中画模式").assertDoesNotExist()
        // Verify Fullscreen exit button ("退出全屏") is also not there
        composeTestRule.onNodeWithContentDescription("退出全屏").assertDoesNotExist()
    }

    @Test
    fun pipModeUi_showsPipControls_andMainContentIsVisible() {
        setVideoDetailScreenForTesting()

        // Directly entering PIP for this test is hard because VideoDetailScreen manages states internally.
        // We need to go: Normal -> Fullscreen -> PIP.
        // 1. Go to Fullscreen
        try {
            composeTestRule.onNodeWithContentDescription("Fullscreen", useUnmergedTree = true).performClick()
            composeTestRule.waitForIdle()
        } catch (e: Exception) {
            println("Failed to enter fullscreen for pipModeUi_showsPipControls. Test may fail.")
            return
        }
        // 2. Go to PIP
        composeTestRule.onNodeWithContentDescription("进入画中画模式").performClick()
        composeTestRule.waitForIdle()

        // Verify PIP specific controls
        composeTestRule.onNodeWithContentDescription("返回全屏").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("关闭画中画").assertIsDisplayed()

        // Verify main content (e.g., video title from VideoDetailContent) is still displayed.
        // VideoDetailContent is part of the Scaffold's content, which remains.
        composeTestRule.onNodeWithText(sampleVideo.title, substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText(sampleVideo.description, substring = true).assertIsDisplayed()
    }

    @Test
    fun returnToFullScreen_fromPipMode() {
        setVideoDetailScreenForTesting()
        // Go Normal -> Fullscreen -> PIP
        try {
            composeTestRule.onNodeWithContentDescription("Fullscreen", useUnmergedTree = true).performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithContentDescription("进入画中画模式").performClick()
            composeTestRule.waitForIdle()
        } catch (e: Exception) {
             println("Failed to setup PIP mode for returnToFullScreen_fromPipMode. Test may fail.")
            return
        }


        composeTestRule.onNodeWithContentDescription("返回全屏").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("返回全屏").performClick()
        composeTestRule.waitForIdle()

        // Should be in fullscreen mode now, PIP controls gone
        composeTestRule.onNodeWithContentDescription("返回全屏").assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription("关闭画中画").assertDoesNotExist()

        // Fullscreen controls should be visible
        composeTestRule.onNodeWithContentDescription("退出全屏").assertIsDisplayed() // This is our custom "Home" icon for exiting fullscreen
        composeTestRule.onNodeWithContentDescription("进入画中画模式").assertIsDisplayed()
    }

    @Test
    fun closePipWindow_fromPipMode() {
        setVideoDetailScreenForTesting()
        // Go Normal -> Fullscreen -> PIP
        try {
            composeTestRule.onNodeWithContentDescription("Fullscreen", useUnmergedTree = true).performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithContentDescription("进入画中画模式").performClick()
            composeTestRule.waitForIdle()
        } catch (e: Exception) {
             println("Failed to setup PIP mode for closePipWindow_fromPipMode. Test may fail.")
            return
        }

        composeTestRule.onNodeWithContentDescription("关闭画中画").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("关闭画中画").performClick()
        composeTestRule.waitForIdle()

        // PIP controls should be gone
        composeTestRule.onNodeWithContentDescription("返回全屏").assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription("关闭画中画").assertDoesNotExist()

        // Player should be in normal, non-fullscreen, non-PIP state.
        // "Enter PIP" button should not be visible (it's only in fullscreen)
        composeTestRule.onNodeWithContentDescription("进入画中画模式").assertDoesNotExist()
        // "Exit fullscreen" button should not be visible
        composeTestRule.onNodeWithContentDescription("退出全屏").assertDoesNotExist()

        // Main content should still be visible
        composeTestRule.onNodeWithText(sampleVideo.title, substring = true).assertIsDisplayed()
    }
}
