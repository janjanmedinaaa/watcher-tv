package com.medina.juanantonio.watcher.network.deserializer

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.medina.juanantonio.watcher.network.models.player.EpisodeBean
import java.lang.reflect.Type

class DefinitionCodeDeserializer : JsonDeserializer<EpisodeBean.DefinitionCode> {

    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): EpisodeBean.DefinitionCode {
        return try {
            EpisodeBean.DefinitionCode.valueOf(json.asString)
        } catch (e: Exception) {
            EpisodeBean.DefinitionCode.UNKNOWN
        }
    }
}