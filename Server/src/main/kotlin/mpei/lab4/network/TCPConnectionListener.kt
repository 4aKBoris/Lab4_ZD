package mpei.lab4.network

import org.json.JSONObject

interface TCPConnectionListener {
    fun onConnection(tcpConnection: TCPConnection, name: String)

    fun onDocument(json: JSONObject)

    fun onDisconnect(tcpConnection: TCPConnection)

    fun onException(tcpConnection: TCPConnection, e: Exception)
}