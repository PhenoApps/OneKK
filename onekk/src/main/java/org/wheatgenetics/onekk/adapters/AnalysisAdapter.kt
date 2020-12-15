package org.wheatgenetics.onekk.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
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

class AnalysisAdapter(private val listener: OnClickAnalysis) : ListAdapter<AnalysisEntity, RecyclerView.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

        return ViewHolder(DataBindingUtil.inflate(
                        LayoutInflater.from(parent.context),
                        R.layout.list_item_analysis, parent, false))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

        getItem(position).let { analysis ->

            with(holder as ViewHolder) {

                holder.itemView.findViewById<TextView>(R.id.listWeightEditText).setOnClickListener {

                    listener.onClick(analysis.aid!!)

                }

                holder.itemView.findViewById<TextView>(R.id.countTextView).setOnClickListener {

                    listener.onClickCount(analysis.aid!!)

                }

                bind(analysis)

            }
        }
    }

    class ViewHolder(private val binding: ListItemAnalysisBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(model: AnalysisEntity) {

            with(binding) {

                date = model.date

                count = model.count.toString()

                weight = model.weight.toString()
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