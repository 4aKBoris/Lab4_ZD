package mpei.lab3

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.databind.ObjectMapper
import org.json.JSONObject

@JsonPropertyOrder("name", "author", "cert", "sign", "text")
class Document() {

    @JsonProperty("name")
    var name = ""

    @JsonProperty("author")
    var author =""

    @JsonProperty("cert")
    var certificate = byteArrayOf()

    @JsonProperty("sign")
    var signature = byteArrayOf()

    @JsonProperty("text")
    var text = ""

    constructor(name: String, author: String, certificate: ByteArray, signature: ByteArray, text: String) : this() {
        this.name = name
        this.author = author
        this.certificate = certificate
        this.signature = signature
        this.text = text
    }

    @JsonIgnore
    fun convertToJson() = mapper.writeValueAsString(this)!!

    @JsonIgnore
    fun convertToObject(json: JSONObject): Document = mapper.readValue(json.toString(), Document::class.java)

    companion object {
        private val mapper = ObjectMapper()
    }
}