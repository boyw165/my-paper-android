//// Copyright Feb 2018-present boyw165@gmail.com
////
//// Permission is hereby granted, free of charge, to any person obtaining
//// a copy of this software and associated documentation files (the "Software"),
//// to deal in the Software without restriction, including without limitation
//// the rights to use, copy, modify, merge, publish, distribute, sublicense,
//// and/or sell copies of the Software, and to permit persons to whom the
//// Software is furnished to do so, subject to the following conditions:
////
//// The above copyright notice and this permission notice shall be included
//// in all copies or substantial portions of the Software.
////
//// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
//// OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
//// THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
//// FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
//// DEALINGS IN THE SOFTWARE.
//
//package com.paper.editor
//
//import android.os.Looper
//import com.cardinalblue.gesture.MyMotionEvent
//import com.paper.editor.controller.PaperWidget
//import com.paper.editor.controller.IScrapWidget
//import com.paper.editor.data.GestureRecord
//import com.paper.editor.view.IPaperWidgetView
//import com.paper.editor.view.SimpleGestureListener
//import com.paper.shared.model.PaperModel
//import io.reactivex.Scheduler
//import java.util.*
//
//class CanvasController(private val mUiScheduler: Scheduler,
//                       private val mWorkerScheduler: Scheduler)
//    : SimpleGestureListener() {
//
//    // Model, view, and view-model.
//    private lateinit var mModel: PaperModel
//    private var mView: IPaperWidgetView? = null
//    private lateinit var mViewModel: PaperWidget
//
//    // Sub controllers.
//    private val mControllers: HashMap<UUID, IScrapWidget> = hashMapOf()
//
//    // Gesture detector.
//    private val mGestureHistory = mutableListOf<GestureRecord>()
//
//    fun bindView(view: IPaperWidgetView) {
//        ensureMainThread()
//
//        if (mView != null) throw IllegalStateException(
//            "Already bind to a view")
//
//        // Inti view-model and set to view.
//        mViewModel = PaperWidget(mModel.width, mModel.height)
//        view.bindWidget(mViewModel)
//        view.setGestureListener(this@CanvasController)
//
//        // Init scrap views.
//        mModel.scraps.forEach { scrap ->
//            val controller = mControllers[scrap.uuid]
//            val scrapView = view.addScrap(scrap.uuid)
//
//            controller!!.bindView(scrapView)
//        }
//
//        // Hold reference.
//        mView = view
//    }
//
//    fun unbindView() {
//        ensureMainThread()
//
//        mView?.unbindWidget()
//        mView?.setGestureListener(null)
//        mView = null
//
//        // Unbind views from sub-controllers
//        unbindViewsFromControllers()
//    }
//
//    ///////////////////////////////////////////////////////////////////////////
//    // Paper things ///////////////////////////////////////////////////////////
//
//    fun setPaper(model: PaperModel) {
//        ensureMainThread()
//
//        unbindViewsFromControllers()
//
//        mModel = model
//        // Create the scrap controller.
//        mModel.scraps.forEach { scrap ->
//            val controller = ScrapController(mUiScheduler,
//                                                              mWorkerScheduler)
//
//            // Pass model reference to the controller.
//            controller.subscribeModel(scrap)
//
//            // Add controller to the lookup table.
//            mControllers[scrap.uuid] = controller
//        }
//    }
//
//    fun getPaper(): PaperModel {
//        return mModel
//    }
//
////    ///////////////////////////////////////////////////////////////////////////
////    // Scrap lifecycle ////////////////////////////////////////////////////////
////
////    override fun onAttachToCanvas(view: IScrapWidgetView) {
////        val controller = mControllers[view.getScrapId()]
////
////        controller!!.bindView(view)
////    }
////
////    override fun onDetachFromCanvas(view: IScrapWidgetView) {
////        val controller = mControllers[view.getScrapId()]
////
////        controller!!.unbindView()
////    }
//
//    ///////////////////////////////////////////////////////////////////////////
//    // Gesture handling ///////////////////////////////////////////////////////
//
//    override fun onActionBegin(event: MyMotionEvent,
//                               target: Any?,
//                               context: Any?) {
//        // Clear the gesture history.
//        mGestureHistory.clear()
//    }
//
//    override fun onActionEnd(event: MyMotionEvent,
//                             target: Any?,
//                             context: Any?) {
//        // DO NOTHING.
//    }
//
//    override fun onSingleTap(event: MyMotionEvent,
//                             target: Any?,
//                             context: Any?) {
//
//    }
//
//    //    override fun onDragBegin(event: MyMotionEvent,
////                             target: Any?,
////                             context: Any?) {
////        // Document gesture.
////        mGestureHistory.add(GestureRecord.DRAG)
////
////        // If PINCH is ever present, skip drawing sketch.
////        if (mGestureHistory.contains(GestureRecord.PINCH)) return
////
////        val x = event.downFocusX
////        val y = event.downFocusY
////
////        // Notify view.
////        mView!!.startDrawStroke(x, y)
////    }
////
////    override fun onDrag(event: MyMotionEvent,
////                        target: Any?,
////                        context: Any?,
////                        startPointer: PointF,
////                        stopPointer: PointF) {
////        // If PINCH is ever present, skip drawing sketch.
////        if (mGestureHistory.contains(GestureRecord.PINCH)) return
////
////        val x = event.downFocusX
////        val y = event.downFocusY
////
////        // Notify view.
////        mView!!.onDrawStroke(x, y)
////    }
////
////    override fun onDragEnd(event: MyMotionEvent,
////                           target: Any?,
////                           context: Any?,
////                           startPointer: PointF,
////                           stopPointer: PointF) {
////        // If PINCH is ever present, skip drawing sketch.
////        if (mGestureHistory.contains(GestureRecord.PINCH)) return
////
////        // Notify view.
////        val stroke = mView!!.stopDrawStroke()
////
////        // Commit the temporary scrap.
////        val strokeBoundLeft = stroke.bound.left
////        val strokeBoundTop = stroke.bound.top
////        stroke.offset(-strokeBoundLeft, -strokeBoundTop)
////
////        val sketch = SketchModel()
////        sketch.addStroke(stroke)
////        val scrap = ScrapModel()
////        scrap.x = strokeBoundLeft
////        scrap.y = strokeBoundTop
////        scrap.sketch = sketch
////
////        mModel.scraps.add(scrap)
////
////        // Create controller.
////        val controller = ScrapController(mUiScheduler, mWorkerScheduler)
////        controller.subscribeModel(scrap)
////        mControllers[scrap.uuid] = controller
////
//////        // Inflate scraps.
//////        mView?.addScrapView(scrap)
////    }
////
////    override fun onPinchBegin(event: MyMotionEvent,
////                              target: Any?,
////                              context: Any?,
////                              startPointers: Array<PointF>) {
////        // Document gesture.
////        mGestureHistory.add(GestureRecord.PINCH)
////
////        mView?.startUpdateViewport()
////    }
////
////    override fun onPinch(event: MyMotionEvent,
////                         target: Any?,
////                         context: Any?,
////                         startPointers: Array<PointF>,
////                         stopPointers: Array<PointF>) {
////        mView?.onUpdateViewport(startPointers, stopPointers)
////    }
////
////    override fun onPinchEnd(event: MyMotionEvent,
////                            target: Any?,
////                            context: Any?,
////                            startPointers: Array<PointF>,
////                            stopPointers: Array<PointF>) {
////        mView?.stopUpdateViewport()
////    }
//
//    ///////////////////////////////////////////////////////////////////////////
//    // Protected / Private Methods ////////////////////////////////////////////
//
//    private fun ensureMainThread() {
//        if (Looper.myLooper() != Looper.getMainLooper()) {
//            throw IllegalThreadStateException("Not in MAIN thread")
//        }
//    }
//
//    private fun unbindViewsFromControllers() {
//        mControllers.values.forEach { controller ->
//            controller.unbindView()
//        }
//    }
//}
