package com.example.myliverecord.data.transfer

import com.example.myliverecord.domain.model.LiveRecord
import org.json.JSONArray
import org.json.JSONObject

/**
 * バックアップ用のJSON形式。
 * {
 *   "version": 1,
 *   "records": [
 *     { "title": "", "artists": ["..."], "venue": "...", "seat": "...",
 *       "date": <epoch millis>, "memo": "", "ticketPrice": <Long or null> }
 *   ]
 * }
 */
object LiveRecordsJson {

    const val FORMAT_VERSION = 1

    fun encode(records: List<LiveRecord>): String {
        val array = JSONArray()
        records.forEach { record ->
            array.put(
                JSONObject().apply {
                    put("title", record.title)
                    put("artists", JSONArray(record.artistNames))
                    put("venue", record.venueName)
                    put("seat", record.seatNumber)
                    put("date", record.date)
                    put("memo", record.memo)
                    put("ticketPrice", record.ticketPrice ?: JSONObject.NULL)
                }
            )
        }
        return JSONObject()
            .put("version", FORMAT_VERSION)
            .put("records", array)
            .toString(2)
    }

    /** 不正なJSONや必須項目欠落時は IllegalArgumentException / JSONException を投げる */
    fun decode(text: String): List<LiveRecord> {
        val root = JSONObject(text)
        val array = root.getJSONArray("records")
        return (0 until array.length()).map { i ->
            val obj = array.getJSONObject(i)
            val artistArray = obj.getJSONArray("artists")
            val artists = (0 until artistArray.length())
                .map { artistArray.getString(it).trim() }
                .filter { it.isNotEmpty() }
            require(artists.isNotEmpty()) { "record[$i]: artists is empty" }
            LiveRecord(
                title = obj.optString("title", ""),
                artistNames = artists,
                venueName = obj.getString("venue"),
                seatNumber = obj.optString("seat", ""),
                date = obj.getLong("date"),
                memo = obj.optString("memo", ""),
                ticketPrice = if (obj.isNull("ticketPrice")) null else obj.getLong("ticketPrice"),
            )
        }
    }
}
