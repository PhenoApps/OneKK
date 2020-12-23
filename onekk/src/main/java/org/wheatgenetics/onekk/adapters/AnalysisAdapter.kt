package org.wheatgenetics.onekk.adapters

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.opencv.core.CvException
import org.wheatgenetics.onekk.R
import org.wheatgenetics.onekk.database.models.AnalysisEntity
import org.wheatgenetics.onekk.database.models.ImageEntity
import org.wheatgenetics.onekk.databinding.ListItemAnalysisBinding
import org.wheatgenetics.onekk.interfaces.OnClickAnalysis
import org.wheatgenetics.onekk.shortenString

class AnalysisAdapter(private val uris: List<ImageEntity>, private val listener: OnClickAnalysis) : ListAdapter<AnalysisEntity, RecyclerView.ViewHolder>(DiffCallback()), CoroutineScope by MainScope() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

        return ViewHolder(this, uris, DataBindingUtil.inflate(
                        LayoutInflater.from(parent.context),
                        R.layout.list_item_analysis, parent, false))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

        getItem(position).let { analysis ->

            with(holder as ViewHolder) {

                itemView.tag = analysis.aid

                bind(position, listener, analysis)

            }
        }
    }

    class ViewHolder(private val coroutineScope: CoroutineScope, private val images: List<ImageEntity>, private val binding: ListItemAnalysisBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(position: Int, listener: OnClickAnalysis, model: AnalysisEntity) {

            with(binding) {

                coroutineScope.launch {

                    val bmp = BitmapFactory.decodeFile(images.find { it.aid == (itemView.tag as Int) }?.image?.example)

                    try {

                        exampleImageView.setImageBitmap(bmp)

                    } catch (e: CvException) {

                        e.printStackTrace()

                        bmp
                    }
                }

                weighButton.setOnClickListener {

                    listener.onClick(model.aid!!)

                }

                countButton.setOnClickListener {

                    listener.onClickCount(model.aid!!)

                 }

                itemView.setOnClickListener {

                    model.selected = !model.selected

                    listener.onSelectionSwapped(position, model, model.selected)
                }

                name = model.name

                date = model.date

                count = model.count.toString()

//                avgWeight = shortenString(model.weight ?: 1.0 / (model.count ?: 1).toDouble())

                maxAxisCv = shortenString(model.maxAxisCv ?: 1.0)

                minAxisCv = shortenString(model.minAxisCv ?: 1.0)

                maxAxisVar = shortenString(model.maxAxisVar ?: 1.0)

                minAxisVar = shortenString(model.minAxisVar ?: 1.0)

                weight = shortenString(model.weight ?: 0.0) + "g"

                minAxisAvg = shortenString(model.minAxisAvg ?: 0.0)

                maxAxisAvg = shortenString(model.maxAxisAvg ?: 0.0)

                selected = model.selected
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<AnalysisEntity>() {

        override fun areItemsTheSame(oldItem: AnalysisEntity, newItem: AnalysisEntity): Boolean {
            return false
        }

        override fun areContentsTheSame(oldItem: AnalysisEntity, newItem: AnalysisEntity): Boolean {
            return false
        }
    }
}