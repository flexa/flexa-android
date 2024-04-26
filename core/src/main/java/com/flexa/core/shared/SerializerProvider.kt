package com.flexa.core.shared

import com.google.gson.Gson


class SerializerProvider {


    val json by lazy(LazyThreadSafetyMode.NONE) {
        Gson()
    }
}
