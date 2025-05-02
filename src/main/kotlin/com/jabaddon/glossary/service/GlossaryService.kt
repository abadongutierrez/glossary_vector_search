package com.jabaddon.glossary.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.jabaddon.glossary.model.GlossaryEntry
import com.jabaddon.glossary.model.SearchResult
import com.jabaddon.glossary.model.SearchResults
import org.springframework.ai.vectorstore.SearchRequest
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.BufferedReader
import java.io.InputStreamReader
import java.sql.ResultSet
import java.util.UUID

@Service
class GlossaryService(
    private val vectorStore: VectorStore,
    private val jdbcTemplate: JdbcTemplate,
) {
    // Jackson ObjectMapper for JSON parsing
    private val objectMapper = jacksonObjectMapper()

    fun processGlossaryCsv(file: MultipartFile): List<GlossaryEntry> {
        val entries = mutableListOf<GlossaryEntry>()

        BufferedReader(InputStreamReader(file.inputStream)).use { reader ->
            reader.readLine() // Skip header if exists

            reader.forEachLine { line ->
                val (term, definition) = line.split(",", limit = 2)
                entries.add(
                    GlossaryEntry(
                        id = UUID.randomUUID(),
                        term = term.trim(),
                        definition = definition.trim()
                    )
                )
            }
        }

        // Generate embeddings and store in vector database
        entries.windowed(50, 50, true).forEachIndexed { _, chunk ->
            vectorStore.add(chunk.map { it.toDocument() })
        }

        return entries
    }

    fun searchSimilarTerms(query: String, limit: Int = 5): SearchResults {
        // Vector-based similarity search 
        val similarDocuments = vectorStore.similaritySearch(
            SearchRequest.builder().query(query).topK(limit).build()
        )
        val vectorResults = similarDocuments?.map { GlossaryEntry.fromDocument(it) } ?: emptyList()

        // Direct SQL search for content similarity
        val sqlResults = jdbcTemplate.query(
            """
            SELECT content, metadata 
            FROM vector_store 
            WHERE content ILIKE ? 
            LIMIT ?
            """,
            toGlossaryEntryMapper(),
            "%$query%",
            limit
        )
        val sqlIds = sqlResults.associate { it.id to 0.0f }
        val vectorIds = vectorResults.associate { it.id to it.distance }

        val finalResult = (vectorResults + sqlResults).distinctBy { it.id }

        return SearchResults(finalResult.map {
            SearchResult(
                it,
                distance = if (it.id in vectorIds) vectorIds[it.id] else sqlIds[it.id],
                type = when {
                    it.id in sqlIds && it.id in vectorIds -> listOf("vector", "sql")
                    it.id in sqlIds -> listOf("sql")
                    it.id in vectorIds -> listOf("vector")
                    else -> emptyList()
                }
            )
        })
    }

    private fun toGlossaryEntryMapper() = { rs: ResultSet, _: Int ->
        val content = rs.getString("content") ?: ""
        val metadataStr = rs.getString("metadata") ?: "{}"

        // Parse metadata string to Map
        val metadata: Map<String, Any> = try {
            objectMapper.readValue(metadataStr)
        } catch (e: Exception) {
            mapOf()
        }

        // Extract information preferring metadata map over content parsing
        val term = metadata["term"]?.toString() ?: if (content.contains(":")) {
            content.substringBefore(":").trim()
        } else {
            ""
        }

        val definition = metadata["definition"]?.toString() ?: if (content.contains(":")) {
            content.substringAfter(":").trim()
        } else {
            content
        }

        GlossaryEntry(
            id = metadata["id"]?.let { UUID.fromString(it as String) },
            term = term,
            definition = definition
        )
    }
}