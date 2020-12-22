package org.wheatgenetics.onekk.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.wheatgenetics.onekk.R
import org.wheatgenetics.onekk.callbacks.DiffCallbacks
import org.wheatgenetics.onekk.database.models.ContourEntity
import org.wheatgenetics.onekk.databinding.ListItemContourBinding
import org.wheatgenetics.onekk.interfaces.ContourOnTouchListener

class ContourAdapter(private val listener: ContourOnTouchListener) : ListAdapter<ContourEntity,
        ContourAdapter.ViewHolder>(DiffCallbacks.Companion.ContourDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
                DataBindingUtil.inflate(
                        LayoutInflater.from(parent.context),
                        R.layout.list_item_contour, parent, false
                )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        getItem(position).let { contour ->

            with(holder) {

                itemView.tag = contour.cid

                bind(contour)
            }
        }
    }

    inner class ViewHolder(private val binding: ListItemContourBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(model: ContourEntity) {

            with(binding) {

                highlight = View.OnClickListener {

                    this.selected = !model.selected

                    model.selected = !model.selected

                    listener.onChoiceSwapped(model.cid ?: -1, model.selected)
                }

                viewCluster = View.OnClickListener {

                    with(model.contour) {

                        this?.let {

                            listener.onTouch(model.cid!!, x, y, count > 1, minAxis, maxAxis)

                        }
                    }
                }

                this.area = shortenString((model.contour?.area ?: 0.0))

                this.length = shortenString((model.contour?.maxAxis ?: 0.0))

                this.width = shortenString(model.contour?.minAxis ?: 0.0)

                this.count = shortenString(model.contour?.count?.toDouble() ?: 0.0)

                this.selected = model.selected
            }
        }

        //simple function that takes a string expecting decimal places and shortens to 2 after the decimal.
        private fun shortenString(long: Double): String {

            val decimalPlaces = 2

            val longNumber = long.toString()

            val last = longNumber.indexOf(".") + decimalPlaces

            return longNumber.padEnd(last).substring(0, last)

        }
    }
}