package org.wheatgenetics.onekk.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import org.wheatgenetics.onekk.R
import org.wheatgenetics.onekk.database.models.AnalysisEntity
import org.wheatgenetics.onekk.databinding.ListItemAnalysisBinding
import org.wheatgenetics.onekk.interfaces.OnClickAnalysis
import org.wheatgenetics.onekk.shortenString
import kotlin.math.sqrt

class AnalysisAdapter(private val listener: OnClickAnalysis) : ListAdapter<AnalysisEntity, RecyclerView.ViewHolder>(DiffCallback()), CoroutineScope by MainScope() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

        return ViewHolder(DataBindingUtil.inflate(
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

    class ViewHolder(private val binding: ListItemAnalysisBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(position: Int, listener: OnClickAnalysis, model: AnalysisEntity) {

            with(binding) {

                //this is required to force marquee
                nameTextView.isSelected = true

                graphButton.setOnClickListener {

                    listener.onClickGraph(model.aid!!)

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

//                maxAxisCv = shortenString(model.maxAxisCv ?: 1.0)
//
//                minAxisCv = shortenString(model.minAxisCv ?: 1.0)

                tkw = shortenString(model.tkw ?: 0.0)

                maxAxisVar = shortenString(sqrt(model.maxAxisVar ?: 1.0))

                minAxisVar = shortenString(sqrt(model.minAxisVar ?: 1.0))

                weight = "${model.weight ?: 0.0}g"

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