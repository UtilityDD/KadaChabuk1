package com.blackgrapes.kadachabuk

import kotlinx.serialization.Serializable // Import

@Serializable // Add this annotation
data class Chapter(
    val heading: String,
    val date: String?,
    val writer: String,
    val data: String,
    val serial: String,
    val version: String
)
