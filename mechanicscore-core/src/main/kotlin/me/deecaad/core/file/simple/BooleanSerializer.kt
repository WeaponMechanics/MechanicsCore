package me.deecaad.core.file.simple

import me.deecaad.core.file.SerializerException
import me.deecaad.core.file.ErrorLocation
import me.deecaad.core.file.SimpleSerializer

class BooleanSerializer : SimpleSerializer<Boolean> {
    override fun getTypeName(): String = "true/false"

    override fun deserialize(
        data: String,
        errorLocation: ErrorLocation,
    ): Boolean {
        return when (data.lowercase()) {
            "true" -> true
            "false" -> false
            else -> throw SerializerException.Builder()
                .located(errorLocation)
                .buildInvalidType("true/false", data)
        }
    }

    override fun examples(): MutableList<String> {
        return mutableListOf("true", "false")
    }
}
