package com.blackgrapes.kadachabuk

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents a chapter in the application.
 * This class is an Entity for the Room database, meaning it defines a table named "chapters".
 */
@Entity(
    tableName = "chapters",
    // Creates a unique index on the combination of 'languageCode' and 'serial'.
    // This prevents duplicate entries for the same chapter within the same language.
    indices = [Index(value = ["languageCode", "serial"], unique = true)]
)
data class Chapter(
    /**
     * The auto-generated primary key for this chapter in the database.
     */
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0,

    /**
     * The language code for this chapter (e.g., "en", "bn", "hi").
     * Must be set before inserting the chapter into the database.
     */
    var languageCode: String = "",

    /**
     * The main heading or title of the chapter.
     */
    val heading: String,

    /**
     * The publication date or relevant date for the chapter.
     */
    val date: String?,

    /**
     * The author or writer of the chapter.
     */
    val writer: String,

    /**
     * The main content or detailed text of the chapter.
     */
    val dataText: String,

    /**
     * A serial number or unique identifier for the chapter within its language.
     */
    val serial: String,

    /**
     * The version of this chapter's content.
     */
    val version: String
)
