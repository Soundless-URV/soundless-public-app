package cat.urv.cloudlab.soundless.view.activity


import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.navigation.NavDeepLinkBuilder
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.test.InstrumentationRegistry.getTargetContext
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
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
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


@LargeTest
@RunWith(AndroidJUnit4::class)
class SummaryAndInfoTest {

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

    private fun waitId(millis: Long): ViewAction {
        return object : ViewAction {
            override fun getConstraints(): Matcher<View> {
                return isRoot()
            }

            override fun getDescription(): String {
                return "wait for a specific view during $millis millis."
            }

            override fun perform(uiController: UiController, view: View) {
                uiController.loopMainThreadUntilIdle()
                val startTime = System.currentTimeMillis()
                val endTime = startTime + millis
                do {
                    uiController.loopMainThreadForAtLeast(50)
                } while (System.currentTimeMillis() < endTime)
            }
        }
    }

    private fun getDate(): String {
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
        return LocalDateTime.now().format(formatter).toString()
    }

    private fun pressBack() {
        onView(isRoot()).perform(ViewActions.pressBack())
    }

    @Before
    fun grantPhonePermission() {
        // In M+, trying to call a number will trigger a runtime dialog. Make sure
        // the permission is granted before running this test.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getInstrumentation().uiAutomation.executeShellCommand(
                "pm grant " + getTargetContext().packageName
                        + " android.permission.ACCESS_FINE_LOCATION"
            )
            getInstrumentation().uiAutomation.executeShellCommand(
                "pm grant " + getTargetContext().packageName
                        + " android.permission.ACCESS_COARSE_LOCATION"
            )
            getInstrumentation().uiAutomation.executeShellCommand(
                "pm grant " + getTargetContext().packageName
                        + " android.permission.RECORD_AUDIO"
            )
        }
    }


    @Test
    fun summaryAndInfoTest() {
        launchFragment(R.id.nav_main)

        val floatingActionButton = onView(
            allOf(
                withId(R.id.recordButton), withContentDescription(R.string.start_and_stop_recording),
                childAtPosition(
                    allOf(
                        withId(R.id.parentConstraintLayout),
                        childAtPosition(
                            withId(R.id.nav_host_fragment),
                            0
                        )
                    ),
                    4
                ),
                isDisplayed()
            )
        )
        floatingActionButton.perform(click())

        onView(isRoot()).perform(waitId(5000))

        val floatingActionButton2 = onView(
            allOf(
                withId(R.id.recordButton), withContentDescription(R.string.start_and_stop_recording),
                childAtPosition(
                    allOf(
                        withId(R.id.parentConstraintLayout),
                        childAtPosition(
                            withId(R.id.nav_host_fragment),
                            0
                        )
                    ),
                    4
                ),
                isDisplayed()
            )
        )
        floatingActionButton2.perform(click())

        val date = getDate()

        val actionMenuItemView = onView(
            allOf(
                withId(R.id.hamburger), withContentDescription(R.string.hamburger),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.toolbar),
                        1
                    ),
                    0
                ),
                isDisplayed()
            )
        )
        actionMenuItemView.perform(click())

        val recyclerView = onView(
            allOf(
                withId(R.id.recycledViewMenu),
                childAtPosition(
                    withId(R.id.slider),
                    0
                )
            )
        )
        recyclerView.perform(actionOnItemAtPosition<ViewHolder>(2, click()))

        val floatingActionButton3 = onView(
            allOf(
                withId(R.id.btn_correlate_health),
                withContentDescription(R.string.correlate_health),
                childAtPosition(
                    allOf(
                        withId(R.id.parentConstraintLayout),
                        childAtPosition(
                            withId(R.id.nav_host_fragment),
                            0
                        )
                    ),
                    3
                ),
                isDisplayed()
            )
        )
        floatingActionButton3.perform(click())

        val materialButton3 = onView(
            allOf(
                withId(R.id.filter_date_btn), withText(R.string.filter_date_btn_text),
                childAtPosition(
                    allOf(
                        withId(R.id.filter_linear_layout),
                        childAtPosition(
                            withId(R.id.filter_scroll),
                            0
                        )
                    ),
                    1
                )
            )
        )
        materialButton3.perform(scrollTo(), click())

        val materialButton4 = onView(
            allOf(
                withId(android.R.id.button1), withText(android.R.string.ok),
                childAtPosition(
                    childAtPosition(
                        withClassName(`is`("android.widget.ScrollView")),
                        0
                    ),
                    3
                )
            )
        )
        materialButton4.perform(scrollTo(), click())

        val materialButton5 = onView(
            allOf(
                withId(android.R.id.button1), withText(android.R.string.ok),
                childAtPosition(
                    childAtPosition(
                        withClassName(`is`("android.widget.ScrollView")),
                        0
                    ),
                    3
                )
            )
        )
        materialButton5.perform(scrollTo(), click())

        val materialTextView = onView(
            allOf(
                withId(R.id.text_row_item), withText(date),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.row),
                        1
                    ),
                    0
                ),
                isDisplayed()
            )
        )
        materialTextView.perform(click())

        val floatingActionButton4 = onView(
            allOf(
                withId(R.id.deleteRecordingButton), withContentDescription(R.string.delete_recordings_btn),
                childAtPosition(
                    allOf(
                        withId(R.id.switch_graphs),
                        childAtPosition(
                            withId(R.id.nav_host_fragment),
                            0
                        )
                    ),
                    2
                ),
                isDisplayed()
            )
        )
        floatingActionButton4.perform(click())

        val materialButton9 = onView(
            allOf(
                withId(android.R.id.button2), withText(R.string.delete_all_data_cancel),
                childAtPosition(
                    childAtPosition(
                        withId(androidx.appcompat.R.id.buttonPanel),
                        0
                    ),
                    2
                )
            )
        )
        materialButton9.perform(scrollTo(), click())

        val floatingActionButton5 = onView(
            allOf(
                withId(R.id.deleteRecordingButton), withContentDescription(R.string.delete_recordings_btn),
                childAtPosition(
                    allOf(
                        withId(R.id.switch_graphs),
                        childAtPosition(
                            withId(R.id.nav_host_fragment),
                            0
                        )
                    ),
                    2
                ),
                isDisplayed()
            )
        )
        floatingActionButton5.perform(click())

        val materialButton10 = onView(
            allOf(
                withId(android.R.id.button1), withText(R.string.delete_recordings_confirm),
                childAtPosition(
                    childAtPosition(
                        withId(androidx.appcompat.R.id.buttonPanel),
                        0
                    ),
                    3
                )
            )
        )
        materialButton10.perform(scrollTo(), click())

        pressBack()
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
