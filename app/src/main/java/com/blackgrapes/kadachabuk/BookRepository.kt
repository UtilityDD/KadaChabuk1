package com.blackgrapes.kadachabuk

import android.util.Log
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader // Import kotlin-csv

class BookRepository {

    private val apiService = RetrofitInstance.api

    // Sheet URLs remain the same
    private val sheetUrls = mapOf(
        "bn" to "https://docs.google.com/spreadsheets/d/e/2PACX-1vRztE9nSnn54KQxwLlLMNgk-v1QjfC-AVy35OyBZPFssRt1zSkgrdX1Xi92oW9i3pkx4HV4AZjclLzF/pub?gid=0&single=true&output=csv",
        "hi" to "https://docs.google.com/spreadsheets/d/e/2PACX-1vRztE9nSnn54KQxwLlLMNgk-v1QjfC-AVy35OyBZPFssRt1zSkgrdX1Xi92oW9i3pkx4HV4AZjclLzF/pub?gid=1815418271&single=true&output=csv",
        "en" to "https://docs.google.com/spreadsheets/d/e/2PACX-1vRztE9nSnn54KQxwLlLMNgk-v1QjfC-AVy35OyBZPFssRt1zSkgrdX1Xi92oW9i3pkx4HV4AZjclLzF/pub?gid=680564409&single=true&output=csv",
        "as" to "https://docs.google.com/spreadsheets/d/e/2PACX-1vRztE9nSnn54KQxwLlLMNgk-v1QjfC-AVy35OyBZPFssRt1zSkgrdX1Xi92oW9i3pkx4HV4AZjclLzF/pub?gid=1703268117&single=true&output=csv",
        "od" to "https://docs.google.com/spreadsheets/d/e/2PACX-1vRztE9nSnn54KQxwLlLMNgk-v1QjfC-AVy35OyBZPFssRt1zSkgrdX1Xi92oW9i3pkx4HV4AZjclLzF/pub?gid=217039634&single=true&output=csv",
        "tm" to "https://docs.google.com/spreadsheets/d/e/2PACX-1vRztE9nSnn54KQxwLlLMNgk-v1QjfC-AVy35OyBZPFssRt1zSkgrdX1Xi92oW9i3pkx4HV4AZjclLzF/pub?gid=1124108521&single=true&output=csv"
    )

    suspend fun getChapters(languageCode: String): List<Chapter> {
        val url = sheetUrls[languageCode] ?: run {
            Log.w("BookRepository", "No URL found for language code: $languageCode")
            return emptyList()
        }
        try {
            val response = apiService.getBookCsvData(url)
            if (response.isSuccessful && response.body() != null) {
                return parseCsvWithKotlinCsv(response.body()!!)
            } else {
                Log.e("BookRepository", "Error fetching data for $languageCode: ${response.code()} - ${response.message()}")
            }
        } catch (e: Exception) {
            Log.e("BookRepository", "Exception fetching data for $languageCode: ${e.message}", e)
        }
        return emptyList()
    }

// In BookRepository.kt

// ... (other imports and class definition) ...

    private fun parseCsvWithKotlinCsv(csvString: String): List<Chapter> {
        val chapters = mutableListOf<Chapter>()
        if (csvString.isBlank()) {
            Log.w("BookRepository", "CSV string is blank, cannot parse.")
            return emptyList()
        }

        try {
            // Use the String directly with the csvReader
            val rows: List<Map<String, String>> = csvReader {
                // You can set options here if needed, e.g.:
                // skipEmptyLine = true
                // escapeChar = '\\'
                // quoteChar = '"'
                // delimiter = ','
            }.readAllWithHeader(csvString) // Pass the csvString directly

            if (rows.isEmpty()) {
                Log.w("BookRepository", "kotlin-csv parsed with zero data rows (after header). Check CSV content.")
            }

            rows.forEachIndexed { index, row ->
                val heading = row["heading"]?.trim()
                val dateStr = row["date"]?.trim()
                val writer = row["writer"]?.trim()
                val dataContent = row["data"]?.trim()
                val serial = row["serial"]?.trim()
                val version = row["version"]?.trim()

                if (heading.isNullOrBlank()) {
                    Log.w("BookRepository", "Skipping row ${index + 1} due to missing or blank 'heading'. Row data: $row")
                    return@forEachIndexed
                }
                if (writer.isNullOrBlank()) {
                    Log.w("BookRepository", "Skipping row ${index + 1} (Heading: '$heading') due to missing or blank 'writer'. Row data: $row")
                    return@forEachIndexed
                }
                if (dataContent.isNullOrBlank() || serial.isNullOrBlank() || version.isNullOrBlank()) {
                    Log.w("BookRepository", "Row ${index + 1} (Heading: '$heading') has missing data for 'data', 'serial', or 'version'. Row data: $row")
                }

                val date = if (dateStr.isNullOrBlank()) null else dateStr

                chapters.add(
                    Chapter(
                        heading = heading,
                        date = date,
                        writer = writer,
                        data = dataContent ?: "",
                        serial = serial ?: "",
                        version = version ?: ""
                    )
                )
            }
        } catch (e: Exception) {
            Log.e("BookRepository", "Error parsing CSV with kotlin-csv: ${e.message}", e)
            // Log.d("BookRepository", "Problematic CSV string: $csvString")
        }

        if (chapters.isEmpty() && csvString.split("\n").getOrElse(1) { "" }.isNotBlank()) {
            Log.w("BookRepository", "kotlin-csv parsing resulted in zero chapters, but data lines seemed to exist (after header). Ensure CSV column names ('heading', 'date', 'writer', 'data', 'serial', 'version') exactly match your sheet. Also check for unexpected empty rows after the header.")
        }
        return chapters
    }

}
