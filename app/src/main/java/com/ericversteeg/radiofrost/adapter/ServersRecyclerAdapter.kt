package com.ericversteeg.radiofrost.adapter

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ericversteeg.radiofrost.R
import com.ericversteeg.radiofrost.model.Server
import com.ericversteeg.radiofrost.model.SessionSettings
import com.ericversteeg.radiofrost.view.ButtonFrame

class ServersRecyclerAdapter(private val context: Context, private val servers: List<Server>, val serverOnClick: (server: Server) -> Unit): RecyclerView.Adapter<ServersRecyclerAdapter.ServerViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServerViewHolder {
        return ServerViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.adapter_server, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ServerViewHolder, position: Int) {
        holder.bind(servers[position], { server ->
            serverOnClick.invoke(server)
        }, {
            showDeleteDialog(it)
        })
    }

    override fun getItemCount(): Int {
        return servers.size
    }

    class ServerViewHolder(view: View): RecyclerView.ViewHolder(view) {
        private val buttonFrame: ButtonFrame = view.findViewById(R.id.button_frame)
        private val serverNameText: TextView = view.findViewById(R.id.text_server_name)

        fun bind(server: Server, onClick: (server: Server) -> Unit, onLongClick: (server: Server) -> Unit) {
            if (server.isAdmin) {
                serverNameText.text = "${server.name} (Admin)"
            }
            else {
                serverNameText.text = server.name
            }

            buttonFrame.setOnClickListener {
                onClick.invoke(server)
            }

            buttonFrame.setOnLongClickListener {
                onLongClick.invoke(server)
                return@setOnLongClickListener false
            }
        }
    }

    private fun showDeleteDialog(server: Server) {
        AlertDialog.Builder(context, R.style.AlertDialogTheme)
            .setMessage("Remove ${server.name} from your list of servers?")
            .setPositiveButton("Yes") { _, _ ->
                SessionSettings.instance.removeServer(context, server)
                notifyDataSetChanged()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}