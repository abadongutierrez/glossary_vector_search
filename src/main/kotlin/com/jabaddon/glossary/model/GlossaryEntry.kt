package com.jabaddon.glossary.model

import org.springframework.ai.document.Document
import java.util.UUID

data class GlossaryEntry(
    val id: UUID? = null,
    val term: String,
    val definition: String,
    val distance: Float? = null
) {
    fun toDocument(): Document {
        val content = "$term: $definition"
        return Document(
            content,
            mapOf(
                "term" to term,
                "definition" to definition,
                "id" to id?.toString()
            )
        )
    }

    companion object {
        fun fromDocument(document: Document): GlossaryEntry {
            val metadata = document.metadata
            return GlossaryEntry(
                id = metadata["id"]?.let { UUID.fromString(it as String) },
                term = metadata["term"] as String,
                definition = metadata["definition"] as String,
                distance = metadata["distance"] as? Float
            )
        }
    }
}

data class SearchResult(
    val glossaryEntry: GlossaryEntry,
    val distance: Float?,
    val type: List<String>
)

data class SearchResults(
    val results: List<SearchResult>
)