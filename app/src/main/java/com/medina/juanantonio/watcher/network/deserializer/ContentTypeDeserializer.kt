package com.medina.juanantonio.watcher.network.deserializer

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.medina.juanantonio.watcher.network.models.home.HomePageBean
import java.lang.reflect.Type

class ContentTypeDeserializer : JsonDeserializer<HomePageBean.ContentType> {

    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): HomePageBean.ContentType {
        return try {
            HomePageBean.ContentType.valueOf(json.asString)
        } catch (e: Exception) {
            HomePageBean.ContentType.UNKNOWN
        }
    }
}