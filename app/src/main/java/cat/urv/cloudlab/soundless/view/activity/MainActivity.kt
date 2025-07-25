package cat.urv.cloudlab.soundless.view.activity

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.updatePadding
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import cat.urv.cloudlab.soundless.R
import cat.urv.cloudlab.soundless.databinding.ActivityMainBinding
import cat.urv.cloudlab.soundless.util.DeviceLocation
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.mikepenz.iconics.typeface.library.googlematerial.GoogleMaterial
import com.mikepenz.materialdrawer.iconics.iconicsIcon
import com.mikepenz.materialdrawer.model.*
import com.mikepenz.materialdrawer.model.interfaces.descriptionText
import com.mikepenz.materialdrawer.model.interfaces.iconRes
import com.mikepenz.materialdrawer.model.interfaces.nameRes
import com.mikepenz.materialdrawer.model.interfaces.nameText
import com.mikepenz.materialdrawer.widget.AccountHeaderView
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit


@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    // Firebase variables
    private lateinit var auth: FirebaseAuth

    // View Binding
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_Soundless)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Add this block to handle Window Insets
        ViewCompat.setOnApplyWindowInsetsListener(binding.toolbarBinding.toolbar) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Apply the top inset as padding to the toolbar
            view.updatePadding(top = insets.top)
            // Return a default value
            WindowInsetsCompat.CONSUMED
        }

        // Navigation Component
        val navController = getNavController()

        // UI config
        with(binding) {
            setSupportActionBar(toolbarBinding.toolbar)

            // Drawer customization
            slider.setSelectionAtPosition(-1)

            AccountHeaderView(this@MainActivity).apply {
                attachToSliderView(slider)
                addProfiles(
                    ProfileDrawerItem().apply {
                        nameText = getString(R.string.anonymous_user)
                        descriptionText = getString(R.string.anonymous_description)
                        iconRes = R.drawable.pic_profile
                        identifier = 10
                    }
                )
                selectionListEnabledForSingleProfile = false
                profileImagesClickable = false
                withSavedInstance(savedInstanceState)
            }

            val itemMain = PrimaryDrawerItem().apply {
                nameRes = R.string.drawer_item_home
                descriptionText = getString(R.string.drawer_item_home_desc)
                identifier = 0
                iconicsIcon = GoogleMaterial.Icon.gmd_graphic_eq
            }
            val itemSummary = PrimaryDrawerItem().apply {
                nameRes = R.string.drawer_item_reports
                descriptionText = getString(R.string.drawer_item_reports_desc)
                identifier = 1
                iconicsIcon = GoogleMaterial.Icon.gmd_book
            }
            val itemSensitivity = PrimaryDrawerItem().apply {
                nameRes = R.string.drawer_item_sensitivity
                descriptionText = getString(R.string.drawer_item_sensitivity_desc)
                identifier = 2
                iconicsIcon = GoogleMaterial.Icon.gmd_hearing
            }
            /*val itemMap = ExpandableBadgeDrawerItem().apply {
                nameRes = R.string.drawer_item_map
                descriptionText = getString(R.string.drawer_item_map_desc)
                iconicsIcon = GoogleMaterial.Icon.gmd_map
                identifier = 2
                isSelectable = false

                subItems = mutableListOf(
                    SecondaryDrawerItem().apply {
                        nameRes = R.string.tarragona_city
                        level = 2
                        iconicsIcon = GoogleMaterial.Icon.gmd_location_on
                        identifier = 3
                    },
                    SecondaryDrawerItem().apply {
                        nameRes = R.string.reus_city
                        level = 2
                        iconicsIcon = GoogleMaterial.Icon.gmd_location_on
                        identifier = 4
                    },
                    SecondaryDrawerItem().apply {
                        nameRes = R.string.tarragona_province
                        level = 2
                        iconicsIcon = GoogleMaterial.Icon.gmd_location_on
                        identifier = 5
                    }
                )
            }*/
            val itemSettings = PrimaryDrawerItem().apply {
                nameRes = R.string.drawer_item_settings
                descriptionText = getString(R.string.drawer_item_settings_desc)
                identifier = 3
                iconicsIcon = GoogleMaterial.Icon.gmd_settings
            }
            val itemAbout = PrimaryDrawerItem().apply {
                nameRes = R.string.drawer_item_about
                descriptionText = getString(R.string.drawer_item_about_desc)
                identifier = 4
                iconicsIcon = GoogleMaterial.Icon.gmd_info
            }

            slider.itemAdapter.add(
                itemMain,
                itemSummary,
                itemSensitivity,
                DividerDrawerItem(),
                itemSettings,
                itemAbout,
            )

            slider.onDrawerItemClickListener = { _, drawerItem, _ ->
                when (// Navigation without actions when it comes to Drawer fragments
                    drawerItem.identifier) {
                    0L -> navController.navigate(R.id.nav_main)
                    1L -> navController.navigate(R.id.nav_reports)
                    2L -> navController.navigate(R.id.nav_sensitivity)
                    3L -> navController.navigate(R.id.nav_settings)
                    4L -> navController.navigate(R.id.nav_about)
                }
                drawerLayout.closeDrawer(GravityCompat.START)

                slider.setSelectionAtPosition(-1)
                true
            }
            slider.recyclerView.id = R.id.recycledViewMenu
        }

        // Initialize Firebase auth
        auth = Firebase.auth
    }

    override fun onStart() {
        super.onStart()

        // Check that user is Firebase authenticated
        var currentUser = auth.currentUser
        if (currentUser == null) {
            auth.signInAnonymously()
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) currentUser = auth.currentUser
                }
        }

        // Show Telegram popup if needed
        val sharedPreferences = getSharedPreferences(
            getString(R.string.shared_preferences_filename),
            Context.MODE_PRIVATE
        )
        if (mustShowPopup(sharedPreferences)) {
            incrementNumTimesPopupShown(sharedPreferences)
            updateDatePopupShown(sharedPreferences)
            resetDaysLeftToShowPopup(sharedPreferences)
            showPopup()
        } else {
            decrementDaysLeftToShowPopup(sharedPreferences)
        }
    }

    override fun onPause() {
        super.onPause()
        binding.drawerLayout.closeDrawer(GravityCompat.START)
    }

    override fun onDestroy() {
        super.onDestroy()

        // When application is closed, delete Location to prevent data being too old
        val sharedPreferences = getSharedPreferences(
            DeviceLocation.SHARED_PREFERENCES_FILENAME,
            Context.MODE_PRIVATE
        )
        val editor = sharedPreferences.edit()
        editor.remove("lat")
        editor.remove("lng")
        editor.apply()
    }

    private fun updateDatePopupShown(sharedPreferences: SharedPreferences) {
        sharedPreferences.edit()
            .putString(getString(R.string.key_pop_up_date), getTodayDate())
            .apply()
    }

    private fun decrementDaysLeftToShowPopup(sharedPreferences: SharedPreferences) {
        val daysLeftToShowPopup = sharedPreferences.getInt(getString(R.string.key_pop_up_days_left_counter), 3)
        if (daysLeftToShowPopup != 0){
            sharedPreferences.edit().putInt(getString(R.string.key_pop_up_days_left_counter), daysLeftToShowPopup - 1).apply()
        }
    }

    private fun resetDaysLeftToShowPopup(sharedPreferences: SharedPreferences) {
        sharedPreferences.edit().putInt(getString(R.string.key_pop_up_days_left_counter), 3).apply()
    }

    private fun mustShowPopup(sharedPreferences: SharedPreferences): Boolean {
        val numTimesPopupShown = sharedPreferences.getInt(getString(R.string.key_num_times_popup_shown), 0)
        if (numTimesPopupShown > 2)
            return false

        val daysLeftToShowPopup = sharedPreferences.getInt(getString(R.string.key_pop_up_days_left_counter), 3)
        val datePopUp = sharedPreferences.getString(getString(R.string.key_pop_up_date), "")

        if (datePopUp == "")
            updateDatePopupShown(sharedPreferences)

        if (daysLeftToShowPopup <= 0 && daysHavePassedSince(datePopUp!!) >= 3)
            return true

        return false
    }

    private fun incrementNumTimesPopupShown(sharedPreferences: SharedPreferences) {
        val numTimes = sharedPreferences.getInt(getString(R.string.key_num_times_popup_shown), 0)
        sharedPreferences.edit().putInt(getString(R.string.key_num_times_popup_shown), numTimes + 1).apply()
    }

    private fun showPopup() {
        val navController = getNavController()
        navController.navigate(R.id.nav_pop_up_telegram)
    }

    private fun getTodayDate() : String{
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-d")
        return LocalDateTime.now().format(formatter).toString()
    }

    private fun daysHavePassedSince(initialDateString: String) : Int{
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-d")

        return try {
            val initialDate: LocalDate = LocalDate.parse(initialDateString, formatter)
            val todayDate : LocalDate =  LocalDate.now()

            ChronoUnit.DAYS.between(initialDate, todayDate).toInt()
        } catch (e: DateTimeParseException) {
            999
        }
    }

    private fun getNavController(): NavController {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        return navHostFragment.navController
    }

    /**
     * Set up toolbar buttons, e.g. the hamburger menu to open/close drawer.
     */
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.menu_items, menu)
        return true
    }

    /**
     * Set up toolbar callbacks, e.g. the hamburger menu to open/close drawer.
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.hamburger -> {
                binding.drawerLayout.openDrawer(GravityCompat.START)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * Set toolbar visibility to 'gone'. Public method to make it callable from Fragments.
     */
    fun hideToolbar() {
        binding.toolbarBinding.toolbar.visibility = View.GONE
    }

    /**
     * Set toolbar visibility to 'visible'. Public method to make it callable from Fragments.
     */
    fun showToolbar() {
        binding.toolbarBinding.toolbar.visibility = View.VISIBLE
    }

}