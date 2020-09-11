package org.wheatgenetics.onekk.adapters

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.wheatgenetics.onekk.R
import org.wheatgenetics.onekk.databinding.ListItemAnalysisBinding

class AnalysisAdapter : ListAdapter<Bitmap, RecyclerView.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

        return ViewHolder(DataBindingUtil.inflate(
                        LayoutInflater.from(parent.context),
                        R.layout.list_item_analysis, parent, false))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

        getItem(position).let { bmp ->

            with(holder as ViewHolder) {

                bind(bmp)

            }
        }
    }

    class ViewHolder(
            private val binding: ListItemAnalysisBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(bmp: Bitmap) {

            with(binding) {

                imageView.setImageBitmap(bmp)

            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Bitmap>() {

        override fun areItemsTheSame(oldItem: Bitmap, newItem: Bitmap): Boolean {
            return false
        }

        override fun areContentsTheSame(oldItem: Bitmap, newItem: Bitmap): Boolean {
            return false
        }
    }
}