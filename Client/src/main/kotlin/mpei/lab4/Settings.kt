package mpei.lab4

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.core.JsonGenerationException
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper
import java.io.File

@JsonPropertyOrder("ip", "port", "documents")
class Settings() {

    @JsonProperty("ip")
    var ip = "127.0.0.1"

    @JsonProperty("port")
    var port = 8289

    @JsonProperty("documents")
    var documents = mutableListOf<String>()

    constructor(ip: String, port: Int, documents: MutableList<String>): this() {
        this.ip = ip
        this.port = port
        this.documents = documents
    }

    @JsonIgnore
    fun createDefaultSettings() = Settings("127.0.0.1", 8289, mutableListOf())

}