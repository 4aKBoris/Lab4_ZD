@file:Suppress("DEPRECATION")

package mpei.lab4

import javafx.scene.control.Alert
import mpei.lab3.Document
import mpei.lab3.MyException
import org.bouncycastle.x509.X509V3CertificateGenerator
import java.io.*
import java.math.BigInteger
import java.security.*
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.*
import javax.naming.ldap.LdapName
import javax.security.auth.x500.X500Principal
import kotlin.random.Random

open class Crypto {
    fun createAlert(msg: String, header: String, type: Alert.AlertType) {
        val alert = Alert(type)
        alert.headerText = header
        alert.contentText = msg
        alert.showAndWait()
    }

    fun createKeyPair(keyStore: KeyStore, name: String) {
        val rnd = Random
        val keyPairGenerator = KeyPairGenerator.getInstance("DSA")
        keyPairGenerator.initialize(1024)
        val keyPair = keyPairGenerator.genKeyPair()
        val gen = X509V3CertificateGenerator()
        val serverCommonName = X500Principal("CN=Dmitriy Losev")
        val commonName = X500Principal("CN=$name")
        val after = Date(2030, 1, 1, 0, 0, 0)
        val before = Date()
        gen.setIssuerDN(serverCommonName)
        gen.setNotBefore(after)
        gen.setNotAfter(before)
        gen.setSubjectDN(commonName)
        gen.setPublicKey(keyPair.public)
        gen.setSignatureAlgorithm("SHA256withDSA")
        gen.setSerialNumber(BigInteger(rnd.nextInt(0, 2000000), java.util.Random()))
        val myCert = gen.generate(keyPair.private)
        keyStore.setKeyEntry(name, keyPair.private, null, arrayOf(myCert))
    }

    @Throws(MyException::class)
    fun readFile(file: File): ByteArray {
        if (!file.exists()) throw MyException("Файла ${file.name} не существует!")
        val br = BufferedInputStream(FileInputStream(file))
        val arr = br.readBytes()
        br.close()
        return arr
    }

    fun writeFile(file: File, arr: ByteArray) {
        val bw = BufferedOutputStream(FileOutputStream(file))
        bw.write(arr)
        bw.close()
    }

    @Throws(MyException::class)
    fun signEnc(doc: Document): Document {
        if (!File(pathKeyStore).exists()) throw MyException("Отсутствует хранилище ключей!")
        val keyStore = KeyStore.getInstance(keyStoreAlgorithm)
        keyStore.load(FileInputStream(pathKeyStore), keyStorePassword)
        if (!keyStore.containsAlias(doc.author)) throw MyException("Для пользователя ${doc.author} не существует закрытого ключа!")
        val entryPassword = KeyStore.PasswordProtection(null)
        val privateKeyEntry =
            keyStore.getEntry(doc.author, entryPassword) as KeyStore.PrivateKeyEntry
        val sign = Signature.getInstance(SHA1)
        sign.initSign(privateKeyEntry.privateKey, SecureRandom())
        sign.update(doc.text.toByteArray(Charsets.UTF_8))
        doc.signature = sign.sign()
        doc.certificate = privateKeyEntry.certificate.encoded
        return doc
    }

    @Throws(MyException::class)
    fun signDec(doc: Document) {
        val crt = File("cert.cer")
        writeFile(crt, doc.certificate)
        val certificateFactory = CertificateFactory.getInstance("X.509")
        val fis = FileInputStream(crt)
        val c = certificateFactory.generateCertificate(fis) as X509Certificate
        fis.close()
        crt.delete()
        if (LdapName(c.subjectX500Principal.name).rdns[0].value.toString() != doc.author) throw MyException("Имя автора в документе не совпадает с именем в сертификате!")
        val s = Signature.getInstance(SHA1)
        s.initVerify(c.publicKey)
        s.update(doc.text.toByteArray(Charsets.UTF_8))
        if (!s.verify(doc.signature)) throw MyException("Цифровая подпись не прошла проверку")
    }

    private fun ByteArray.s() = (this.size - 128).toByte()
}