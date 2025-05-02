package com.jabaddon.glossary.model

import org.springframework.ai.document.Document
import java.util.UUID

data class GlossaryEntry(
    val id: UUID? = null,
    val term: String,
    val definition: String
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
                definition = metadata["definition"] as String
            )
        }
    }
}

data class SearchResults(
    val vectorResults: List<GlossaryEntry>,
    val sqlResults: List<GlossaryEntry>
)