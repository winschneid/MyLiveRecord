package com.example.myliverecord.domain.model

data class LiveRecord(
    val id: Long = 0,
    val artistName: String,
    val venueName: String,
    val seatNumber: String,
    val date: Long,
)
