// Copyright Jul 2018-present Paper
//
// Author: boyw165@gmail.com
//
// Permission is hereby granted, free of charge, to any person obtaining
// a copy of this software and associated documentation files (the "Software"),
// to deal in the Software without restriction, including without limitation
// the rights to use, copy, modify, merge, publish, distribute, sublicense,
// and/or sell copies of the Software, and to permit persons to whom the
// Software is furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included
// in all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
// OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
// THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
// FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
// DEALINGS IN THE SOFTWARE.

package com.paper.unity

import android.content.Context
import android.os.Looper
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.widget.FrameLayout
import com.cardinalblue.gesture.GestureDetector
import com.cardinalblue.gesture.rx.DragBeginEvent
import com.cardinalblue.gesture.rx.DragEndEvent
import com.cardinalblue.gesture.rx.GestureEventObservable
import com.cardinalblue.gesture.rx.OnDragEvent
import com.unity3d.player.UnityPlayer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable

class PaperCanvasUnityView : FrameLayout {

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int)
        : super(context, attrs, defStyleAttr) {
        // DO NOTHING
    }

    private var mUnityPlayer: UnityPlayer? = null

    private var mTouchSlop: Float = 0f
    private var mTapSlop: Float = 0f
    private var mMinFlingVec: Float = 0f
    private var mMaxFlingVec: Float = 0f
    private val mGestureDetector by lazy {
        GestureDetector(
            uiLooper = Looper.getMainLooper(),
            viewConfig = ViewConfiguration.get(context),
            tapSlop = mTapSlop,
            touchSlop = mTouchSlop,
            minFlingVec = mMinFlingVec,
            maxFlingVec = mMaxFlingVec)
    }

    fun inject(player: UnityPlayer,
               touchSlop: Float,
               tapSlop: Float,
               minFlingVec: Float,
               maxFlingVec: Float) {
        removeAllViews()

        mUnityPlayer = player
        mUnityPlayer?.let { addView(it) }

        mTouchSlop = touchSlop
        mTapSlop = tapSlop
        mMinFlingVec = minFlingVec
        mMaxFlingVec = maxFlingVec
    }

    private val mDisposables = CompositeDisposable()

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        mDisposables.add(
            GestureEventObservable(mGestureDetector)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { event ->
                    when (event) {
                        is DragBeginEvent -> {
                            UnityPlayer.UnitySendMessage("CanvasController", "BeginMoveCameraXY", "")
                        }
                        is OnDragEvent -> {
                            val dx = event.stopPointer.x - event.startPointer.x
                            val dy = event.stopPointer.y - event.startPointer.y
                            val normDx = dx / width
                            // The Unity Y is opposite to Android coordinate system
                            val normDy = -dy / height

                            UnityPlayer.UnitySendMessage("CanvasController", "MoveCameraXY", "$normDx,$normDy")
                        }
                        is DragEndEvent -> {
                            UnityPlayer.UnitySendMessage("CanvasController", "StopMoveCameraXY", "")
                        }
                    }
                })
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()

        mDisposables.dispose()
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        return true
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return mGestureDetector.onTouchEvent(event, null, null) ||
               super.onTouchEvent(event)
    }
}
