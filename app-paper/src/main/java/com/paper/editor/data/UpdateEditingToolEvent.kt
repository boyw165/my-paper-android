package com.paper.editor.data

data class UpdateEditingToolEvent(
    val toolIDs: List<Int>,
    val usingIndex: Int = -1)
