package cat.urv.cloudlab.soundless.view.activity

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.navigation.NavDeepLinkBuilder
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import androidx.test.runner.AndroidJUnit4
import cat.urv.cloudlab.soundless.R
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.allOf
import org.hamcrest.TypeSafeMatcher
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class AllTest {

    @Rule
    @JvmField
    var mActivityTestRule = ActivityTestRule(MainActivity::class.java)

    private fun launchFragment(destinationId: Int,
                               argBundle: Bundle? = null) {
        val launchFragmentIntent = buildLaunchFragmentIntent(destinationId, argBundle)
        mActivityTestRule.launchActivity(launchFragmentIntent)
    }

    private fun buildLaunchFragmentIntent(destinationId: Int, argBundle: Bundle?): Intent =
        NavDeepLinkBuilder(InstrumentationRegistry.getInstrumentation().targetContext)
            .setGraph(R.navigation.nav_graph)
            .setComponentName(MainActivity::class.java)
            .setDestination(destinationId)
            .setArguments(argBundle)
            .createTaskStackBuilder().intents[0]

    @Before
    fun grantPhonePermission() {
        // In M+, trying to call a number will trigger a runtime dialog. Make sure
        // the permission is granted before running this test.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            InstrumentationRegistry.getInstrumentation().getUiAutomation().executeShellCommand(
                "pm grant " + androidx.test.InstrumentationRegistry.getTargetContext().packageName
                        + " android.permission.ACCESS_FINE_LOCATION"
            )
            InstrumentationRegistry.getInstrumentation().getUiAutomation().executeShellCommand(
                "pm grant " + androidx.test.InstrumentationRegistry.getTargetContext().packageName
                        + " android.permission.ACCESS_COARSE_LOCATION"
            )
            InstrumentationRegistry.getInstrumentation().getUiAutomation().executeShellCommand(
                "pm grant " + androidx.test.InstrumentationRegistry.getTargetContext().packageName
                        + " android.permission.RECORD_AUDIO"
            )
        }
    }

    @Test
    fun all() {
        TutorialNextTest().tutorialNextTest()
        TutorialSkipOneTimeTest().tutorialSkipOneTimeTest()
        TutorialSkipAlwaysTest().tutorialSkipAlwaysTest()
        SummaryAndInfoTest().summaryAndInfoTest()
        RecordingCanceledTest().recordingCanceledTest()
        RecordingSavedTest().recordingSavedTest()
        MapsTest().mapsTest()
        AboutTest().aboutTest()
        SettingsTest().settingsTest()
    }

    private fun childAtPosition(
        parentMatcher: Matcher<View>, position: Int
    ): Matcher<View> {

        return object : TypeSafeMatcher<View>() {
            override fun describeTo(description: Description) {
                description.appendText("Child at position $position in parent ")
                parentMatcher.describeTo(description)
            }

            public override fun matchesSafely(view: View): Boolean {
                val parent = view.parent
                return parent is ViewGroup && parentMatcher.matches(parent)
                        && view == parent.getChildAt(position)
            }
        }
    }
}
