package org.wheatgenetics.onekk.adapters

import android.text.Editable
import android.text.TextWatcher
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
import org.wheatgenetics.onekk.shortenString

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

                            listener.onTouch(model.cid!!, x, y, count > 1, minAxis ?: 0.0, maxAxis ?: 0.0)

                        }
                    }
                }

                val watcher = object : TextWatcher {

                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                    }

                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    }

                    override fun afterTextChanged(s: Editable?) {
                        listener.onCountEdited((itemView.tag as Int), s.toString().toIntOrNull() ?: 0)
                    }
                }

                countTextView.addTextChangedListener(watcher)

                this.area = shortenString((model.contour?.area ?: 0.0))

                val maxAxis = model.contour?.maxAxis
                this.length = if (maxAxis != null) {
                    shortenString(maxAxis)
                } else "NA"

                val minAxis = model.contour?.minAxis
                this.width = if (minAxis != null) {
                    shortenString(minAxis)
                } else "NA"

                this.count = model.contour?.count ?: 0

                this.selected = model.selected
            }
        }
    }
}