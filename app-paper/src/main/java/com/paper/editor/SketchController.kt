// Copyright Feb 2018-present boyw165@gmail.com
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

package com.paper.editor

import android.graphics.Matrix
import android.graphics.PointF
import com.paper.editor.view.IScrapView
import com.paper.shared.model.ScrapModel
import com.paper.shared.model.TransformModel
import com.paper.util.TransformUtils
import io.reactivex.Scheduler
import io.reactivex.disposables.CompositeDisposable

class SketchController(private val mUiScheduler: Scheduler,
                       private val mWorkerScheduler: Scheduler)
    : IScrapController {

    // View.
    private var mView: IScrapView? = null

    // Gesture detector.
    private val mPointerMap: FloatArray = floatArrayOf(0f, 0f)
    private val mStartMatrixToParent: Matrix = Matrix()
    private val mStopMatrixToParent: Matrix = Matrix()
    private val mStopTransformToParent: TransformModel = TransformModel(0f, 0f, 1f, 1f, 0f)
    private val mTransformHelper: TransformUtils = TransformUtils()

    // Disposables
    private val mDisposablesOnCreate = CompositeDisposable()
    private val mDisposablesOnResume = CompositeDisposable()

    override fun loadModel(model: ScrapModel) {
        TODO("not implemented")
    }

    override fun bindView(view: IScrapView) {
        println("bindView")
    }

    override fun unbindView() {
        println("unbindView")
    }

    ///////////////////////////////////////////////////////////////////////////
    // Gesture handling ///////////////////////////////////////////////////////

    ///////////////////////////////////////////////////////////////////////////

    ///////////////////////////////////////////////////////////////////////////
    // Protected / Private Methods ////////////////////////////////////////////

    private fun holdStartTransform() {
        mStartMatrixToParent.reset()
        mStartMatrixToParent.set(mView!!.getTransformMatrix())
        mStopMatrixToParent.reset()
        mStopMatrixToParent.set(mStartMatrixToParent)
    }

    private fun convertPointToParentWorld(point: PointF): PointF {
        mPointerMap[0] = point.x
        mPointerMap[1] = point.y
        mView!!.convertPointToParentWorld(mPointerMap)

        return PointF(mPointerMap[0], mPointerMap[1])
    }
}
