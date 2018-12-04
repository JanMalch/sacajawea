package io.github.janmalch.sacajawea.listening

import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import io.github.janmalch.sacajawea.R
import io.github.janmalch.sacajawea.inflate
import kotlinx.android.synthetic.main.rv_device.view.*

class DeviceRVAdapter(private val items: List<Translator>, private val listener: (Translator) -> Unit) :
    RecyclerView.Adapter<DeviceRVAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(parent.inflate(R.layout.rv_device))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(items[position], listener)

    override fun getItemCount() = items.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(item: Translator, listener: (Translator) -> Unit) = with(itemView) {
            val txt = item.name + " >> " + item.language + "\n:" + item.port
            rv_device_address.text = txt
            setOnClickListener { listener(item) }
        }
    }
}