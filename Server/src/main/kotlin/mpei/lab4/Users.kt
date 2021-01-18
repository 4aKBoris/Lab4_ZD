package mpei.lab4

import mpei.lab4.network.TCPConnection

class Users() {
    private val users = mutableListOf<User>()

    fun add(user: User) {
        users.add(user)
    }

    fun remove(tcpConnection: TCPConnection) {
       users.removeIf { it.tcpConnection == tcpConnection }
    }

    fun getName(tcpConnection: TCPConnection) = users.first { it.tcpConnection == tcpConnection }.name

    fun listUsers() = users.map{ it.name }

    fun find(name: String) = users.first { it.name == name }.tcpConnection

    fun getTCP() = users.map { it.tcpConnection }
}