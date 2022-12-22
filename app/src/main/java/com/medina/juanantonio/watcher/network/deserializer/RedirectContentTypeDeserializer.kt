package com.medina.juanantonio.watcher.network.deserializer

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.medina.juanantonio.watcher.network.models.home.NavigationItemBean
import java.lang.reflect.Type

class RedirectContentTypeDeserializer : JsonDeserializer<NavigationItemBean.RedirectContentType> {

    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): NavigationItemBean.RedirectContentType {
        return try {
            NavigationItemBean.RedirectContentType.valueOf(json.asString)
        } catch (e: Exception) {
            NavigationItemBean.RedirectContentType.UNKNOWN
        }
    }
}