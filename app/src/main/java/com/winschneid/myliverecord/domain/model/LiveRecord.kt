package com.winschneid.myliverecord.domain.model

data class LiveRecord(
    val id: Long = 0,
    val title: String = "",
    val artistNames: List<String>,
    val venueName: String,
    val seatNumber: String,
    val date: Long,
    val memo: String = "",
    val ticketPrice: Long? = null,
)
