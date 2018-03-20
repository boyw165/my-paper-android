package com.paper.shared.model.sketch

class Scrap constructor(id: Long,
                        width: Int,
                        height: Int) {

    private val mMutex = Any()

    val id: Long = id

    var width: Int = width
        get() = field
    var height: Int = height
        get() = field

    var sketch: SketchModel? = null

    constructor() : this(0, 0, 0)

    fun setSize(width: Int, height: Int) {
        synchronized(mMutex) {
            this.width = width
            this.height = height
        }
    }

    override fun toString(): String {
        return "SketchModel{" +
               ", width=" + width +
               ", height=" + height +
               '}'
    }
}
