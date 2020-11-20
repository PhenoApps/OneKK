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
import org.wheatgenetics.onekk.database.models.ExperimentEntity
import org.wheatgenetics.onekk.databinding.ListItemExperimentBinding
import org.wheatgenetics.onekk.fragments.ExperimentFragmentDirections

class ExperimentAdapter(
        val context: Context
) : ListAdapter<ExperimentEntity, ExperimentAdapter.ViewHolder>(DiffCallbacks.Companion.ExperimentDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
                DataBindingUtil.inflate(
                        LayoutInflater.from(parent.context),
                        R.layout.list_item_experiment, parent, false
                )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        getItem(position).let { experiment ->

            with(holder) {

                itemView.tag = experiment.eid

                bind(experiment)
            }
        }
    }

    class ViewHolder(private val binding: ListItemExperimentBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(experiment: ExperimentEntity) {

            with(binding) {

                clickListener = View.OnClickListener {

                    experiment.eid?.let { eid ->

                        Navigation.findNavController(binding.root).navigate(
                                ExperimentFragmentDirections.actionToCamera(eid))

                    }

                }

                this.model = experiment

            }
        }
    }
}