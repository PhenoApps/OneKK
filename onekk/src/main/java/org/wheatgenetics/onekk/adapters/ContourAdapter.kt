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

class ContourAdapter(
        private val listener: ContourOnTouchListener,
        val context: Context
) : ListAdapter<ContourEntity, ContourAdapter.ViewHolder>(DiffCallbacks.Companion.ContourDiffCallback()) {

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

                checkbox.isChecked = model.selected

                checkbox.setOnClickListener {

                    listener.onChoiceSwapped(model.cid ?: -1, checkbox.isChecked)

                }

                clickListener = View.OnClickListener {

                    with(model.contour) {

                        this?.let {

                            listener.onTouch(x, y, count > 1, minAxis, maxAxis)

                        }
                    }
                }

                this.area = shortenString((model.contour?.area ?: 0.0).toString())

                //format the displayed data, single contours show their measurements as min/max axis and area in mm
                //clusters show their estimated count along with their estimated area in mm
                this.data = if ((model.contour?.count ?: 1) == 1) {

                    val maxAxis = shortenString((model.contour?.maxAxis ?: 0.0).toString())
                    val minAxis = shortenString((model.contour?.minAxis ?: 0.0).toString())

                    "$maxAxis x $minAxis"

                } else ((model.contour?.count) ?: 0).toString()

                this.selected = model.selected
            }
        }

        //simple function that takes a string expecting decimal places and shortens to 2 after the decimal.
        private fun shortenString(long: String): String {

            val last = long.indexOf(".") + 3

            return long.padEnd(last).substring(0, last)

        }
    }
}