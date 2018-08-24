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

package com.paper.domain.ui

import com.paper.domain.data.DrawingMode
import com.paper.model.IPaper
import com.paper.domain.event.UpdateScrapWidgetEvent
import io.reactivex.Observable

/**
 * The canvas widget (serving as the ViewModel to View).
 */
interface ICanvasWidget : IWidget {

    fun setModel(paper: IPaper)

    fun toPaper(): IPaper

    // Add & Remove Scrap /////////////////////////////////////////////////////

    fun addScrapWidget(scrapWidget: IBaseScrapWidget)

    fun removeScrapWidget(scrapWidget: IBaseScrapWidget)

    fun getFocusScrap(): IBaseScrapWidget?

    fun addScrapWidgetAndSetFocus(scrapWidget: IBaseScrapWidget)

    fun onUpdateScrap(): Observable<UpdateScrapWidgetEvent>

    fun eraseCanvas()

    // Drawing ////////////////////////////////////////////////////////////////

    fun startSketch(x: Float,
                    y: Float)

    fun sketchTo(x: Float,
                 y: Float)

    fun closeSketch()

    fun setDrawingMode(mode: DrawingMode)

    fun setChosenPenColor(color: Int)

    fun setViewPortScale(scale: Float)

    fun setPenSize(size: Float)

    fun onUpdateCanvasSize(): Observable<Pair<Float, Float>>

    // Operation //////////////////////////////////////////////////////////////

    fun onUpdateCanvasOperation(): Observable<ICanvasOperation>

    // Debug //////////////////////////////////////////////////////////////////

    fun onPrintDebugMessage(): Observable<String>
}
