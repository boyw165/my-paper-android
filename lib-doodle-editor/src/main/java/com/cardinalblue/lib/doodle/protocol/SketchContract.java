// Copyright (c) 2017-present Cardinalblue
//
// Author: boy@cardinalblue.com
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
//    The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
//    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.

package com.cardinalblue.lib.doodle.protocol;

import com.cardinalblue.lib.doodle.event.DragEvent;
import com.cardinalblue.lib.doodle.event.DrawStrokeEvent;
import com.cardinalblue.lib.doodle.event.GestureEvent;
import com.cardinalblue.lib.doodle.event.PinchEvent;
import com.cardinalblue.lib.doodle.event.SingleTapEvent;
import com.my.reactive.uiEvent.UiEvent;
import com.paper.shared.model.sketch.Sketch;
import com.paper.shared.model.sketch.SketchStroke;

import java.io.InputStream;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableTransformer;

/**
 * The contract between components in the sketch editor.
 */
public abstract class SketchContract {

    /**
     * The editor view contains the brush width bar and color palette.
     */
    public interface IEditorView {

        void showProgress(String message);

        void hideProgress();

        void close();

        void closeWithUpdate(Sketch model, int brushColor, int strokeWidth);

        void showErrorAlertThenClose(Throwable error);

        void setBrushSize(int brushSize);

        void setBrushItems(List<ISketchBrush> brushes);

        void selectBrushAt(int defaultSelection);

        void showBrushColor(int color);

        void showStrokeColorAndWidthPreview(int color);

        void hideStrokeColorAndWidthPreview();

        void showOrHideDoneButton(boolean showed);

        void showOrHideUndoButton(boolean showed, boolean enabled);

        void showOrHideRedoButton(boolean showed, boolean enabled);

        void showOrHideClearButton(boolean showed);

        void enableFullscreenMode(boolean enabled);
    }

    /**
     * The canvas view focusing on drawing.
     */
    public interface ISketchView extends IMatrixProvider {

        // Init methods...

        /**
         * Create canvas. This is NECESSARY for using the editor.
         *
         * @param width  Desired width.
         * @param height Desired height.
         * @param color  Default background color.
         */
        Observable<Object> createCanvasSource(int width,
                                              int height,
                                              int color);

        int getCanvasWidth();

        int getCanvasHeight();

        /**
         * Assign the sketch view a background.
         *
         * @param background The bitmap input stream.
         */
        void setBackground(InputStream background);

        void setCanvasConstraintPadding(int left, int top, int right, int bottom);

        // Draw methods...

        void eraseCanvas();

        void drawStrokeFrom(SketchStroke stroke, int from);

        void drawStrokes(List<SketchStroke> strokes);

        void drawAndSharpenStrokes(List<SketchStroke> strokes);

        // Pinch methods...

        void updateCanvasMatrix(IMatrix matrix);

        void stopUpdatingCanvasMatrix(float px, float py);

        // Animation methods...

        /**
         * Any kind of running animation.
         */
        boolean isAnimating();

        /**
         * Run an animation that reset the view to the transform at first
         * onCanvasSourceReady call.
         */
        Observable<?> resetAnimation();

        // DEBUG methods...

        boolean isDebug();

        void setDebug(boolean isDebug);

        void debugStrokes(List<SketchStroke> strokes);
    }

    /**
     * For the manipulators to get model.
     */
    public interface IModelProvider {

        Sketch getSketchModel();
    }

    /**
     * Presenter in charge of the sketch editor.
     */
    public interface ISketchEditorPresenter {

        Observable<?> initEditorAndLoadSketch(int sketchWidth,
                                              int sketchHeight,
                                              int brushColor,
                                              int brushSize);

        ObservableTransformer<InputStream, ?> setBackground();

        ObservableTransformer<GestureEvent, ?> touchCanvas();

        ObservableTransformer<Integer, ?> setBrush();

        ObservableTransformer<UiEvent<Integer>, ?> setStrokeWidth();

        ObservableTransformer<Object, ?> undo();

        ObservableTransformer<Object, ?> redo();

        ObservableTransformer<Object, ?> clearStrokes();

        ObservableTransformer<Object, ?> done();

        ObservableTransformer<Object, ?> close();
    }

    /**
     * Sub-component used by the presenter to draw stroke on canvas.
     */
    public interface IDrawStrokeManipulator {

        void setBrush(ISketchBrush brush);

        ISketchBrush getBrush();

        void setBrushSize(float baseWidth, int value);

        ObservableTransformer<DragEvent, DrawStrokeEvent> drawStroke(IModelProvider modelProvider);

        ObservableTransformer<SingleTapEvent, DrawStrokeEvent> drawDot(IModelProvider modelProvider);
    }

    // TODO: Make it public rather than just made for this contract.

    /**
     * Sub-component used by the presenter to pinch in/out canvas.
     */
    public interface IPinchCanvasManipulator {

        ObservableTransformer<PinchEvent, PinchEvent> pinchCanvas();
    }

    /**
     * Sub-component used by the presenter to handle undo/redo to the model.
     */
    public interface ISketchUndoRedoManipulator {

        int sizeOfUndo();

        int sizeOfRedo();

        ObservableTransformer<Object, List<SketchStroke>> undo(IModelProvider modelProvider);

        ObservableTransformer<Object, List<SketchStroke>> undoAll(IModelProvider modelProvider);

        ObservableTransformer<Object, List<SketchStroke>> redo(IModelProvider modelProvider);

        ObservableTransformer<Object, List<SketchStroke>> clearAll(IModelProvider modelProvider);

        ObservableTransformer<Object, ?> onSpyingStrokesUpdate(IModelProvider modelProvider);
    }
}
