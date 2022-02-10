import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.merge
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.wheatgenetics.imageprocess.PotatoDetector
import org.wheatgenetics.onekk.toFile
import org.wheatgenetics.utils.DateUtil
import java.io.File
import java.io.IOException
import kotlin.math.abs
import kotlin.math.pow


/***
 * A sweet potatoes dataset is provided in assets/datasets/potatoes
 * It is a dataset of ~270 single sample jpegs.
 * Make sure the aaptOptions (in app build) that deletes the samples folder is disabled if you're running tests.
 *
 * It is possible to run this test on an emulator, but you need to copy x86 jni libs from the allJni assets folder to the java/jni folder.
 * assets/datasets/allJni folder is a backup of all the possible Opencv Jni in case they are ever needed.
 */
@RunWith(AndroidJUnit4::class)
class SweetPotatoUnitTest : OpenCvTest() {

    private data class Residual(val actual: Double = 0.0, val model: Double)
    private data class PotatoVector(val length: Residual, val width: Residual)

    //instrumentation context
    private lateinit var iContext: Context

    //heap objects for storing baseline data and picture to merge id mapping (see sweet potato csvs')
    private lateinit var mPictureToIdMap: HashMap<String, String>
    private lateinit var mBaselineMap: HashMap<String, PotatoVector>
    private lateinit var mGeneratedMap: HashMap<String, PotatoVector>

    //before any test is run, start an instrumentation context (this test requires a connected device!)
    @Before
    fun establishContext() {
        iContext = ApplicationProvider.getApplicationContext()
    }

    @Before
    fun importBaselineMapping() {

        assert(::iContext.isInitialized)

        try {
            //load pic names to id (which can be string) csv
            mPictureToIdMap = HashMap()
            mBaselineMap = HashMap()

            //load lines into memory and drop the header
            iContext.assets.open("datasets/sweet_potatoes_onekk.csv").bufferedReader()
                .lines().toArray().drop(1)
                .map {
                    it.toString().split(",")
                }
                .forEach { row ->
                    //associate pic names to merge ids
                    mPictureToIdMap[row[0]] = row[3]
                }

            //read actual and baseline data from merged csv
            val potatoes = iContext.assets.open("datasets/sweet_potato_data_20210303.csv").bufferedReader()
                .lines().toArray().drop(1)
                .map {
                    val tokens = it.toString().split(",")
                    val potato = PotatoVector(
                        length = Residual(
                            actual = tokens[2].toDouble(),
                            model = tokens[9].toDouble()
                        ),
                        width = Residual(
                            actual = tokens[4].toDouble(),
                            model = tokens[10].toDouble()
                        )
                    )

                    //add potato to baseline hash map
                    val mergeId = tokens[8]
                    Log.d("SweetPotatoTest", "${potato.length} ${potato.width}")
                    mBaselineMap[mergeId] = potato

                    potato
                }

            assert(potatoes.size == 268)

            val lData = potatoes.map { it.length.actual }.toTypedArray()
            val lfData = potatoes.map { it.length.model }.toTypedArray()
            val wData = potatoes.map { it.width.actual }.toTypedArray()
            val wfData = potatoes.map { it.width.model }.toTypedArray()

            val lengthCorr = PearsonsCorrelation().correlation(lData.toDoubleArray(), lfData.toDoubleArray())
            val widthCorr = PearsonsCorrelation().correlation(wData.toDoubleArray(), wfData.toDoubleArray())

            //val lr2 = lengthCorr.pow(2)
            //val wr2 = widthCorr.pow(2)

            Log.d("SweetPotatoTest", "Correlation length: $lengthCorr width: $widthCorr")

        } catch (io: IOException) {

            io.printStackTrace()

        }

    }

    /**
     * This test processes the LSS algorithm on all images in assets/datasets/potatoes.
     * The output is saved in /storage/Android/media/org...onekk/sweet_potatoes_onekk.csv
     */
    @Test
    fun exportPotatoContours() {

        assert(::iContext.isInitialized)
        assert(::mBaselineMap.isInitialized)
        assert(::mPictureToIdMap.isInitialized)

        mGeneratedMap = HashMap()

        try {  //catch any io exceptions

            val imageDirectory = "datasets/potatoes"

            //gets instrumentation context
            with(ApplicationProvider.getApplicationContext<Context>()) {

                val detector = PotatoDetector(applicationContext, 24.26, "2")

                //creates buffered writer for the output csv file in the external media directory
                //be careful of using filesDir here (has size limits of ~3.5Kb)
                File(externalMediaDirs[0], "sweet_potatoes_onekk.csv").bufferedWriter().use { out ->

                    //iterate over all images in the dataset
                    assets.list(imageDirectory)?.forEach { imageName ->

//                        val toc = System.nanoTime()
                        if (imageName in mPictureToIdMap) {
                            //decode image from an asset input stream
                            val imagePath = "$imageDirectory/$imageName"
                            val bmp = BitmapFactory.decodeStream(assets.open(imagePath))

                            //process image expecting a quarter as reference (24.26mm diameter)
                            val result = detector.process(bmp)

                            result.dst.toFile("${externalMediaDirs[0]}/test", name = "${imageName}.png")

                            //finally print out contour information to the csv
                            result.contours.forEach { detection ->

                                val mergeId = mPictureToIdMap[imageName].toString()
                                val baseline = mBaselineMap[mergeId]

                                val actualLength = baseline?.length?.actual ?: 0.0
                                val actualWidth = baseline?.width?.actual ?: 0.0
                                val potato = PotatoVector(
                                    length = Residual(
                                        model = detection.maxAxis ?: 0.0,
                                        actual = actualLength
                                    ),
                                    width = Residual(
                                        model = detection.minAxis ?: 0.0,
                                        actual = actualWidth
                                    )
                                )

                                mGeneratedMap[mergeId] = potato

                                out.write("$imageName,${detection}")

                                out.newLine()
                            }

//                            val tic = System.nanoTime()
//                            val time = (tic-toc)*1e9
//                            out.write("$imageName, $time")
//                            out.newLine()
                        }
                    }
                }

                val lData = mGeneratedMap.values.map { it.length.model }.toTypedArray()
                val lfData = mGeneratedMap.values.map { it.length.actual }.toTypedArray()
                val wData = mGeneratedMap.values.map { it.width.model }.toTypedArray()
                val wfData = mGeneratedMap.values.map { it.width.actual }.toTypedArray()

                val lengthCorr = PearsonsCorrelation().correlation(lData.toDoubleArray(), lfData.toDoubleArray())
                val widthCorr = PearsonsCorrelation().correlation(wData.toDoubleArray(), wfData.toDoubleArray())

                //val lr2 = lengthCorr.pow(2)
                //val wr2 = widthCorr.pow(2)

                Log.d("SweetPotatoTest", "Correlation length: $lengthCorr width: $widthCorr")
            }

        } catch (io: IOException) {

            io.printStackTrace()

        } catch (e: Exception) {

            e.printStackTrace()

        }
    }
}