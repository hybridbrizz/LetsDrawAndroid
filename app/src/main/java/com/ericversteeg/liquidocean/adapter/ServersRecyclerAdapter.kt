package com.ericversteeg.liquidocean.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ericversteeg.liquidocean.R
import com.ericversteeg.liquidocean.model.Server
import com.ericversteeg.liquidocean.view.ButtonFrame

class ServersRecyclerAdapter(private val servers: List<Server>, val serverOnClick: (server: Server) -> Unit): RecyclerView.Adapter<ServersRecyclerAdapter.ServerViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServerViewHolder {
        return ServerViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.adapter_server, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ServerViewHolder, position: Int) {
        holder.bind(servers[position]) { server ->
            serverOnClick.invoke(server)
        }
    }

    override fun getItemCount(): Int {
        return servers.size
    }

    class ServerViewHolder(view: View): RecyclerView.ViewHolder(view) {
        private val buttonFrame: ButtonFrame = view.findViewById(R.id.button_frame)
        private val serverNameText: TextView = view.findViewById(R.id.text_server_name)

        fun bind(server: Server, onClick: (server: Server) -> Unit) {
            if (server.isAdmin) {
                serverNameText.text = "${server.name} (Admin)"
            }
            else {
                serverNameText.text = server.name
            }

            buttonFrame.setOnClickListener {
                onClick.invoke(server)
            }
        }
    }
}