package org.wheatgenetics.onekk.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.navigation.Navigation
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.wheatgenetics.onekk.R
import org.wheatgenetics.onekk.callbacks.DiffCallbacks
import org.wheatgenetics.onekk.database.models.ContourEntity
import org.wheatgenetics.onekk.database.models.ExperimentEntity
import org.wheatgenetics.onekk.databinding.ListItemContourBinding
import org.wheatgenetics.onekk.databinding.ListItemExperimentBinding
import org.wheatgenetics.onekk.fragments.ExperimentFragmentDirections
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

        fun bind(contour: ContourEntity) {

            with(binding) {

                clickListener = View.OnClickListener {

                    with(contour.contour) {

                        this?.let {

                            listener.onTouch(x, y, minAxis, maxAxis)

                        }
                    }
                }

                this.model = contour

            }
        }
    }
}