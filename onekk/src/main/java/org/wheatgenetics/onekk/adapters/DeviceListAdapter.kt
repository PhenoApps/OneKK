package org.wheatgenetics.onekk.adapters

import android.bluetooth.BluetoothDevice
import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.wheatgenetics.onekk.R
import org.wheatgenetics.onekk.databinding.ListItemAnalysisBinding
import org.wheatgenetics.onekk.databinding.ListItemBluetoothDeviceBinding
import org.wheatgenetics.onekk.interfaces.RecyclerViewClickListener

class DeviceListAdapter(private val listener: RecyclerViewClickListener) : ListAdapter<BluetoothDevice, RecyclerView.ViewHolder>(DiffCallback()) {

    private val deviceList = ArrayList<BluetoothDevice>()

    /**
     * Add device populates an item to the recycler list and updates the recycler data.
     * device is the bluetooth device that is discovered
     * rssi is the connection strength, items are sorted by rssi
     */
    fun addDevice(device: BluetoothDevice, rssi: Int) {

        device.name?.let { deviceName ->

            deviceList.add(device)

            submitList(deviceList.distinct())

            notifyDataSetChanged()

        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

        return ViewHolder(DataBindingUtil.inflate(
                        LayoutInflater.from(parent.context),
                        R.layout.list_item_bluetooth_device, parent, false), listener)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

        getItem(position).let { device ->

            with(holder as ViewHolder) {

                bind(device)

                itemView.findViewById<TextView>(R.id.deviceName)
                        .setText(device.name)

            }
        }
    }

    class ViewHolder(
            private val binding: ListItemBluetoothDeviceBinding,
            private val listener: RecyclerViewClickListener) : RecyclerView.ViewHolder(binding.root), View.OnClickListener {

        override fun onClick(v: View?) {

            listener.onItemClick(v, adapterPosition)

        }

        fun bind(device: BluetoothDevice) {

            with(binding) {

                deviceName.text = device.name

                deviceName.setOnClickListener(this@ViewHolder)
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<BluetoothDevice>() {

        override fun areItemsTheSame(oldItem: BluetoothDevice, newItem: BluetoothDevice): Boolean {
            return oldItem.address == newItem.address
        }

        override fun areContentsTheSame(oldItem: BluetoothDevice, newItem: BluetoothDevice): Boolean {
            return oldItem.address == newItem.address
        }
    }
}