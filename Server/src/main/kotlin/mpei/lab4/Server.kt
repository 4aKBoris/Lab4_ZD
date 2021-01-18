package mpei.lab4

import Add
import Command
import Name
import Remove
import To
import listUsers
import mpei.lab4.network.TCPConnection
import mpei.lab4.network.TCPConnectionListener
import org.json.JSONObject
import java.io.IOException
import java.net.ServerSocket

class Server : TCPConnectionListener {
    private val users = Users()

    override fun onConnection(tcpConnection: TCPConnection, name: String) {
        val json = JSONObject()
        json.put(Command, Add)
        json.put(Name, name)
        users.getTCP().forEach { sendMessage(json, it) }
        val j = JSONObject()
        j.put(Command, listUsers)
        j.put(listUsers, users.listUsers())
        sendMessage(j, tcpConnection)
        users.add(User(name, tcpConnection))
    }

    override fun onDocument(json: JSONObject) {
        val name = json.remove(To).toString()
        sendMessage(json, users.find(name))
    }

    override fun onDisconnect(tcpConnection: TCPConnection) {
        val json = JSONObject()
        json.put(Command, Remove)
        json.put(Name, users.getName(tcpConnection))
        users.remove(tcpConnection)
        users.getTCP().forEach { sendMessage(json, it) }
    }


    @Synchronized
    override fun onException(tcpConnection: TCPConnection, e: Exception) {
        println("Ошибка соединения: $e")
    }

    @Synchronized
    private fun sendMessage(msg: JSONObject, tcpConnection: TCPConnection) {
        tcpConnection.sendString(msg)
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            Server()
        }

        private const val PORT = 8289
    }

    init {
        println("Server running...")
        try {
            ServerSocket(PORT).use { serverSocket ->
                while (true) {
                    try {
                        TCPConnection(this, serverSocket.accept())
                    } catch (e: IOException) {
                        println("Ошибка соединения: $e")
                    }
                }
            }
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

}