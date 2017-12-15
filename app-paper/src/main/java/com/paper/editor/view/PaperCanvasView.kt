package com.paper.editor.view

import android.content.Context
import android.graphics.Color
import android.graphics.Matrix
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.FrameLayout
import com.cardinalblue.gesture.GestureDetector
import com.paper.editor.PaperCanvasContract
import com.paper.editor.TwoDTransformUtils
import com.paper.shared.model.TransformModel

class PaperCanvasView : FrameLayout,
                        PaperCanvasContract.BaseView {

    // Gesture.
    private var mGestureDetector: GestureDetector? = null

    constructor(context: Context?) : super(context)

    constructor(context: Context?,
                attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context?,
                attrs: AttributeSet?,
                defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        // Changing the pivot to left-top at the beginning.
        // Note: Changing the pivot will update the rendering matrix, where it
        // is like making the parent see the child in a different angles.
        pivotX = 0f
        pivotY = 0f

        // TEST
        translationX = -360f
        translationY = -360f
        scaleX = 1.5f
        scaleY = 1.5f
        rotation = -50f
        setBackgroundColor(Color.RED)
    }

    override fun convertPointToParentWorld(point: FloatArray) {
        matrix.mapPoints(point)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        // TODO: Solve the hitting problem.

        // If the canvas don't handle the touch, bubble up the event.
        return (mGestureDetector?.onTouchEvent(event, this, 0) ?: false) ||
               super.onTouchEvent(event)
    }

    override fun getTransform(): TransformModel {
        return TransformModel(
            translationX = this.translationX,
            translationY = this.translationY,
            scaleX = this.scaleX,
            scaleY = this.scaleY,
            rotationInRadians = Math.toRadians(this.rotation.toDouble()).toFloat())
    }

    override fun getTransformMatrix(): Matrix {
        return matrix
    }

    override fun setTransform(transform: TransformModel) {
//        Log.d("xyz", "pivot x=%.3f, y=%.3f".format(pivotX, pivotY))
//        this.pivotX = pivotX
//        this.pivotY = pivotY

        // Reset the translation so that the following scale and rotation
        // transform works without the bias.
//        this.translationX = 0f
//        this.translationY = 0f

        this.rotation = Math.toDegrees(transform.rotationInRadians.toDouble()).toFloat()
        this.scaleX = transform.scaleX
        this.scaleY = transform.scaleY
        this.translationX = transform.translationX
        this.translationY = transform.translationY
    }

    override fun setTransformPivot(pivotX: Float, pivotY: Float) {
        this.pivotX = pivotX
        this.pivotY = pivotY
    }

    override fun setInterceptTouchEvent(enabled: Boolean) {
        // DO NOTHING.
    }

    override fun setGestureDetector(detector: GestureDetector?) {
        mGestureDetector = detector
    }
}
