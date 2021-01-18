@file:Suppress("PackageName", "CAST_NEVER_SUCCEEDS")

package mpei.lab4.network

import mpei.lab4.*
import mpei.lab4.Add
import mpei.lab4.Command
import mpei.lab4.Doc
import mpei.lab4.Users
import org.json.JSONObject
import java.io.*
import java.net.Socket
import java.nio.charset.StandardCharsets

class TCPConnection(eventListener: TCPConnectionListener, IP: String, PORT: Int) {
    private val socket: Socket = Socket(IP, PORT)
    private lateinit var rxThread: Thread
    private val out: BufferedWriter = BufferedWriter(OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))
    private val `in`: BufferedReader = BufferedReader(InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))
    private val eventListener: TCPConnectionListener? = null

    @Synchronized
    fun sendString(msg: JSONObject) {
        try {
            out.write("$msg\r\n")
            out.flush()
        } catch (e: IOException) {
            eventListener!!.onException(this@TCPConnection, e)
            disconnect()
        }
    }

    override fun toString(): String {
        return socket.localAddress.toString() + ":   " + socket.port
    }

    @Synchronized
    fun disconnect() {
        rxThread.interrupt()
        try {
            socket.close()
        } catch (e: IOException) {
            eventListener!!.onException(this@TCPConnection, e)
        }
    }

    override fun hashCode(): Int {
        var result = socket.hashCode()
        result = 31 * result + rxThread.hashCode()
        result = 31 * result + (eventListener?.hashCode() ?: 0)
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TCPConnection

        if (socket != other.socket) return false
        if (rxThread != other.rxThread) return false

        return true
    }

    init {
        rxThread = Thread {
            try {
                eventListener.onConnection(this)
                while (!rxThread.isInterrupted) {
                    val json = JSONObject(`in`.readLine())
                    when(json.getString(Command)) {
                        Users -> eventListener.onListUsers(json)
                        Doc -> eventListener.onDocument(json)
                        Add -> eventListener.addUser(json.getString(Name))
                        Remove -> eventListener.removeUser(json.getString(Name))
                    }
                }
            } catch (e: IOException) {
                eventListener.onException(this@TCPConnection, e)
            } finally {
                eventListener.onDisconnect(this)
            }
        }
        rxThread.start()
    }
}