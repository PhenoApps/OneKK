package org.wheatgenetics.onekk.activities

import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.databinding.DataBindingUtil
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.preference.PreferenceManager
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import org.opencv.android.BaseLoaderCallback
import org.opencv.android.LoaderCallbackInterface
import org.opencv.android.OpenCVLoader
import org.wheatgenetics.onekk.BuildConfig
import org.wheatgenetics.onekk.R
import org.wheatgenetics.onekk.databinding.ActivityMainBinding
import org.wheatgenetics.utils.SnackbarQueue
import java.io.File

typealias LumaListener = (luma: Double) -> Unit

class MainActivity : AppCompatActivity(), CoroutineScope by MainScope() {

//    private val mFirebaseAnalytics by lazy {
//        FirebaseAnalytics.getInstance(this)
//    }

    private val mLoaderCallback: BaseLoaderCallback = object : BaseLoaderCallback(this) {

        override fun onManagerConnected(status: Int) {

            when (status) {

                LoaderCallbackInterface.SUCCESS -> {

                    Log.i("OpenCV", "OpenCV loaded successfully")

                }

                else -> {

                    super.onManagerConnected(status)

                }
            }
        }
    }

    override fun onResume() {

        super.onResume()

        if (!OpenCVLoader.initDebug()) {

            Log.d("OpenCV", "Internal OpenCV library not found. Using OpenCV Manager for initialization")

            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0, this, mLoaderCallback)

        } else {

            Log.d("OpenCV", "OpenCV library found inside package. Using it!")

            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS)

        }
    }

    private var doubleBackToExitPressedOnce = false

    private lateinit var mSnackbar: SnackbarQueue

    private lateinit var mDrawerLayout: DrawerLayout

    private lateinit var mDrawerToggle: ActionBarDrawerToggle

    private lateinit var mBinding: ActivityMainBinding

    private lateinit var mNavController: NavController

    private val permissionCheck by lazy {


        (this as ComponentActivity).registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {

            setupActivity()

        }
    }

    private fun writeStream(file: File, resourceId: Int) {

        if (!file.isFile) {

            val stream = resources.openRawResource(resourceId)

            file.writeBytes(stream.readBytes())

            stream.close()
        }

    }

    private fun setupDirs() {

        //create separate subdirectory foreach type of import
        val scans = File(this.externalCacheDir, "Images")

        scans.mkdir()

    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        if ("release" in BuildConfig.BUILD_TYPE) {

            //TODO add firebase analytics event on error

            Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->

                Log.e("OneKKCrash", throwable.message)

                throwable.printStackTrace()

            }
        }

        setupActivity()

    }

    private fun setupActivity() {

        setupDirs()

        mBinding = DataBindingUtil.setContentView(this@MainActivity, R.layout.activity_main)

        mSnackbar = SnackbarQueue()

        setupNavDrawer()

        setupNavController()

        supportActionBar.apply {
            title = ""
            this?.let {
                it.themedContext
                setDisplayHomeAsUpEnabled(true)
                setHomeButtonEnabled(true)
            }
        }

    }

    private fun setupNavController() {

        mNavController = Navigation.findNavController(this@MainActivity, R.id.nav_fragment)

        mNavController.addOnDestinationChangedListener { _, destination, _ ->

            when (destination.id) {

                R.id.camera_preview_fragment -> {

                    mDrawerToggle.isDrawerIndicatorEnabled = true

                    mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)

                }
                else -> {

                    mDrawerToggle.isDrawerIndicatorEnabled = false

                    mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)

                }
            }
        }

    }

    private fun setupNavDrawer() {

        mDrawerLayout = mBinding.drawerLayout

        mDrawerToggle = object : ActionBarDrawerToggle(this, mDrawerLayout, R.string.drawer_open, R.string.drawer_close) {

            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {

                super.onDrawerSlide(drawerView, slideOffset)

                closeKeyboard()
            }
        }

        mDrawerToggle.isDrawerIndicatorEnabled = true

        mDrawerLayout.addDrawerListener(mDrawerToggle)

        // Setup drawer view
        val nvDrawer = findViewById<NavigationView>(R.id.nvView)

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)

//        nvDrawer.getHeaderView(0).apply {
//            findViewById<TextView>(R.id.navHeaderText)
//                    .text = prefs.getString(OPERATOR, "")
//        }

        nvDrawer.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {

//                R.id.action_nav_export -> {
//
//                    askExport()
//
//                }
//                R.id.action_nav_settings -> {
//
//                    mNavController.navigate(ExperimentListFragmentDirections.actionToSettings())
//                }
//                R.id.action_nav_about -> {
//
//                    //mNavController.navigate(ExperimentListFragmentDirections.actionToAbout())
//
//                }
            }

            mDrawerLayout.closeDrawers()

            true
        }
    }

    private fun closeKeyboard() {

        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager

        imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        val dl = findViewById<DrawerLayout>(R.id.drawer_layout)

        closeKeyboard()

        if (mDrawerToggle.onOptionsItemSelected(item)) {

            return true
        }

        when (item.itemId) {

            android.R.id.home -> {

                mNavController.currentDestination?.let {

                    when (it.id) {

                        R.id.camera_preview_fragment -> {

                            dl.openDrawer(GravityCompat.START)

                        }

                        //go back to the last fragment instead of opening the navigation drawer
                        else -> mNavController.popBackStack()
                    }
                }
            }
            else -> return super.onOptionsItemSelected(item)
        }

        return true
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)

        if (::mDrawerToggle.isInitialized) {

            mDrawerToggle.syncState()

        }

    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        mDrawerToggle.onConfigurationChanged(newConfig)
    }

    /**
     * Uses nav controller to change what the back press does depending on the fragment id.
     */
    override fun onBackPressed() {

        mNavController.currentDestination?.let { it ->

            when (it.id) {

                //go back to the last fragment instead of opening the navigation drawer
                R.id.camera_preview_fragment -> {

                    if (doubleBackToExitPressedOnce) {

                        super.onBackPressed();

                        return
                    }

                    this.doubleBackToExitPressedOnce = true;

                    Toast.makeText(this, getString(R.string.double_back_press), Toast.LENGTH_SHORT).show();

                    Handler().postDelayed({ doubleBackToExitPressedOnce = false }, 2000)
                }

                else -> super.onBackPressed()
            }
        }
    }
}
