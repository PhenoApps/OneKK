package org.wheatgenetics.utils

import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.util.Log
import org.wheatgenetics.onekk.R
import org.wheatgenetics.onekk.database.models.AnalysisEntity
import java.io.FileNotFoundException

open class FileUtil(private val ctx: Context) {

    /**
     * Cross table export file header fields.
     */
    private val nameHeader: String by lazy { ctx.getString(R.string.export_name_header) }

    private val dateHeader: String by lazy { ctx.getString(R.string.export_date_header) }

    private val countHeader: String by lazy { ctx.getString(R.string.export_count_header) }

    private val weightHeader: String by lazy { ctx.getString(R.string.export_weight_header) }

    private val exportHeaderString by lazy {
        arrayOf(nameHeader, dateHeader, countHeader, weightHeader)
                .joinToString(", ")
    }

    fun export(uri: Uri, analysisList: List<AnalysisEntity>) {

        val newLine: ByteArray = System.getProperty("line.separator")?.toByteArray() ?: "\n".toByteArray()

        try {

            ctx.contentResolver.openOutputStream(uri).apply {

                this?.let {

                    write(exportHeaderString.toByteArray())

                    write(newLine)

                    analysisList.forEach { analysis ->

                        write("${analysis.name}, ${analysis.date}, ${analysis.count}, ${analysis.weight}".toByteArray())

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