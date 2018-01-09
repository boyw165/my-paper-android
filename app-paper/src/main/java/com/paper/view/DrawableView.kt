package com.paper.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.util.AttributeSet
import android.view.View

class DrawableView : View {

    private val mDots: MutableList<PointF> = mutableListOf()

    // Paint.
    private val mDotPaint: Paint by lazy {
        val p = Paint()
        p.style = Paint.Style.STROKE
        p.color = Color.BLACK
        p.strokeWidth = 15f
        p
    }

    constructor(context: Context)
        : this(context, null)

    constructor(context: Context?,
                attrs: AttributeSet?)
        : this(context, attrs, 0)

    constructor(context: Context?,
                attrs: AttributeSet?,
                defStyleAttr: Int)
        : super(context, attrs, defStyleAttr)

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val r = mDotPaint.strokeWidth / 2f;
        for (p in mDots) {
            canvas.drawCircle(p.x, p.y, r, mDotPaint)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int,
                           heightMeasureSpec: Int) {
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec),
                             MeasureSpec.getSize(heightMeasureSpec))
    }

    fun addDot(p: PointF) {
        mDots.add(p)
        postInvalidateDelayed(150)
    }

    fun removeDot(p: PointF) {
        mDots.remove(p)
        postInvalidateDelayed(150)
    }

    fun clearAllDots() {
       mDots.clear()
        postInvalidateDelayed(150)
    }
}
