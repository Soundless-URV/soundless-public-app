package cat.urv.cloudlab.soundless.view.activity


import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.navigation.NavDeepLinkBuilder
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import androidx.test.runner.AndroidJUnit4
import cat.urv.cloudlab.soundless.R
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.allOf
import org.hamcrest.TypeSafeMatcher
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class MapsTest {

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

    @Before
    fun grantPhonePermission() {
        // In M+, trying to call a number will trigger a runtime dialog. Make sure
        // the permission is granted before running this test.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(
                "pm grant " + androidx.test.InstrumentationRegistry.getTargetContext().packageName
                        + " android.permission.ACCESS_FINE_LOCATION"
            )
            InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(
                "pm grant " + androidx.test.InstrumentationRegistry.getTargetContext().packageName
                        + " android.permission.ACCESS_COARSE_LOCATION"
            )
            InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(
                "pm grant " + androidx.test.InstrumentationRegistry.getTargetContext().packageName
                        + " android.permission.RECORD_AUDIO"
            )
        }
    }

    @Test
    fun mapsTest() {
        launchFragment(R.id.nav_main)

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
        recyclerView.perform(actionOnItemAtPosition<ViewHolder>(3, click()))

        val recyclerView2 = onView(
            allOf(
                withId(R.id.recycledViewMenu),
                childAtPosition(
                    withId(R.id.slider),
                    0
                )
            )
        )
        recyclerView2.perform(actionOnItemAtPosition<ViewHolder>(4, click()))

        onView(isRoot()).perform(waitId(3000))

        val materialButton3 = onView(
            allOf(
                withId(android.R.id.button1), withText(R.string.ok),
                childAtPosition(
                    childAtPosition(
                        withId(androidx.appcompat.R.id.buttonPanel),
                        0
                    ),
                    3
                )
            )
        )
        materialButton3.perform(scrollTo(), click())

        val materialButton4 = onView(
            allOf(
                withId(R.id.openAlertDialogButton), withText(R.string.select_date),
                childAtPosition(
                    allOf(
                        withId(R.id.linearLayoutMap),
                        childAtPosition(
                            withClassName(`is`("androidx.constraintlayout.widget.ConstraintLayout")),
                            0
                        )
                    ),
                    1
                ),
                isDisplayed()
            )
        )
        materialButton4.perform(click())

        val appCompatCheckedTextView1 = Espresso.onData(Matchers.anything())
            .inAdapterView(
                allOf(
                    withId(androidx.appcompat.R.id.select_dialog_listview),
                    childAtPosition(
                        withId(androidx.appcompat.R.id.contentPanel),
                        0
                    )
                )
            )
            .atPosition(1)
        appCompatCheckedTextView1.perform(click())

        onView(isRoot()).perform(waitId(3000))

        materialButton3.perform(scrollTo(), click())

        onView(isRoot()).perform(waitId(1000))

        val materialButton5 = onView(
            allOf(
                withId(R.id.openAlertDialogButton), withText(R.string.select_date),
                childAtPosition(
                    allOf(
                        withId(R.id.linearLayoutMap),
                        childAtPosition(
                            withClassName(`is`("androidx.constraintlayout.widget.ConstraintLayout")),
                            0
                        )
                    ),
                    1
                ),
                isDisplayed()
            )
        )
        materialButton5.perform(click())

        val appCompatCheckedTextView2 = Espresso.onData(Matchers.anything())
            .inAdapterView(
                allOf(
                    withId(androidx.appcompat.R.id.select_dialog_listview),
                    childAtPosition(
                        withId(androidx.appcompat.R.id.contentPanel),
                        0
                    )
                )
            )
            .atPosition(2)
        appCompatCheckedTextView2.perform(click())

        onView(isRoot()).perform(waitId(3000))

        materialButton3.perform(scrollTo(), click())

        onView(isRoot()).perform(waitId(1000))

        val materialButton6 = onView(
            allOf(
                withId(R.id.openAlertDialogButton), withText(R.string.select_date),
                childAtPosition(
                    allOf(
                        withId(R.id.linearLayoutMap),
                        childAtPosition(
                            withClassName(`is`("androidx.constraintlayout.widget.ConstraintLayout")),
                            0
                        )
                    ),
                    1
                ),
                isDisplayed()
            )
        )
        materialButton6.perform(click())

        val appCompatCheckedTextView3 = Espresso.onData(Matchers.anything())
            .inAdapterView(
                allOf(
                    withId(androidx.appcompat.R.id.select_dialog_listview),
                    childAtPosition(
                        withId(androidx.appcompat.R.id.contentPanel),
                        0
                    )
                )
            )
            .atPosition(0)
        appCompatCheckedTextView3.perform(click())

        onView(isRoot()).perform(waitId(1000))

        val actionMenuItemView2 = onView(
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
        actionMenuItemView2.perform(click())

        val recyclerView3 = onView(
            allOf(
                withId(R.id.recycledViewMenu),
                childAtPosition(
                    withId(R.id.slider),
                    0
                )
            )
        )
        recyclerView3.perform(actionOnItemAtPosition<ViewHolder>(5, click()))

        onView(isRoot()).perform(waitId(3000))

        val materialButton10 = onView(
            allOf(
                withId(android.R.id.button2), withText(R.string.cancel),
                childAtPosition(
                    childAtPosition(
                        withId(androidx.appcompat.R.id.buttonPanel),
                        0
                    ),
                    2
                )
            )
        )
        materialButton10.perform(scrollTo(), click())

        onView(isRoot()).perform(waitId(1000))

        val actionMenuItemView3 = onView(
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
        actionMenuItemView3.perform(click())

        val recyclerView4 = onView(
            allOf(
                withId(R.id.recycledViewMenu),
                childAtPosition(
                    withId(R.id.slider),
                    0
                )
            )
        )
        recyclerView4.perform(actionOnItemAtPosition<ViewHolder>(6, click()))

        onView(isRoot()).perform(waitId(3000))

        val materialButton11 = onView(
            allOf(
                withId(android.R.id.button1), withText(R.string.ok),
                childAtPosition(
                    childAtPosition(
                        withId(androidx.appcompat.R.id.buttonPanel),
                        0
                    ),
                    3
                )
            )
        )
        materialButton11.perform(scrollTo(), click())

        onView(isRoot()).perform(waitId(1000))

        val materialButton12 = onView(
            allOf(
                withId(R.id.openAlertDialogButton), withText(R.string.select_date),
                childAtPosition(
                    allOf(
                        withId(R.id.linearLayoutMap),
                        childAtPosition(
                            withClassName(`is`("androidx.constraintlayout.widget.ConstraintLayout")),
                            0
                        )
                    ),
                    1
                ),
                isDisplayed()
            )
        )
        materialButton12.perform(click())

        val appCompatCheckedTextView4 = Espresso.onData(Matchers.anything())
            .inAdapterView(
                allOf(
                    withId(androidx.appcompat.R.id.select_dialog_listview),
                    childAtPosition(
                        withId(androidx.appcompat.R.id.contentPanel),
                        0
                    )
                )
            )
            .atPosition(2)
        appCompatCheckedTextView4.perform(click())

        onView(isRoot()).perform(waitId(3000))

        materialButton3.perform(scrollTo(), click())

        onView(isRoot()).perform(waitId(1000))
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
