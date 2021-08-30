import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.runner.RunWith
import org.opencv.android.OpenCVLoader

/**
 * A simple test that initializes OpenCv Jni.
 * All image processing tests can extend this test to automatically setup OpenCV.
 */
@RunWith(AndroidJUnit4::class)
open class OpenCvTest {

    @Before
    fun setup() { OpenCVLoader.initDebug() }
}