package cat.urv.cloudlab.soundless.view.activity


import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.navigation.NavDeepLinkBuilder
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import androidx.test.runner.AndroidJUnit4
import cat.urv.cloudlab.soundless.R
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import org.hamcrest.TypeSafeMatcher
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@LargeTest
@RunWith(AndroidJUnit4::class)
class SettingsTest {

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

    private fun pressBack() {
        onView(isRoot()).perform(ViewActions.pressBack())
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
    fun settingsTest() {
        launchFragment(R.id.nav_settings)

        val switchMaterial = onView(
            allOf(
                withId(R.id.switch_tutorial_active), withText(R.string.switch_tutorial_active),
                childAtPosition(
                    allOf(
                        withId(R.id.switch_graphs),
                        childAtPosition(
                            withId(R.id.nav_host_fragment),
                            0
                        )
                    ),
                    0
                ),
                isDisplayed()
            )
        )
        switchMaterial.perform(click())

        val switchMaterial2 = onView(
            allOf(
                withId(R.id.switch_tutorial_active), withText(R.string.switch_tutorial_active),
                childAtPosition(
                    allOf(
                        withId(R.id.switch_graphs),
                        childAtPosition(
                            withId(R.id.nav_host_fragment),
                            0
                        )
                    ),
                    0
                ),
                isDisplayed()
            )
        )
        switchMaterial2.perform(click())

        val materialButton4 = onView(
            allOf(
                withId(R.id.btn_fitbit_code), withText(R.string.fitbit_button_code),
                childAtPosition(
                    allOf(
                        withId(R.id.switch_graphs),
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
        materialButton4.perform(click())

        val editText = onView(
            allOf(
                childAtPosition(
                    childAtPosition(
                        withId(androidx.appcompat.R.id.custom),
                        0
                    ),
                    0
                ),
                isDisplayed()
            )
        )
        editText.perform(
            replaceText("3fd86c68bccda0a87b82de63d70211b5b6b1c377"),
            closeSoftKeyboard()
        )

        val materialButton5 = onView(
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
        materialButton5.perform(scrollTo(), click())

        val materialButton6 = onView(
            allOf(
                withId(R.id.btn_delete_recordings), withText(R.string.delete_recordings_btn),
                childAtPosition(
                    allOf(
                        withId(R.id.switch_graphs),
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
        materialButton6.perform(click())

        val materialButton7 = onView(
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
        materialButton7.perform(scrollTo(), click())

        val materialButton8 = onView(
            allOf(
                withId(R.id.btn_fitbit_code), withText(R.string.fitbit_button_code),
                childAtPosition(
                    allOf(
                        withId(R.id.switch_graphs),
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
        materialButton8.perform(click())

        val materialButton9 = onView(
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
        materialButton9.perform(scrollTo(), click())

        val materialButton10 = onView(
            allOf(
                withId(R.id.btn_delete_recordings), withText(R.string.delete_recordings_btn),
                childAtPosition(
                    allOf(
                        withId(R.id.switch_graphs),
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
        materialButton10.perform(click())

        val materialButton11 = onView(
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
        materialButton11.perform(scrollTo(), click())

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
