package mpei.lab4

import mpei.lab4.network.TCPConnection

data class User(val name: String, val tcpConnection: TCPConnection)