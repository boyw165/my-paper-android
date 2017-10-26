package com.paper

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.MotionEvent
import android.widget.ImageView
import android.widget.TextView
import com.jakewharton.rxbinding2.view.RxView
import com.my.widget.gesture.MyGestureDetector

class GestureEditorActivity : AppCompatActivity(),
                              MyGestureDetector.MyGestureListener {

    private val mLog: MutableList<String> = mutableListOf()

    private val mBtnClearLog: ImageView by lazy {
        findViewById(R.id.btn_clear) as ImageView
    }
    private val mTxtLog: TextView by lazy {
        findViewById(R.id.text_gesture_test) as TextView
    }

    private val mGestureDetector: MyGestureDetector by lazy {
        MyGestureDetector(this@GestureEditorActivity, this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_my_gesture_editor)

        RxView.clicks(mBtnClearLog)
            .subscribe { _ ->
                clearLog()
            }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean =
        mGestureDetector.onTouchEvent(event, null, null)

    // MyGestureListener ----------------------------------------------------->

    override fun onActionDown(event: MyGestureDetector.MyMotionEvent,
                              touchingObject: Any?,
                              touchingContext: Any?) {
        printLog("--------------")
        printLog("⬇onActionDown")
    }

    override fun onActionUpOrCancel(event: MyGestureDetector.MyMotionEvent,
                                    touchingObject: Any?,
                                    touchingContext: Any?,
                                    isCancel: Boolean) {
        printLog("⬆onActionUpOrCancel")
    }

    override fun onSingleTap(event: MyGestureDetector.MyMotionEvent,
                             touchingObject: Any?,
                             touchingContext: Any?) {
        printLog(" 1⃣ onSingleTap")
    }

    override fun onDoubleTap(event: MyGestureDetector.MyMotionEvent,
                             touchingObject: Any?,
                             touchingContext: Any?): Boolean {
        printLog(" 2⃣ onDoubleTap  ")
        return false
    }

    override fun onLongTap(event: MyGestureDetector.MyMotionEvent,
                           touchingObject: Any?,
                           touchingContext: Any?): Boolean {
        printLog("\uD83D\uDD50 1⃣ onLongTap")
        return false
    }

    override fun onLongPress(event: MyGestureDetector.MyMotionEvent,
                             touchingObject: Any?,
                             touchingContext: Any?): Boolean {
        printLog("\uD83D\uDD50 onLongPress")
        return false
    }

    override fun onDragBegin(event: MyGestureDetector.MyMotionEvent,
                             touchingObject: Any?,
                             touchingContext: Any?,
                             xInCanvas: Float,
                             yInCanvas: Float): Boolean {
        // DO NOTHING.
        return false
    }

    override fun onDrag(event: MyGestureDetector.MyMotionEvent,
                        touchingObject: Any?,
                        touchingContext: Any?,
                        translationInCanvas: FloatArray?) {
        // DO NOTHING.
        printLog("⬌ onDrag")
    }

    override fun onDragEnd(event: MyGestureDetector.MyMotionEvent,
                           touchingObject: Any?,
                           touchingContext: Any?,
                           translationInCanvas: FloatArray?) {
        // DO NOTHING.
    }

    override fun onFling(event: MyGestureDetector.MyMotionEvent,
                         touchingObject: Any?,
                         touchContext: Any?,
                         startPointerInCanvas: FloatArray?,
                         stopPointerInCanvas: FloatArray?,
                         velocityX: Float,
                         velocityY: Float): Boolean {
        // DO NOTHING.
        return false
    }

    override fun onPinchBegin(event: MyGestureDetector.MyMotionEvent,
                              touchingObject: Any?,
                              touchContext: Any?,
                              pivotXInCanvas: Float,
                              pivotYInCanvas: Float): Boolean {
        // DO NOTHING.
        return false
    }

    override fun onPinch(event: MyGestureDetector.MyMotionEvent,
                         touchingObject: Any?,
                         touchContext: Any?,
                         startPointerOneInCanvas: FloatArray?,
                         startPointerTwoInCanvas: FloatArray?,
                         stopPointerOneInCanvas: FloatArray?,
                         stopPointerTwoInCanvas: FloatArray?) {
        // DO NOTHING.
    }

    override fun onPinchEnd(event: MyGestureDetector.MyMotionEvent,
                            touchingObject: Any?,
                            touchContext: Any?,
                            startPointerOneInCanvas: FloatArray?,
                            startPointerTwoInCanvas: FloatArray?,
                            stopPointerOneInCanvas: FloatArray?,
                            stopPointerTwoInCanvas: FloatArray?) {
        // DO NOTHING.
    }

    // MyGestureListener <- end -----------------------------------------------

    private fun printLog(msg: String) {
        mLog.add(msg)
        while (mLog.size > 16) {
            mLog.removeAt(0)
        }

        val builder = StringBuilder()
        mLog.forEach { line ->
            builder.append(line)
            builder.append(System.lineSeparator())
        }

        mTxtLog.text = builder.toString()
    }

    private fun clearLog() {
        mLog.clear()
        mTxtLog.text = getString(R.string.tap_anywhere_to_start)
    }
}
