package com.flexa.identity.create_id

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.IOException
import java.io.InputStream
import java.nio.charset.Charset

class ResidenceProvider {


    fun getResidences(context: Context): List<Residence> {
        val res: List<Residence>
        val raw = loadJSONFromAsset(context)
        res = if (raw == null)
            ArrayList(0)
        else {
            val type = object : TypeToken<ArrayList<Residence>>() {}.type
            Gson().fromJson(raw, type)
        }
        return res
    }


    private fun loadJSONFromAsset(context: Context): String? {
        val json: String?
        var `is`: InputStream? = null
        json = try {
            `is` = context.assets.open("administrative-division-suggestions.json")
            val size: Int = `is`.available()
            val buffer = ByteArray(size)
            `is`.read(buffer)
            String(buffer, Charset.defaultCharset())
        } catch (ex: IOException) {
            Log.w(ResidenceProvider::class.java.simpleName, ex.message, ex)
            return null
        } finally {
            `is`?.run {
                try {
                    close()
                } catch (ex: IOException) {
                    Log.w(ResidenceProvider::class.java.simpleName, ex.message, ex)
                }
            }
        }
        return json
    }
}
