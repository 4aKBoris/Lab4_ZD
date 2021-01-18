@file:Suppress("PackageName")

package mpei.lab4.network

import mpei.lab4.network.TCPConnection
import org.json.JSONObject

interface TCPConnectionListener {
    fun onConnection(tcpConnection: TCPConnection)

    fun onListUsers(json: JSONObject)
    fun onDocument(json: JSONObject)
    fun addUser(name: String)
    fun removeUser(name: String)

    fun onDisconnect(tcpConnection: TCPConnection)

    fun onException(tcpConnection: TCPConnection, e: Exception)
}