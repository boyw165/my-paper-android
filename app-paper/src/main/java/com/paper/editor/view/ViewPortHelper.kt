//// Copyright Apr 2018-present boyw165@gmail.com
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
//package com.paper.editor.view
//
//import android.graphics.Matrix
//import android.graphics.PointF
//import android.graphics.RectF
//import android.util.Log
//import com.paper.AppConst
//import com.paper.shared.model.TransformModel
//import com.paper.util.TransformUtils
//import io.reactivex.Observable
//import io.reactivex.subjects.BehaviorSubject
//
//class ViewPortHelper {
//
//    private val mViewPortSignal = BehaviorSubject.create<RectF>()
//    private val mTransformSignal = BehaviorSubject.create<TransformModel>()
//
//    /**
//     * The view-port boundary in the model world.
//     */
//    private var mViewPortRatio = Float.NaN
//    /**
//     * The matrix for converting the View coordinate to View canvas coordinate.
//     */
//    private val mViewCanvasMatrix = Matrix()
//    /**
//     * The inverse of [mViewCanvasMatrix]
//     */
//    private val mViewCanvasMatrixInverse = Matrix()
//    private val mViewCanvasMatrixInverseStart = Matrix()
//    private var mCanvasMatrixDirty = false
//
//    fun init
//
//    fun startUpdateViewport() {
//        val viewPort = mViewPortSignal.value
//
//        Log.d(AppConst.TAG, "")
//        Log.d(AppConst.TAG, "---")
//        Log.d(AppConst.TAG, "start view port = x=%.3f, y=%.3f, w=%.3f, h=%.3f".format(
//            viewPort.left, viewPort.top, viewPort.width(), viewPort.height()))
//
//        // Hold necessary starting states.
//        mViewCanvasMatrixInverseStart.set(mViewCanvasMatrixInverse)
//    }
//
//    // TODO: Make the view-port code a component.
//    fun onUpdateViewport(startPointers: Array<PointF>,
//                                 stopPointers: Array<PointF>) {
//        // Compute new canvas matrix.
//        val transform = TransformUtils.getTransformFromPointers(
//            startPointers, stopPointers)
//        // TODO: Why do I need the mapper here?
//        //        val dx = mGestureEventMapper.invertScaleX(transform[TransformUtils.DELTA_X])
//        //        val dy = mGestureEventMapper.invertScaleY(transform[TransformUtils.DELTA_Y])
//        val dx = transform[TransformUtils.DELTA_X]
//        val dy = transform[TransformUtils.DELTA_Y]
//        val dScale = transform[TransformUtils.DELTA_SCALE_X]
//        val px = transform[TransformUtils.PIVOT_X]
//        val py = transform[TransformUtils.PIVOT_Y]
//        mTmpMatrixInverse.set(mViewCanvasMatrixInverseStart)
//        // TODO: Fix scale!
//        //        mTmpMatrixInverse.postScale(dScale, dScale, px, py)
//        mTmpMatrixInverse.postTranslate(dx, dy)
//        mTmpMatrixInverse.invert(mTmpMatrix)
//
//        mTransformHelper.getValues(mTmpMatrix)
//
//        val modelCanvasWidth = mModelCanvasSizeSignal.value.width
//        val modelCanvasHeight = mModelCanvasSizeSignal.value.height
//        val scaleFromModel2View = mScaleFromModel2ViewSignal.value
//        val scaleFromView2Model = 1f / scaleFromModel2View
//        val scaleFromModelCanvas2ViewCanvas = mTransformHelper.scaleX * scaleFromModel2View
//
//        // Compute new view port bound in the view world:
//        // .-------------------------.
//        // |          .---.          |
//        // | View     | V |          |
//        // | Canvas   | P |          |
//        // |          '---'          |
//        // |                         |
//        // '-------------------------'
//        mTmpBound.set(0f, 0f, width.toFloat(), height.toFloat())
//        mTmpMatrix.mapRect(mTmpBound)
//        val vx = mTmpBound.left / scaleFromModelCanvas2ViewCanvas
//        val vy = mTmpBound.top / scaleFromModelCanvas2ViewCanvas
//        // TODO: Why divide by canvas scale again?
//        val vw = mTmpBound.width() / scaleFromModelCanvas2ViewCanvas / mTransformHelper.scaleX
//        val vh = mTmpBound.height() / scaleFromModelCanvas2ViewCanvas / mTransformHelper.scaleX
//        mViewPort.set(vx, vy, vx + vw, vy + vh)
//
//        // Constraint view port
//        val minWidth = scaleFromView2Model * width / 3
//        val minHeight = minWidth / mViewPortRatio
//        val maxWidth = scaleFromView2Model * width
//        val maxHeight = maxWidth / mViewPortRatio
//        constraintViewPort(mViewPort,
//                           left = 0f,
//                           top = 0f,
//                           right = modelCanvasWidth,
//                           bottom = modelCanvasHeight,
//                           minWidth = minWidth,
//                           minHeight = minHeight,
//                           maxWidth = maxWidth,
//                           maxHeight = maxHeight)
//
//        markCanvasMatrixDirty()
//        delayedInvalidate()
//    }
//
//    fun stopUpdateViewport() {
//        // TODO: Upsample?
//    }
//
//    // Output /////////////////////////////////////////////////////////////////
//
//    fun onUpdateCanvasMatrix(): Observable<TransformModel> {
//        return mTransformSignal
//    }
//
//    fun onUpdateViewPort(): Observable<RectF> {
//        return mViewPortSignal
//    }
//}
