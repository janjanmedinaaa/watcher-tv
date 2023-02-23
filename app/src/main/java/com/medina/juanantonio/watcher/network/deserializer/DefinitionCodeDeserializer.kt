package com.medina.juanantonio.watcher.network.deserializer

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.medina.juanantonio.watcher.network.models.player.Definition
import java.lang.reflect.Type

class DefinitionCodeDeserializer : JsonDeserializer<Definition.DefinitionCode> {

    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): Definition.DefinitionCode {
        return try {
            Definition.DefinitionCode.valueOf(json.asString)
        } catch (e: Exception) {
            Definition.DefinitionCode.UNKNOWN
        }
    }
}