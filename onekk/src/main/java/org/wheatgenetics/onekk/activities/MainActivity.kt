package org.wheatgenetics.onekk.activities

import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.navigation.NavController
import androidx.navigation.Navigation
import kotlinx.coroutines.*
import org.opencv.android.BaseLoaderCallback
import org.opencv.android.LoaderCallbackInterface
import org.opencv.android.OpenCVLoader
import org.wheatgenetics.onekk.BuildConfig
import org.wheatgenetics.onekk.R
import org.wheatgenetics.onekk.database.OnekkDatabase
import org.wheatgenetics.onekk.database.OnekkRepository
import org.wheatgenetics.onekk.database.viewmodels.ExperimentViewModel
import org.wheatgenetics.onekk.database.viewmodels.factory.OnekkViewModelFactory
import org.wheatgenetics.onekk.databinding.ActivityMainBinding
import org.wheatgenetics.onekk.fragments.CameraFragmentDirections
import org.wheatgenetics.onekk.observeOnce
import org.wheatgenetics.utils.SnackbarQueue
import java.io.File

/**
 * Basic main activity class that holds the navigation drawer view and all other fragments.
 */
class MainActivity : AppCompatActivity(), CoroutineScope by MainScope() {

//    private val mFirebaseAnalytics by lazy {
//        FirebaseAnalytics.getInstance(this)
//    }

    private val db by lazy {
        OnekkDatabase.getInstance(this)
    }

    private val viewModel by viewModels<ExperimentViewModel> {
        OnekkViewModelFactory(OnekkRepository.getInstance(db.dao(), db.coinDao()))
    }

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

    private lateinit var mBinding: ActivityMainBinding

    private lateinit var mNavController: NavController

//    private val permissionCheck by lazy {
//
//        (this as ComponentActivity).registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
//
//            setupActivity()
//
//        }
//    }
//
//    private fun writeStream(file: File, resourceId: Int) {
//
//        if (!file.isFile) {
//
//            val stream = resources.openRawResource(resourceId)
//
//            file.writeBytes(stream.readBytes())
//
//            stream.close()
//        }
//
//    }

    private fun setupDirs() {

        try {

            val directory = this.externalMediaDirs[0]

            //create separate subdirectory foreach type of import
            val scans = File(directory, "Images")

            scans.mkdir()

        } catch (iob: ArrayIndexOutOfBoundsException) {

            iob.printStackTrace()

        }
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

        viewModel.coins().observeOnce(this, {

            it?.let {

                if (it.isEmpty()) {

                    launch(Dispatchers.IO) {

                        viewModel.loadCoinDatabase(assets.open("coin_database.csv")).await()

                    }
                }
            }

        })
    }

    private fun setupActivity() {

        setupDirs()

        mBinding = DataBindingUtil.setContentView(this@MainActivity, R.layout.activity_main)

        mSnackbar = SnackbarQueue()

        setupNavDrawer()

        setupNavController()

        supportActionBar.apply {

            this?.hide()

            title = ""
//            this?.let {
//                it.themedContext
//                setDisplayHomeAsUpEnabled(true)
//                setHomeButtonEnabled(true)
//            }
        }

    }

    private fun setupNavController() {

        mNavController = Navigation.findNavController(this@MainActivity, R.id.nav_fragment)

        mNavController.addOnDestinationChangedListener { _, destination, _ ->

            when (destination.id) {

                R.id.camera_preview_fragment, R.id.settings_fragment -> {

                    supportActionBar?.hide()
                }
                else -> {

                    supportActionBar?.show()
                }
            }
        }
    }

    private fun setupNavDrawer() {

        val botNavView = mBinding.bottomNavView

        botNavView.inflateMenu(R.menu.menu_bot_nav)

        botNavView.setOnNavigationItemSelectedListener { menuItem ->

            when (menuItem.itemId) {

                R.id.action_to_cam -> {

                    mNavController.navigate(CameraFragmentDirections.globalActionToImport(mode = "default"))
                }
                R.id.action_to_import -> {

                    mNavController.navigate(CameraFragmentDirections.globalActionToImport(mode = "import"))

                }
                R.id.action_coin_manager -> {

                    mNavController.navigate(CameraFragmentDirections.globalActionToCoinManager())

                }
                R.id.action_nav_settings -> {

                    mNavController.navigate(CameraFragmentDirections.globalActionToSettings())

                }
                R.id.action_to_analysis -> {

                    viewModel.analysis().observeOnce(this, {

                        it?.let { analyses ->

                            if (analyses.isNotEmpty()) {

                                mNavController.navigate(CameraFragmentDirections.globalActionToAnalysis())

                            } else {

                                Toast.makeText(this, R.string.frag_analysis_table_empty_message, Toast.LENGTH_SHORT).show()

                            }
                        }
                    })

                }
                R.id.action_nav_about -> {

                    mNavController.navigate(CameraFragmentDirections.globalActionToAbout())

                }
            }

            true
        }
    }

    private fun closeKeyboard() {

        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager

        imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
    }

    /**
     * Hack to update the options menu selected item by re inflating the menu
     * By default, the bottom nav bar does not update the selected item when back button is pressed.
     */
    private fun invalidateMenu() {

        launch {

            async {

                mBinding.bottomNavView.visibility = View.GONE

                mBinding.bottomNavView.menu.clear()

                mBinding.bottomNavView.inflateMenu(R.menu.menu_bot_nav)

            }.await()

            mBinding.bottomNavView.visibility = View.VISIBLE

        }
    }

    /**
     * Uses nav controller to change what the back press does depending on the fragment id.
     */
    override fun onBackPressed() {

        invalidateMenu()

        mNavController.currentDestination?.let { it ->

            when (it.id) {

                //go back to the last fragment instead of opening the navigation drawer
                R.id.camera_preview_fragment -> {

                    if (doubleBackToExitPressedOnce) {

                        super.onBackPressed()

                        return
                    }

                    this.doubleBackToExitPressedOnce = true

                    Toast.makeText(this, getString(R.string.double_back_press), Toast.LENGTH_SHORT).show()

                    Handler().postDelayed({ doubleBackToExitPressedOnce = false }, 2000)
                }

                else -> super.onBackPressed()
            }
        }
    }
}
