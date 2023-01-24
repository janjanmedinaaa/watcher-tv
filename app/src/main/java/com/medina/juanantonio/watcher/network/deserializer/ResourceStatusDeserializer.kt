package com.medina.juanantonio.watcher.network.deserializer

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.medina.juanantonio.watcher.network.models.home.HomePageBean
import java.lang.reflect.Type

class ResourceStatusDeserializer : JsonDeserializer<HomePageBean.ResourceStatus> {

    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): HomePageBean.ResourceStatus {
        return try {
            when (json.asString) {
                "1" -> HomePageBean.ResourceStatus.UPDATED
                "2" -> HomePageBean.ResourceStatus.TOTAL
                else -> HomePageBean.ResourceStatus.UNKNOWN
            }
        } catch (e: Exception) {
            HomePageBean.ResourceStatus.UNKNOWN
        }
    }
}