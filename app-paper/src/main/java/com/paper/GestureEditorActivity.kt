package com.paper

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.MotionEvent
import android.widget.TextView
import com.my.widget.gesture.MyGestureDetector

class GestureEditorActivity : AppCompatActivity(),
                              MyGestureDetector.OnGestureListener {

    private val mGestureText: TextView by lazy {
        findViewById(R.id.text_gesture_test) as TextView
    }

    private val mGestureDetector: MyGestureDetector by lazy {
        MyGestureDetector(this@GestureEditorActivity, this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_my_gesture_editor)
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean =
        mGestureDetector.onTouchEvent(event)

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

    // GestureListener <-------------------------------------------------------
}
