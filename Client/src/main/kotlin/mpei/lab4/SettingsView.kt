@file:Suppress("unused")

package mpei.lab4

import javafx.fxml.FXML
import javafx.scene.control.Alert
import javafx.scene.control.TextField
import javafx.scene.layout.VBox
import mpei.lab3.MyException
import tornadofx.View

class SettingsView(var ip: String, var port: Int) : View("Настройки") {
    override val root: VBox by fxml()

    private val ipTextFiled: TextField by fxid("IpTextField")
    private val portTextFiled: TextField by fxid("PortTextField")

    init {
        ipTextFiled.text = ip
        portTextFiled.text = port.toString()
    }

    @FXML
    private fun saveSettings() {
        try {
            if (regPort.matches(portTextFiled.text)) throw MyException("Значение порта введено неверно!")
            if (regIP.matches(ipTextFiled.text)) throw MyException("Значение IP-адреса введено неверно!")
            ipTextFiled.text.split('.').forEach {
                val k = Integer.parseInt(it)
                if (k < 0 || k > 255) throw MyException("Значение IP-адреса введено неверно!")
            }
            ip = ipTextFiled.text
            port = Integer.parseInt(portTextFiled.text)
            this.close()
        } catch (e: MyException) {
            Crypto().createAlert(e.message!!, "Ошибка!", Alert.AlertType.ERROR)
        }
    }

    companion object {
        private val regPort = Regex("\\d{1,4}")
        private val regIP = Regex("\\d{1,3}.\\d{1,3}.\\d{1,3}.\\d{1,3}")
    }
}
