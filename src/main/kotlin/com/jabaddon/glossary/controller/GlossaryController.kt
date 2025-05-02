package com.jabaddon.glossary.controller

import com.jabaddon.glossary.model.GlossaryEntry
import com.jabaddon.glossary.model.SearchResults
import com.jabaddon.glossary.service.GlossaryService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/glossary")
class GlossaryController(
    private val glossaryService: GlossaryService
) {
    @PostMapping("/upload")
    fun uploadGlossary(@RequestParam("file") file: MultipartFile): ResponseEntity<List<GlossaryEntry>> {
        val entries = glossaryService.processGlossaryCsv(file)
        return ResponseEntity.ok(entries)
    }

    @GetMapping("/search")
    fun searchTerms(
        @RequestParam("query") query: String,
        @RequestParam(value = "limit", defaultValue = "5") limit: Int
    ): ResponseEntity<SearchResults> {
        val results = glossaryService.searchSimilarTerms(query, limit)
        return ResponseEntity.ok(results)
    }
}