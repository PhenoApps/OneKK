package org.wheatgenetics.utils

import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.util.Log
import org.wheatgenetics.onekk.R
import org.wheatgenetics.onekk.database.models.AnalysisEntity
import org.wheatgenetics.onekk.database.models.ContourEntity
import java.io.FileNotFoundException

open class FileUtil(private val ctx: Context) {

    /**
     * Samples file headers
     * sample_id, picture_id, collector, timestamp, length_avg, length_var, length_cv, width_avg, width_var, width_cv,
     */
    private val nameHeader: String by lazy { ctx.getString(R.string.export_name_header) }
    private val pictureHeader: String by lazy { ctx.getString(R.string.export_picture_header) }
    private val collectorHeader: String by lazy { ctx.getString(R.string.export_collector_header) }
    private val dateHeader: String by lazy { ctx.getString(R.string.export_date_header) }
    private val lengthAvgHeader: String by lazy { ctx.getString(R.string.export_length_avg_header) }
    private val lengthVarHeader: String by lazy { ctx.getString(R.string.export_length_var_header) }
    private val lengthCvHeader: String by lazy { ctx.getString(R.string.export_length_cv_header) }
    private val widthAvgHeader: String by lazy { ctx.getString(R.string.export_width_avg_header) }
    private val widthVarHeader: String by lazy { ctx.getString(R.string.export_width_var_header) }
    private val widthCvHeader: String by lazy { ctx.getString(R.string.export_width_cv_header) }
    private val weightHeader: String by lazy { ctx.getString(R.string.export_weight_header) }

    private val exportHeaderString by lazy {
        arrayOf(nameHeader, pictureHeader, collectorHeader, dateHeader,
                lengthAvgHeader, lengthVarHeader, lengthCvHeader,
                widthAvgHeader, widthVarHeader, widthCvHeader, weightHeader)
                .joinToString(", ")
    }

    /**
     * seeds headers: sample_id, number, length, width, area, weight (=sample weight / seed area)
     */
    private val numberHeader: String by lazy { ctx.getString(R.string.export_number_header) }
    private val lengthHeader: String by lazy { ctx.getString(R.string.export_length_header) }
    private val widthHeader: String by lazy { ctx.getString(R.string.export_width_header) }
    private val areaHeader: String by lazy { ctx.getString(R.string.export_area_header) }
    private val xHeader: String by lazy { ctx.getString(R.string.export_x_header) }
    private val yHeader: String by lazy { ctx.getString(R.string.export_y_header) }

    private val exportSeedsHeaderString by lazy {
        arrayOf(nameHeader, numberHeader, lengthHeader, widthHeader,
                areaHeader, weightHeader, xHeader, yHeader)
                .joinToString(", ")
    }

    fun exportSeeds(uri: Uri, analysis: List<AnalysisEntity>, contours: List<ContourEntity>) {

        val newLine: ByteArray = System.getProperty("line.separator")?.toByteArray() ?: "\n".toByteArray()

        try {

            ctx.contentResolver.openOutputStream(uri).apply {

                this?.let {

                    //(nameHeader, numberHeader, lengthHeader, widthHeader, areaHeader, weightHeader)
                    write(exportSeedsHeaderString.toByteArray())

                    write(newLine)

                    analysis.forEach { analysis ->

                        contours.filter { it.aid == analysis.aid }.forEach {

                            if (it.contour?.count == 1) {

                                write("${analysis.name}, ${it.contour?.count}, ${it.contour?.maxAxis}, ${it.contour?.minAxis}, ${it.contour?.area}, ${analysis.weight ?: 0.0}, ${it.contour?.x}, ${it.contour?.y}".toByteArray())

                            } else {

                                write("${analysis.name}, ${it.contour?.count}, N/A, N/A, ${it.contour?.area}, ${analysis.weight ?: 0.0}, ${it.contour?.x}, ${it.contour?.y}".toByteArray())

                            }

                            write(newLine)
                        }

                    }

                    close()
                }

            }

        } catch (exception: FileNotFoundException) {

            Log.e("IntFileNotFound", "Chosen uri path was not found: $uri")

        }

        MediaScannerConnection.scanFile(ctx, arrayOf(uri.path), arrayOf("*/*"), null)

    }

    fun export(uri: Uri, analysisList: List<AnalysisEntity>) {

        val newLine: ByteArray = System.getProperty("line.separator")?.toByteArray() ?: "\n".toByteArray()

        try {

            ctx.contentResolver.openOutputStream(uri).apply {

                this?.let {

                    write(exportHeaderString.toByteArray())

                    write(newLine)

                    analysisList.forEach { analysis ->

                        write("${analysis.name}, ${analysis.src}, ${analysis.collector}, ${analysis.date}, ${analysis.maxAxisAvg}, ${analysis.maxAxisVar}, ${analysis.maxAxisCv}, ${analysis.minAxisAvg}, ${analysis.minAxisVar}, ${analysis.minAxisCv}, ${analysis.weight}".toByteArray())

                        write(newLine)

                    }

                    close()
                }

            }

        } catch (exception: FileNotFoundException) {

            Log.e("IntFileNotFound", "Chosen uri path was not found: $uri")

        }

        MediaScannerConnection.scanFile(ctx, arrayOf(uri.path), arrayOf("*/*"), null)

    }
}