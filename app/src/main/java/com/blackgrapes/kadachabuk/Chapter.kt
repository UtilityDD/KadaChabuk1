package com.blackgrapes.kadachabuk

data class Chapter(
    val heading: String,
    val date: String?, // Nullable as it can be blank
    val writer: String,
    val data: String,
    val serial: String,
    val version: String
)