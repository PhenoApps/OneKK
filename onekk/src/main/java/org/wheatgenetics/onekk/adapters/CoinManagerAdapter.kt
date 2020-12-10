package org.wheatgenetics.onekk.adapters

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.wheatgenetics.onekk.R
import org.wheatgenetics.onekk.callbacks.DiffCallbacks
import org.wheatgenetics.onekk.database.models.CoinEntity
import org.wheatgenetics.onekk.databinding.ListItemCoinManagerBinding
import org.wheatgenetics.onekk.interfaces.CoinValueChangedListener

class CoinManagerAdapter(
        private val listener: CoinValueChangedListener,
        val context: Context
) : ListAdapter<CoinEntity, CoinManagerAdapter.ViewHolder>(DiffCallbacks.Companion.CoinDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
                DataBindingUtil.inflate(
                        LayoutInflater.from(parent.context),
                        R.layout.list_item_coin_manager, parent, false
                )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        getItem(position).let { coin ->

            with(holder) {

                itemView.tag = coin.name

                bind(coin)
            }
        }
    }

    inner class ViewHolder(private val binding: ListItemCoinManagerBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(coin: CoinEntity) {

            with(binding) {

                this.coinValueEditText.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                    override fun afterTextChanged(newText: Editable?) {

                        newText?.let { nonNullText ->

                            nonNullText.toString().toDoubleOrNull()?.let { newCoinValue ->

                                listener.onCoinValueChanged(coin.country, coin.name, newCoinValue)

                            }
                        }
                    }
                })

                this.model = coin
            }
        }
    }
}