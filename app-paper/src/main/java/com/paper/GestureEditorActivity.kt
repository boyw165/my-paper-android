package com.paper

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.MotionEvent
import android.widget.TextView
import com.my.widget.gesture.MyGestureDetector

class GestureEditorActivity : AppCompatActivity(),
                              MyGestureDetector.OnGestureListener,
                              MyGestureDetector.MyGestureListener {

    private val mGestureText: TextView by lazy {
        findViewById(R.id.text_gesture_test) as TextView
    }

    private val mGestureDetector: MyGestureDetector by lazy {
        MyGestureDetector(this@GestureEditorActivity, this, this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_my_gesture_editor)
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean =
        mGestureDetector.onTouchEvent(event, null, null)

    // GestureListener ------------------------------------------------------->

    override fun onDown(e: MotionEvent?): Boolean {
        return false
    }

    override fun onShowPress(e: MotionEvent?) {
        // DO NOTHING.
    }

    override fun onSingleTapUp(e: MotionEvent?): Boolean {
        return false
    }

    override fun onScroll(e1: MotionEvent?,
                          e2: MotionEvent?,
                          distanceX: Float,
                          distanceY: Float): Boolean {
        return false
    }

    override fun onLongPress(e: MotionEvent?) {
        // DO NOTHING.
    }

    override fun onFling(e1: MotionEvent?,
                         e2: MotionEvent?,
                         velocityX: Float,
                         velocityY: Float): Boolean {
        return false
    }

    // MyGestureListener ----------------------------------------------------->

    override fun onFingerDown(event: MotionEvent?, touchingScrap: Any?, touchContext: Any?) {
        // DO NOTHING.
    }

    override fun onFingerUpOrCancel(event: MotionEvent?, touchingScrap: Any?, touchContext: Any?) {
        // DO NOTHING.
    }

    override fun onSingleTap(event: MotionEvent?, touchingScrap: Any?, touchContext: Any?): Boolean {
        // DO NOTHING.
        return false
    }

    override fun onDoubleTap(event: MotionEvent?, touchingScrap: Any?, touchContext: Any?): Boolean {
        // DO NOTHING.
        return false
    }

    override fun onLongTap(event: MotionEvent?, touchingScrap: Any?, touchContext: Any?): Boolean {
        // DO NOTHING.
        return false
    }

    override fun onLongPress(event: MotionEvent?, touchingScrap: Any?, touchContext: Any?): Boolean {
        // DO NOTHING.
        return false
    }

    override fun onDragBegin(event: MotionEvent?, touchingScrap: Any?, touchContext: Any?, xInCanvas: Float, yInCanvas: Float): Boolean {
        // DO NOTHING.
        return false
    }

    override fun onDrag(event: MotionEvent?, touchingScrap: Any?, touchContext: Any?, translationInCanvas: FloatArray?) {
        // DO NOTHING.
    }

    override fun onDragEnd(event: MotionEvent?, touchingScrap: Any?, touchContext: Any?, translationInCanvas: FloatArray?) {
        // DO NOTHING.
    }

    override fun onFling(event: MotionEvent?, touchingScrap: Any?, touchContext: Any?, startPointerInCanvas: FloatArray?, stopPointerInCanvas: FloatArray?, velocityX: Float, velocityY: Float): Boolean {
        // DO NOTHING.
        return false
    }

    override fun onPinchBegin(event: MotionEvent?, touchingScrap: Any?, touchContext: Any?, pivotXInCanvas: Float, pivotYInCanvas: Float): Boolean {
        // DO NOTHING.
        return false
    }

    override fun onPinch(event: MotionEvent?, touchingScrap: Any?, touchContext: Any?, startPointerOneInCanvas: FloatArray?, startPointerTwoInCanvas: FloatArray?, stopPointerOneInCanvas: FloatArray?, stopPointerTwoInCanvas: FloatArray?) {
        // DO NOTHING.
    }

    override fun onPinchEnd(event: MotionEvent?, touchingScrap: Any?, touchContext: Any?, startPointerOneInCanvas: FloatArray?, startPointerTwoInCanvas: FloatArray?, stopPointerOneInCanvas: FloatArray?, stopPointerTwoInCanvas: FloatArray?) {
        // DO NOTHING.
    }
}
