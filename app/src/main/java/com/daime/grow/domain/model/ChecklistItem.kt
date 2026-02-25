package com.daime.grow.domain.model

data class ChecklistItem(
    val id: Long = 0,
    val plantId: Long,
    val phase: String,
    val task: String,
    val done: Boolean,
    val createdAt: Long
)

