package com.example.projektmunka

import android.os.AsyncTask
import android.util.Log
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class NominatimReverseGeocoding(private val callback: (Pair<Double, Double>?) -> Unit) :
    AsyncTask<String, Void, Pair<Double, Double>>() {

    override fun doInBackground(vararg params: String): Pair<Double, Double>? {
        if (params.isEmpty()) {
            return null
        }

        val partialAddress = params[0]

        // Construct the URL for Nominatim autocomplete
        val apiUrl =
            "https://nominatim.openstreetmap.org/search?q=$partialAddress&format=json&limit=1"
        val url = URL(apiUrl)

        // Open connection
        val connection: HttpURLConnection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"

        // Read response
        val reader = BufferedReader(InputStreamReader(connection.inputStream))
        val response = StringBuilder()
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            response.append(line)
        }
        reader.close()

        // Parse JSON response
        return parseJsonResponse(response.toString())
    }

    override fun onPostExecute(result: Pair<Double, Double>?) {
        super.onPostExecute(result)

        // Pass the result to the callback function
        callback(result)
    }

    private fun parseJsonResponse(jsonResponse: String): Pair<Double, Double>? {
        try {
            val jsonArray = JSONArray(jsonResponse)

            if (jsonArray.length() > 0) {
                val firstResult = jsonArray.getJSONObject(0)
                val lat = firstResult.getDouble("lat")
                val lon = firstResult.getDouble("lon")

                return Pair(lat, lon)
            }
        } catch (e: Exception) {
            Log.e("NominatimAutocomplete", "Error parsing JSON response", e)
        }

        return null
    }
}