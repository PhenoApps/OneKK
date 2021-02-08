package org.wheatgenetics.workers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.bumptech.glide.Glide
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.wheatgenetics.imageprocess.DetectWithReferences
import org.wheatgenetics.onekk.R
import org.wheatgenetics.onekk.analyzers.Detector
import org.wheatgenetics.onekk.database.OnekkDatabase
import org.wheatgenetics.onekk.database.OnekkRepository
import org.wheatgenetics.onekk.database.models.AnalysisEntity
import org.wheatgenetics.onekk.database.models.ContourEntity
import org.wheatgenetics.onekk.database.models.ImageEntity
import org.wheatgenetics.onekk.database.models.embedded.Contour
import org.wheatgenetics.onekk.database.models.embedded.Image
import org.wheatgenetics.onekk.database.viewmodels.ExperimentViewModel
import org.wheatgenetics.onekk.database.viewmodels.factory.OnekkViewModelFactory
import org.wheatgenetics.onekk.interfaces.DetectorAlgorithm
import org.wheatgenetics.onekk.toFile
import org.wheatgenetics.utils.DateUtil
import java.io.File
import java.util.*
import kotlin.math.sqrt


class ImageSaveWorker(appContext: Context, workerParams: WorkerParameters):
        CoroutineWorker(appContext, workerParams) {

    private val db by lazy {
        OnekkDatabase.getInstance(appContext)
    }

    private val viewModel: ExperimentViewModel by lazy {
        OnekkViewModelFactory(OnekkRepository.getInstance(db.dao(), db.coinDao()))
                .create(ExperimentViewModel::class.java)
    }

    private val mPreferences by lazy {
        appContext.getSharedPreferences(appContext.getString(R.string.onekk_preference_key), Context.MODE_PRIVATE)
    }

    override suspend fun doWork(): Result {

        /***
         * example input data
         *  "algorithm" to "potato"
         *  "path" to path,
         *  "weight" to weight
            "imported" to (name == null),
            "dir" to requireContext().externalMediaDirs.first().path,
            "diameter" to diameter)
         */

        val imported = inputData.getBoolean("imported", false)

        val name = inputData.getString("name")!!

        val path = inputData.getString("path")!!

        val algorithm = inputData.getString("algorithm") ?: "0"

        //check if file was imported, this will need content resolver to load bitmap
        val bmp = withContext(CoroutineScope(Dispatchers.IO).coroutineContext) {
            if (imported) {
                BitmapFactory.decodeStream(applicationContext.contentResolver.openInputStream(Uri.parse(path)))
            } else {
                BitmapFactory.decodeFile(path)
            }
        }

        //directory to save output too
//        val dir = File(inputData.getString("dir")!!)

        val weight = inputData.getDouble("weight", 0.0)

        //user preferred coin diameter
        val diameter = inputData.getDouble("diameter", 1.0)

        val result = try {

            runDetector(bmp, algorithm, diameter)

        } catch (e: Exception) {

            e.printStackTrace()

            null

        } ?: return Result.failure()

        val output = result.dst.toFile(applicationContext.externalMediaDirs.first().path, name = "$name-${DateUtil().getTime()}.png")

        val rowid = commitToDatabase(output.path, name, weight, result)

        // Indicate whether the work finished successfully with the Result
        return Result.success(workDataOf(
                "dst" to output.path,
                "rowid" to rowid,
                "imported" to imported))
    }

    private fun runDetector(src: Bitmap, algorithm: String, diameter: Double): DetectorAlgorithm.Result? {

        return Detector(diameter, algorithm = algorithm).scan(src)
    }

    private suspend fun commitToDatabase(dst: String, name: String, weight: Double, result: DetectorAlgorithm.Result): Int {

        val collector = mPreferences?.getString(
                applicationContext.getString(R.string.onekk_preference_collector_key), "") ?: ""

        val rowid = viewModel.insert(AnalysisEntity(
                name = name,
                collector = collector,
                uri = dst,
                date = DateUtil().getTime(),
                weight = weight)).toInt()

        with(viewModel) {

            var totalCount = 0.0

            var minAxisAvg = 0.0

            var maxAxisAvg = 0.0

            var singles = 0

            var minAxisSingles = ArrayList<Double>()
            var maxAxisSingles = ArrayList<Double>()

            result.contours.forEach { contour ->

                val x = contour.x
                val y = contour.y
                val count = contour.count
                val area = contour.area
                val minAxis = contour.minAxis
                val maxAxis = contour.maxAxis

                insert(ContourEntity(
                        Contour(x, y, count, area, minAxis, maxAxis),
                        selected = true,
                        aid = rowid))

                if (count == 1) {

                    if (minAxis != null && maxAxis != null) {

                        minAxisAvg += minAxis

                        maxAxisAvg += maxAxis

                        minAxisSingles.add(minAxis)
                        maxAxisSingles.add(maxAxis)
                    }

                    singles++
                }

                totalCount += count
            }

            minAxisAvg /= singles
            maxAxisAvg /= singles

            val minAxisVar = variance(minAxisSingles, minAxisAvg, singles)
            val maxAxisVar = variance(maxAxisSingles, maxAxisAvg, singles)

            val minAxisCv = sqrt(minAxisVar) / minAxisAvg
            val maxAxisCv = sqrt(maxAxisVar) / maxAxisAvg

            updateAnalysisCount(rowid, totalCount.toInt())

            updateAnalysisData(rowid, minAxisAvg, minAxisVar, minAxisCv, maxAxisAvg, maxAxisVar, maxAxisCv)

            //val resultBitmap = result.dst.toFile(outputDirectory.path, UUID.randomUUID().toString())

            viewModel.insert(ImageEntity(Image(dst, null, DateUtil().getTime()), rowid))

        }

        return rowid
    }

    private fun variance(population: List<Double>, mean: Double, n: Int) =
            population.map { Math.pow(it - mean, 2.0) }.sum() / (n - 1)

}
