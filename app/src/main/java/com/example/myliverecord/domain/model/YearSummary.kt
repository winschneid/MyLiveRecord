package com.example.myliverecord.domain.model

data class ArtistCount(
    val artistName: String,
    val count: Int,
)

data class YearSummary(
    val year: Int,
    val totalCount: Int,
    val artists: List<ArtistCount>,
)
