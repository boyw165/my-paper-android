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
import com.cardinalblue.gesture.MyMotionEvent
import com.paper.editor.view.ICanvasView
import com.paper.editor.view.IScrapLifecycleListener
import com.paper.editor.view.IScrapView
import com.paper.editor.view.SimpleGestureListener
import com.paper.shared.model.PaperModel
import com.paper.shared.model.ScrapModel
import com.paper.shared.model.TransformModel
import com.paper.shared.model.sketch.PathTuple
import com.paper.shared.model.sketch.Sketch
import com.paper.shared.model.sketch.SketchStroke
import com.paper.util.TransformUtils
import io.reactivex.Scheduler
import io.reactivex.disposables.CompositeDisposable
import java.util.*

class PaperController(private val mUiScheduler: Scheduler,
                      private val mWorkerScheduler: Scheduler)
    : SimpleGestureListener(),
      IScrapLifecycleListener {

    // View.
    private var mCanvasView: ICanvasView? = null

    // Model.
    private lateinit var mModel: PaperModel
    private var mTmpSketch: Sketch? = null

    // Sub controllers.
    private val mControllers: HashMap<UUID, IScrapController> = hashMapOf()

    // Gesture detector.
    private val mPointerMap: FloatArray = floatArrayOf(0f, 0f)
    private val mStartMatrixToParent: Matrix = Matrix()
    private val mStopMatrixToParent: Matrix = Matrix()
    private val mStopTransformToParent: TransformModel = TransformModel(0f, 0f, 1f, 1f, 0f)
    private val mTransformHelper: TransformUtils = TransformUtils()

    // Disposables
    private val mDisposablesOnCreate = CompositeDisposable()
    private val mDisposablesOnResume = CompositeDisposable()

    fun loadPaper(model: PaperModel) {
        mControllers.clear()

        mModel = model
        // Create the scrap controller.
        mModel.scraps.forEach { scrap ->
            val controller = ScrapController(mUiScheduler,
                                             mWorkerScheduler)

            // Pass model reference to the controller.
            controller.loadScrap(scrap)

            // Add controller to the lookup table.
            mControllers[scrap.id] = controller
        }
    }

    fun bindView(view: ICanvasView) {
        mCanvasView = view

        // TODO: Encapsulate the inflation with a custom Observable.
        mCanvasView?.setScrapLifecycleListener(this@PaperController)
        mCanvasView?.setGestureListener(this@PaperController)
        mCanvasView?.setCanvasWidthOverHeightRatio(mModel.widthOverHeight)
        mCanvasView?.let { canvasView ->
            // Inflate scraps.
            mModel.scraps.forEach { scrap ->
                canvasView.addViewBy(scrap)
            }
        }
    }

    fun unbindView() {
        mDisposablesOnCreate.clear()

        mCanvasView?.setScrapLifecycleListener(null)
        mCanvasView = null

        // Unbind views from sub-controllers
        mControllers.values.forEach { controller ->
            controller.unbindView()
        }
        mControllers.clear()
    }

    ///////////////////////////////////////////////////////////////////////////
    // Scrap lifecycle ////////////////////////////////////////////////////////

    override fun onAttachToCanvas(view: IScrapView) {
        val controller = mControllers[view.getScrapId()]

        controller!!.bindView(view)
    }

    override fun onDetachFromCanvas(view: IScrapView) {
        val controller = mControllers[view.getScrapId()]

        controller!!.unbindView()
    }

    ///////////////////////////////////////////////////////////////////////////
    // Gesture handling ///////////////////////////////////////////////////////

    override fun onDragBegin(event: MyMotionEvent,
                             target: Any?,
                             context: Any?) {
        val x = event.downFocusX
        val y = event.downFocusY
        val np = mCanvasView!!.normalizePointer(PointF(x, y))

        mTmpSketch = Sketch()
        mTmpSketch!!.addStroke(SketchStroke())
        mTmpSketch!!.lastStroke.setWidth(0.2f)
        mTmpSketch!!.lastStroke.add(PathTuple(np.x, np.y))

        // Notify view.
        mCanvasView!!.startDrawSketch(x, y)
    }

    override fun onDrag(event: MyMotionEvent,
                        target: Any?,
                        context: Any?,
                        startPointer: PointF,
                        stopPointer: PointF) {
        val x = event.downFocusX
        val y = event.downFocusY
        val np = mCanvasView!!.normalizePointer(PointF(x, y))

        mTmpSketch!!.lastStroke.add(PathTuple(np.x, np.y))

        // Notify view.
        mCanvasView!!.onDrawSketch(x, y)
    }

    override fun onDragEnd(event: MyMotionEvent,
                           target: Any?,
                           context: Any?,
                           startPointer: PointF,
                           stopPointer: PointF) {
        val x = event.downFocusX
        val y = event.downFocusY
        val np = mCanvasView!!.normalizePointer(PointF(x, y))

        mTmpSketch!!.lastStroke.add(PathTuple(np.x, np.y))

        // Notify view.
        mCanvasView!!.stopDrawSketch()

        // TODO: Normalize the strokes.
        // Commit the temporary scrap.
        val scrap = ScrapModel()
        scrap.sketch = mTmpSketch
        mModel.scraps.add(scrap)
        mTmpSketch = null

        // Create controller.
        val controller = ScrapController(mUiScheduler, mWorkerScheduler)
        controller.loadScrap(scrap)
        mControllers[scrap.id] = controller

        // Inflate scraps.
        mCanvasView?.addViewBy(scrap)
    }

    ///////////////////////////////////////////////////////////////////////////

    ///////////////////////////////////////////////////////////////////////////
    // Protected / Private Methods ////////////////////////////////////////////

//    private fun holdStartTransform() {
//        mStartMatrixToParent.reset()
//        mStartMatrixToParent.set(mCanvasView!!.getTransformMatrix())
//        mStopMatrixToParent.reset()
//        mStopMatrixToParent.set(mStartMatrixToParent)
//    }

//    private fun convertPointToParentWorld(point: PointF): PointF {
//        mPointerMap[0] = point.x
//        mPointerMap[1] = point.y
//        mCanvasView!!.convertPointToParentWorld(mPointerMap)
//
//        return PointF(mPointerMap[0], mPointerMap[1])
//    }
}
