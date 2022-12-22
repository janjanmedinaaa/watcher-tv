package com.medina.juanantonio.watcher.network.deserializer

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.medina.juanantonio.watcher.network.models.home.HomePageBean
import java.lang.reflect.Type

class SectionTypeDeserializer : JsonDeserializer<HomePageBean.SectionType> {

    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): HomePageBean.SectionType {
        return try {
            HomePageBean.SectionType.valueOf(json.asString)
        } catch (e: Exception) {
            HomePageBean.SectionType.UNKNOWN
        }
    }
}