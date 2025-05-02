package com.jabaddon.glossary.service

import com.jabaddon.glossary.model.GlossaryEntry
import com.jabaddon.glossary.model.SearchResults
import org.springframework.ai.vectorstore.SearchRequest
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.*
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue

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
                entries.add(GlossaryEntry(
                    id = UUID.randomUUID(),
                    term = term.trim(),
                    definition = definition.trim()
                ))
            }
        }
        
        // Generate embeddings and store in vector database
        entries.forEach { entry -> 
            val document = entry.toDocument()
            vectorStore.add(mutableListOf(document))
        }
        
        return entries
    }

    fun searchSimilarTerms(query: String, limit: Int = 5): SearchResults {
        // Vector-based similarity search 
        val similarDocuments = vectorStore.similaritySearch(
            SearchRequest.builder().query(query).topK(limit).build())
        val vectorResults = similarDocuments?.map { GlossaryEntry.fromDocument(it) } ?: emptyList()
        
        // Direct SQL search for content similarity
        val sqlResults = jdbcTemplate.query(
            """
            SELECT id, content, metadata 
            FROM vector_store 
            WHERE content ILIKE ? 
            LIMIT ?
            """,
            { rs, _ ->
                val id = rs.getString("id")?.let { UUID.fromString(it) }
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
                    id = id,
                    term = term,
                    definition = definition
                )
            },
            "%$query%",
            limit
        )
        
        // Return both collections in the SearchResults data class
        return SearchResults(vectorResults = vectorResults, sqlResults = sqlResults)
    }
}