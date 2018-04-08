package com.paper.editor.data

data class UpdateEditingToolsEvent(
    val toolIDs: List<Int>,
    val usingIndex: Int = -1)
