package org.wheatgenetics.onekk.callbacks

import androidx.recyclerview.widget.DiffUtil
import org.wheatgenetics.onekk.database.models.ContourEntity
import org.wheatgenetics.onekk.database.models.ExperimentEntity

/**
 * DiffCallbacks are used in each adapter implementation.
 * They define equality/uniqueness of each item in a list.
 * Duplicate items will not show up if areItemsTheSame is true.
 */
class DiffCallbacks {

    companion object {

        class ExperimentDiffCallback : DiffUtil.ItemCallback<ExperimentEntity>() {

            override fun areItemsTheSame(oldItem: ExperimentEntity, newItem: ExperimentEntity): Boolean {
                return oldItem.eid == newItem.eid
            }

            override fun areContentsTheSame(oldItem: ExperimentEntity, newItem: ExperimentEntity): Boolean {
                return oldItem.eid == newItem.eid
            }
        }

        class ContourDiffCallback : DiffUtil.ItemCallback<ContourEntity>() {

            override fun areItemsTheSame(oldItem: ContourEntity, newItem: ContourEntity): Boolean {
                return oldItem.aid == newItem.aid && oldItem.cid == newItem.cid
            }

            override fun areContentsTheSame(oldItem: ContourEntity, newItem: ContourEntity): Boolean {
                return oldItem.aid == newItem.aid && oldItem.cid == newItem.cid
            }
        }
    }
}
