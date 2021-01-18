@file:Suppress("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")

package mpei.lab4

import com.fasterxml.jackson.core.JsonGenerationException
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper
import javafx.application.Platform
import javafx.collections.ObservableList
import javafx.fxml.FXML
import javafx.scene.Scene
import javafx.scene.control.Alert
import javafx.scene.control.ChoiceBox
import javafx.scene.control.TextArea
import javafx.scene.control.TextField
import javafx.scene.layout.VBox
import javafx.stage.FileChooser
import javafx.stage.Modality
import javafx.stage.Stage
import javafx.stage.StageStyle
import mpei.lab3.Document
import mpei.lab3.MyException
import mpei.lab4.network.TCPConnection
import mpei.lab4.network.TCPConnectionListener
import org.json.JSONObject
import tornadofx.View
import tornadofx.asObservable
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.security.KeyStore


class MainView : View("Лабораторная работа №4"), TCPConnectionListener {
    override val root: VBox by fxml()

    private val selectUser: ChoiceBox<String> by fxid("SelectUser")
    private val userName: TextField by fxid("UserName")
    private val watchDocument: TextArea by fxid("WatchDocument")
    private val listUsers: ChoiceBox<String> by fxid("ListUsers")
    private val listDocuments: ChoiceBox<String> by fxid("ListDocuments")

    init {
        if (!File(pathKeyStore).exists()) {
            val keyStore = KeyStore.getInstance(keyStoreAlgorithm)
            keyStore.load(null, keyStorePassword)
            cr.createKeyPair(keyStore, Admin)
            keyStore.store(FileOutputStream(pathKeyStore), keyStorePassword)
        }

        val keyStore = KeyStore.getInstance(keyStoreAlgorithm)
        keyStore.load(FileInputStream(pathKeyStore), keyStorePassword)
        listCert = keyStore.aliases().toList().asObservable()
        selectUser.items = listCert
        selectUser.selectionModel.selectedItemProperty().addListener { _, _, it -> userName.text = it }

        settings = if (!File(pathSettings).exists()) Settings().createDefaultSettings()
        else loadSettings()
        val file = File(pathDocuments)
        listDoc.addAll(file.listFiles().map { it.name })

        listDocuments.selectionModel.selectedItemProperty().addListener { _, _, it ->
            try {
                if (!File(pathDocuments.plus(it)).exists()) throw MyException("Файл был удалён!")
                doc = loadDocument(pathDocuments.plus(it))
                cr.signDec(doc!!)
                watchDocument.text = doc!!.text
                this.title = "Автор: ${doc!!.author} Название: ${doc!!.name}"
            } catch (e: MyException) {
                cr.createAlert(e.message!!, "Ошибка!", Alert.AlertType.ERROR)
            }
        }
    }

    override fun onConnection(tcpConnection: TCPConnection) {
        val json = JSONObject()
        json.put(Login, userName.text)
        json.put(Command, Connect)
        tcpConnection.sendString(json)
        Platform.runLater { cr.createAlert("Подкючение с сервером установлено!", "Информирование", Alert.AlertType.INFORMATION) }
    }

    override fun onListUsers(json: JSONObject) {
        users = json.getJSONArray(Users).map { it.toString() }.asObservable()
    }

    override fun onDocument(json: JSONObject) {
        try {
            json.remove(Command)
            val doc = Document().convertToObject(json.getJSONObject(Doc))
            saveDocument(doc, pathDocuments.plus(doc.name))
            listDoc.add(doc.name)
        } catch (e: MyException) {
            cr.createAlert(e.message!!, "Ошибка!", Alert.AlertType.ERROR)
        }
    }

    override fun addUser(name: String) {
        users.add(name)
    }

    override fun removeUser(name: String) {
        users.remove(name)
    }

    override fun onDisconnect(tcpConnection: TCPConnection) {
        Platform.runLater { cr.createAlert("Подкючение с сервером разорвано!", "Информирование", Alert.AlertType.INFORMATION) }
    }

    override fun onException(tcpConnection: TCPConnection, e: Exception) {
        cr.createAlert("Ошибка подключения!", "Ошибка!", Alert.AlertType.ERROR)
    }

    @FXML
    private fun createFileAction() {
        this.title = "Подписанный документ"
        watchDocument.clear()
    }

    @FXML
    private fun openFileAction() {
        try {
            val fileChooser = FileChooser()
            fileChooser.title = "Открыть документ"
            fileChooser.initialDirectory = File("C:\\Users\\nagib\\IdeaProjects\\Lab4_ZD")
            val extFilter = FileChooser.ExtensionFilter("JSON files (*.json)", "*.json") //Расширение
            fileChooser.extensionFilters.add(extFilter)
            val file = fileChooser.showOpenDialog(primaryStage)
            val doc = loadDocument(file.name)
            cr.signDec(doc)
            watchDocument.text = doc.text
            this.title = "Автор: ${Companion.doc!!.author} Название: ${Companion.doc!!.name}"
        } catch (e: MyException) {
            cr.createAlert(e.message!!, "Ошибка!", Alert.AlertType.ERROR)
        }
    }

    @FXML
    private fun saveFileAction() {
        try {
            val fileChooser = FileChooser()
            fileChooser.title = "Сохранить документ"
            fileChooser.initialDirectory = File("C:\\Users\\nagib\\IdeaProjects\\Lab4_ZD\\MyDocuments")
            val extFilter = FileChooser.ExtensionFilter("JSON files (*.json)", "*.json")
            fileChooser.extensionFilters.add(extFilter)
            val file = fileChooser.showSaveDialog(primaryStage) ?: throw MyException("Такого файла не существует!")
            val author = userName.text
            if (author.isEmpty()) throw MyException("Введите имя пользователя!")
            doc = Document()
            doc!!.author = author
            doc!!.name = file.name
            doc!!.text = watchDocument.text
            doc = cr.signEnc(doc!!)
            saveDocument(doc!!, file.absolutePath)
            cr.createAlert("Документ ${file.name} успешно сохранён!", "Информирование", Alert.AlertType.INFORMATION)
        } catch (e: MyException) {
            cr.createAlert(e.message!!, "Ошибка!", Alert.AlertType.ERROR)
        }
    }

    @FXML
    private fun closeAction() {
        Platform.exit()
    }

    @FXML
    private fun aboutAction() {
        val aboutWindow = Scene(About().root)
        val newWindow = Stage()
        newWindow.scene = aboutWindow
        newWindow.initModality(Modality.APPLICATION_MODAL)
        newWindow.initOwner(primaryStage)
        newWindow.initStyle(StageStyle.DECORATED)
        newWindow.title = "О программе"
        newWindow.showAndWait()
    }

    @FXML
    private fun deleteKeyPairAction() {
        try {
            val name = userName.text
            if (name.isEmpty()) throw MyException("Введите имя пользователя!")
            if (!File(pathKeyStore).exists()) throw MyException("Хранилище ключей отсутствует!")
            val keyStore = KeyStore.getInstance(keyStoreAlgorithm)
            keyStore.load(FileInputStream(pathKeyStore), keyStorePassword)
            if (!keyStore.containsAlias(name)) throw MyException("Сертификат пользователя $name в хранилище отсутствуют!")
            keyStore.deleteEntry(name)
            keyStore.store(FileOutputStream(pathKeyStore), keyStorePassword)
            cr.createAlert(
                "Сертификат пользователя $name удалён!",
                "Информирование",
                Alert.AlertType.INFORMATION
            )
            listCert.remove(name)
        } catch (e: MyException) {
            cr.createAlert(e.message!!, "Ошибка!", Alert.AlertType.ERROR)
        }
    }

    @FXML
    private fun createKeyPairAction() {
        try {
            val name = userName.text
            if (name.isEmpty()) throw MyException("Введите имя пользователя!")
            if (!File(pathKeyStore).exists()) throw MyException("Хранилище ключей отсутствует!")
            val keyStore = KeyStore.getInstance(keyStoreAlgorithm)
            keyStore.load(FileInputStream(pathKeyStore), keyStorePassword)
            cr.createKeyPair(keyStore, name)
            keyStore.store(FileOutputStream(pathKeyStore), keyStorePassword)
            listCert.add(name.toLowerCase())
            cr.createAlert(
                "Сертификат для пользователя $name создан!",
                "Информирование",
                Alert.AlertType.INFORMATION
            )
        } catch (e: MyException) {
            cr.createAlert(e.message!!, "Ошибка!", Alert.AlertType.ERROR)
        }
    }

    @FXML
    private fun settingsAction() {
        val setSettings = SettingsView(settings.ip, settings.port)
        val settingsDispatcher = Scene(setSettings.root)
        val newWindow = Stage()
        newWindow.scene = settingsDispatcher
        newWindow.initModality(Modality.APPLICATION_MODAL)
        newWindow.initOwner(primaryStage)
        newWindow.initStyle(StageStyle.DECORATED)
        newWindow.setOnCloseRequest {
            settings.ip = setSettings.ip
            settings.port = setSettings.port
        }
        newWindow.showAndWait()
    }

    @FXML
    private fun deleteAction() {
        try {
            val docName = listDocuments.value
            if (docName.isEmpty()) throw MyException("Выберите название документа!")
            listDoc.remove(docName)
            watchDocument.clear()
            File(pathDocuments.plus(docName)).delete()
        } catch (e: MyException) {
            cr.createAlert(e.message!!, "Ошибка!", Alert.AlertType.ERROR)
        }
    }

    @FXML
    private fun sendAction() {
        try {
            val name = listUsers.value
            if (name.isEmpty()) throw MyException("Выберите пользователя для отправки!")
            if (doc == null) saveFileAction()
            val json = JSONObject()
            json.put(To, name)
            json.put(Command, Send)
            json.putOpt(Doc, doc)
            connection!!.sendString(json)
        } catch (e: MyException) {
            cr.createAlert(e.message!!, "Ошибка!", Alert.AlertType.ERROR)
        }
    }

    @FXML
    private fun connectAction() {
        try {
            if (connection != null) throw MyException("Вы уже подключены к серверу!")
            connection = TCPConnection(this, settings.ip, settings.port)
        } catch (e: IOException) {
            cr.createAlert("Ошибка подключения к серверу", "Ошибка!", Alert.AlertType.ERROR)
        } catch (e: MyException) {
            cr.createAlert(e.message!!, "Ошибка!", Alert.AlertType.ERROR)
        }
    }

    @FXML
    private fun disconnectAction() {
        if (connection != null) {
            connection!!.disconnect()
            connection = null
            cr.createAlert("Вы отключены от сервера", "Информирование", Alert.AlertType.INFORMATION)
        }
    }

    override fun onDock() {
        currentWindow?.setOnCloseRequest {
            saveSettings()
        }
    }

    @Throws(JsonGenerationException::class, JsonMappingException::class)
    private fun saveSettings() {
        mapper.writeValue(File(pathSettings), settings)
    }

    @Throws(JsonGenerationException::class, JsonMappingException::class)
    private fun loadSettings(): Settings = mapper.readValue(File(pathSettings), Settings::class.java)

    @Throws(JsonGenerationException::class, JsonMappingException::class)
    private fun saveDocument(doc: Document, path: String) {
        mapper.writeValue(File(path), doc)
    }

    @Throws(JsonGenerationException::class, JsonMappingException::class)
    private fun loadDocument(path: String): Document = mapper.readValue(File(path), Document::class.java)

    companion object {
        private var connection: TCPConnection? = null
        private lateinit var listCert: ObservableList<String>
        private lateinit var users: ObservableList<String>
        private val listDoc = listOf<String>().asObservable()
        private val cr = Crypto()
        private lateinit var settings: Settings
        private val mapper = ObjectMapper()
        private var doc: Document? = null
    }
}
