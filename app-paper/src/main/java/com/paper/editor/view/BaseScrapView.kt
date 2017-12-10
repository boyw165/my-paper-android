package com.paper.editor.view

import android.content.Context
import android.graphics.Color
import android.support.v4.content.ContextCompat
import android.support.v7.widget.AppCompatImageView
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.FrameLayout
import android.widget.ImageView
import com.cardinalblue.gesture.GestureDetector
import com.paper.R
import com.paper.editor.PaperCanvasContract
import com.paper.editor.TwoDTransformUtils
import com.paper.shared.model.TransformModel

class BaseScrapView : FrameLayout,
                      PaperCanvasContract.BaseView {

    // TODO: Make it a FrameLayout instead.
    // Scraps container.
    private val mContainer: AppCompatImageView by lazy {
        AppCompatImageView(context)
    }

    // Gesture.
    private val mTransformHelper: TwoDTransformUtils = TwoDTransformUtils()
    private val mTransformModel: TransformModel = TransformModel(0f, 0f, 1f, 1f, 0f)
    private var mGestureDetector: GestureDetector? = null

    constructor(context: Context?) : super(context)

    constructor(context: Context?,
                attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context?,
                attrs: AttributeSet?,
                defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    constructor(context: Context?,
                attrs: AttributeSet?,
                defStyleAttr: Int,
                defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        // Testing code that makes the container 500x500 dp.
        val oneDp = resources.getDimension(R.dimen.one_dp)
        mContainer.layoutParams = generateDefaultLayoutParams()
        mContainer.layoutParams.width = (320f * oneDp).toInt()
        mContainer.layoutParams.height = (240f * oneDp).toInt()
        mContainer.setBackgroundColor(Color.BLUE)
        mContainer.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.img_android))
        mContainer.scaleType = ImageView.ScaleType.CENTER_CROP

        addView(mContainer)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()

        removeView(mContainer)
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        // TODO: Solve the hitting problem.

        // If the canvas don't handle the touch, bubble up the event.
        return (mGestureDetector?.onTouchEvent(event, this, 0) ?: false) ||
               super.onTouchEvent(event)
    }

    override fun getTransform(): TransformModel {
        // Get the transform information from the view matrix.
        mTransformHelper.getValues(matrix)

        mTransformModel.translationX = mTransformHelper.translationX
        mTransformModel.translationY = mTransformHelper.translationY
        mTransformModel.scaleX = mTransformHelper.scaleX
        mTransformModel.scaleY = mTransformHelper.scaleY
        mTransformModel.rotationInRadians = mTransformHelper.rotationInRadians

        return mTransformModel.copy()
    }

    override fun setTransform(other: TransformModel) {
        mTransformModel.translationX = other.translationX
        mTransformModel.translationY = other.translationY
        mTransformModel.scaleX = other.scaleX
        mTransformModel.scaleY = other.scaleY
        mTransformModel.rotationInRadians = other.rotationInRadians

        scaleX = mTransformModel.scaleX
        scaleY = mTransformModel.scaleY
        rotation = Math.toDegrees(mTransformModel.rotationInRadians.toDouble()).toFloat()
        translationX = mTransformModel.translationX
        translationY = mTransformModel.translationY
    }

    override fun setInterceptTouchEvent(enabled: Boolean) {
        // DO NOTHING.
    }

    override fun setGestureDetector(detector: GestureDetector?) {
        mGestureDetector = detector
    }
}
