package org.wheatgenetics.onekk.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.wheatgenetics.onekk.R
import org.wheatgenetics.onekk.database.models.AnalysisEntity
import org.wheatgenetics.onekk.databinding.ListItemAnalysisBinding
import org.wheatgenetics.onekk.interfaces.OnClickAnalysis
import org.wheatgenetics.onekk.shortenString

class AnalysisAdapter(private val listener: OnClickAnalysis) : ListAdapter<AnalysisEntity, RecyclerView.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

        return ViewHolder(DataBindingUtil.inflate(
                        LayoutInflater.from(parent.context),
                        R.layout.list_item_analysis, parent, false))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

        getItem(position).let { analysis ->

            with(holder as ViewHolder) {

                bind(position, listener, analysis)

            }
        }
    }

    class ViewHolder(private val binding: ListItemAnalysisBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(position: Int, listener: OnClickAnalysis, model: AnalysisEntity) {

            with(binding) {

                listWeightEditText.setOnClickListener {

                    listener.onClick(model.aid!!)

                }

                countTextView.setOnClickListener {

                    listener.onClickCount(model.aid!!)

                 }

                itemView.setOnClickListener {

                    model.selected = !model.selected

                    listener.onSelectionSwapped(position, model, model.selected)
                }

                name = model.name

                date = model.date

                count = model.count.toString()

                weight = model.weight.toString()

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