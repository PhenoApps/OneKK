package org.wheatgenetics.onekk.callbacks

import androidx.recyclerview.widget.DiffUtil
import org.wheatgenetics.onekk.database.models.CoinEntity
import org.wheatgenetics.onekk.database.models.ContourEntity

/**
 * DiffCallbacks are used in each adapter implementation.
 * They define equality/uniqueness of each item in a list.
 * Duplicate items will not show up if areItemsTheSame is true.
 */
class DiffCallbacks {

    companion object {

        class ContourDiffCallback : DiffUtil.ItemCallback<ContourEntity>() {

            override fun areItemsTheSame(oldItem: ContourEntity, newItem: ContourEntity): Boolean {
                return oldItem.aid == newItem.aid && oldItem.cid == newItem.cid
            }

            override fun areContentsTheSame(oldItem: ContourEntity, newItem: ContourEntity): Boolean {
                return oldItem.aid == newItem.aid && oldItem.cid == newItem.cid
            }
        }

        class CoinDiffCallback : DiffUtil.ItemCallback<CoinEntity>() {

            override fun areItemsTheSame(oldItem: CoinEntity, newItem: CoinEntity): Boolean {
                return oldItem.name == newItem.name && oldItem.country == newItem.country
            }

            override fun areContentsTheSame(oldItem: CoinEntity, newItem: CoinEntity): Boolean {
                return oldItem.name == newItem.name && oldItem.country == newItem.country
            }
        }
    }
}
