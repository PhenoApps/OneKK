import android.content.Context
import android.graphics.BitmapFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import org.wheatgenetics.imageprocess.PotatoDetector
import java.io.File
import java.io.IOException

/***
 * A sweet potatoes dataset is provided in assets/samples/potatoes
 * It is a dataset of ~270 single sample jpegs.
 * Make sure the aaptOptions (in app build) that deletes the samples folder is disabled if you're running tests.
 */
@RunWith(AndroidJUnit4::class)
class SweetPotatoUnitTest : OpenCvTest() {

    /**
     * This test processes the LSS algorithm on all images in assets/samples/potatoes.
     * The output is saved in /storage/Android/media/org...onekk/sweet_potatoes_onekk.csv
     */
    @Test
    fun exportPotatoContours() {

        try {  //catch any io exceptions

            val imageDirectory = "samples/potatoes"

            //gets instrumentation context
            with(ApplicationProvider.getApplicationContext<Context>()) {

                //creates buffered writer for the output csv file in the external media directory
                //be careful of using filesDir here (has size limits of ~3.5Kb)
                File(externalMediaDirs[0], "sweet_potatoes_onekk.csv").bufferedWriter().use { out ->

                    //iterate over all images in the dataset
                    assets.list(imageDirectory)?.forEach { imageName ->

                        //decode image from an asset input stream
                        val imagePath = "$imageDirectory/$imageName"
                        val bmp = BitmapFactory.decodeStream(assets.open(imagePath))

                        //process image expecting a quarter as reference (24.26mm diameter)
                        val detector = PotatoDetector(24.26)
                        val result = detector.process(bmp)

                        //finally print out contour information to the csv
                        result.contours.forEach { detection ->

                            out.write("$imageName,${detection}")

                            out.newLine()
                        }
                    }
                }
            }

        } catch (io: IOException) {

            io.printStackTrace()

        } catch (e: Exception) {

            e.printStackTrace()

        }
    }
}