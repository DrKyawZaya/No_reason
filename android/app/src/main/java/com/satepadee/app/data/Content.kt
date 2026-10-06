package com.satepadee.app.data

import android.content.Context
import androidx.compose.ui.graphics.Color
import org.json.JSONArray
import org.json.JSONObject

data class Guna(val number: Int, val pali: String, val meaning: List<String>)

data class Wood(val id: String, val name: String, val day: String, val hi: Color, val mid: Color, val lo: Color, val glow: Color)

data class BirthDay(val id: String, val name: String)

data class Content(
    val kozawinName: String,
    val kozawinSummary: String,
    val gunas: List<Guna>,
    val rules: List<String>,
    val stageBenefits: List<String>,
    val woodNote: String,
    val birthDays: List<BirthDay>,
    val woods: List<Wood>,
) {
    fun guna(number: Int): Guna = gunas.first { it.number == number }
    fun birthDayName(id: String?): String = birthDays.firstOrNull { it.id == id }?.name.orEmpty()

    companion object {
        /** Reads content/kozawin.json and content/woods.json, packaged as assets. */
        fun load(context: Context): Content {
            val k = JSONObject(context.assets.open("kozawin.json").bufferedReader().use { it.readText() })
            val w = JSONObject(context.assets.open("woods.json").bufferedReader().use { it.readText() })
            val gunas = k.getJSONArray("gunas").objects().map {
                Guna(it.getInt("number"), it.getString("pali"), it.getJSONArray("meaning").strings())
            }
            val woods = w.getJSONArray("woods").objects().map {
                val c = it.getJSONObject("colors")
                Wood(
                    id = it.getString("id"), name = it.getString("name"), day = it.getString("day"),
                    hi = c.color("hi"), mid = c.color("mid"), lo = c.color("lo"), glow = c.color("glow"),
                )
            }
            return Content(
                kozawinName = k.getString("name"),
                kozawinSummary = k.getString("summary"),
                gunas = gunas,
                rules = k.getJSONArray("rules").strings(),
                stageBenefits = k.getJSONArray("stageBenefits").strings(),
                woodNote = w.getString("note"),
                birthDays = w.getJSONArray("birthDays").objects().map { BirthDay(it.getString("id"), it.getString("name")) },
                woods = woods,
            )
        }

        private fun JSONArray.objects() = (0 until length()).map { getJSONObject(it) }
        private fun JSONArray.strings() = (0 until length()).map { getString(it) }
        private fun JSONObject.color(key: String) = Color(android.graphics.Color.parseColor(getString(key)))
    }
}
