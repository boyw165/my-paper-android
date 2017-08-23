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

package com.cardinalblue.lib.doodle.controller;

import android.util.Log;

import com.cardinalblue.lib.doodle.data.SketchBrushFactory;
import com.cardinalblue.lib.doodle.event.DragEvent;
import com.cardinalblue.lib.doodle.event.DrawStrokeEvent;
import com.cardinalblue.lib.doodle.event.GestureEvent;
import com.cardinalblue.lib.doodle.event.PinchEvent;
import com.cardinalblue.lib.doodle.event.SingleTapEvent;
import com.cardinalblue.lib.doodle.event.UndoRedoEvent;
import com.cardinalblue.lib.doodle.protocol.ILogger;
import com.cardinalblue.lib.doodle.protocol.ISketchBrush;
import com.cardinalblue.lib.doodle.protocol.SketchContract;
import com.my.reactive.uiEvent.UiEvent;
import com.my.reactive.uiModel.UiModel;
import com.my.reactive.util.ObservableConst;
import com.paper.shared.model.repository.protocol.ISketchModelRepo;
import com.paper.shared.model.sketch.SketchModel;
import com.paper.shared.model.sketch.SketchStrokeModel;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.ObservableTransformer;
import io.reactivex.Scheduler;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;

public class SketchEditorPresenter implements SketchContract.ISketchEditorPresenter,
                                              SketchContract.IModelProvider {

    private static final String TAG = "sketch";
    private static final int DEFAULT_BG_COLOR = 0xFFFFFFFF;

    // Given
    private final ISketchModelRepo mSketchModelRepository;
    private final SketchContract.IEditorView mEditorView;
    private final SketchContract.ISketchView mSketchView;
    private final SketchContract.IDrawStrokeManipulator mDrawStrokeManipulator;
    private final SketchContract.IPinchCanvasManipulator mPinchCanvasController;
    private final SketchContract.ISketchUndoRedoManipulator mUndoRedoManipulator;
    private final Scheduler mWorkerScheduler;
    private final Scheduler mUiScheduler;
    private final ILogger mLogger;

    // Model
    private SketchModel mSketchModel;

    // Brush.
    private List<ISketchBrush> mBrushes = new ArrayList<>();
    // Stroke.
    private int mStrokeWidthProgress;

    // Life state.
    private AtomicBoolean mIsAlive = new AtomicBoolean(true);
    // Save condition state.
    private AtomicBoolean mIfApplyChangeForClose = new AtomicBoolean(false);
    // View state.
    private AtomicInteger mCountOfDoing = new AtomicInteger(0);

    // TODO: Inject it.
    public SketchEditorPresenter(ISketchModelRepo sketchModelRepo,
                                 SketchContract.IEditorView editorView,
                                 SketchContract.ISketchView sketchView,
                                 SketchContract.IDrawStrokeManipulator drawStrokeManipulator,
                                 SketchContract.IPinchCanvasManipulator pinchCanvasManipulator,
                                 SketchContract.ISketchUndoRedoManipulator undoRedoController,
                                 Scheduler workerScheduler,
                                 Scheduler uiScheduler,
                                 ILogger logger) {
        mSketchModelRepository = sketchModelRepo;
        // Create a dummy model and the presenter will inflate the real one with
        // the help of repository.
        mSketchModel = new SketchModel(1, 1);

        mEditorView = editorView;
        mSketchView = sketchView;
        mSketchView.createCanvasSource(mSketchModel.getWidth(),
                                       mSketchModel.getHeight(),
                                       DEFAULT_BG_COLOR);

        mDrawStrokeManipulator = drawStrokeManipulator;
        mPinchCanvasController = pinchCanvasManipulator;
        mUndoRedoManipulator = undoRedoController;

        mWorkerScheduler = workerScheduler;
        mUiScheduler = uiScheduler;

        mLogger = logger;
    }

    // TODO: Merge loading background.
    @Override
    public Observable<?> initEditorAndLoadSketch(final int brushColor,
                                                 final int brushSize) {
        return Observable
            .mergeArray(
            Observable.fromCallable(new Callable<Object>() {
                @Override
                public Object call() throws Exception {
                    // Init the brush color and size.
                    prepareBrushes(brushColor, brushSize);

                    return ObservableConst.IGNORED;
                }
            }),
            mSketchModelRepository
                .getTempSketch()
                .publish(new Function<Observable<SketchModel>, ObservableSource<Object>>() {
                    @Override
                    public ObservableSource<Object> apply(Observable<SketchModel> shared)
                        throws Exception {
                        return Observable.mergeArray(
                            // For triggering the start UI state.
                            shared.compose(mToUiModelTransformer),
                            // Init the model.
                            shared.observeOn(mUiScheduler)
                                  .map(new Function<SketchModel, List<SketchStrokeModel>>() {
                                      @Override
                                      public List<SketchStrokeModel> apply(SketchModel sketchModel)
                                          throws Exception {
                                          Log.d(TAG, String.format(Locale.ENGLISH,
                                                                   "Load sketch(w=%d, h=%d)",
                                                                   sketchModel.getWidth(),
                                                                   sketchModel.getHeight()));
                                          // Update model.
                                          mSketchModel = sketchModel;

                                          // TODO: A improvement that progressively have view
                                          // TODO: draw strokes without freezing UI.
                                          // Init strokes preview.
                                          final List<SketchStrokeModel> strokes = mSketchModel.getAllStrokes();
                                          mSketchView.eraseCanvas();
                                          mSketchView.drawStrokes(strokes);



                                          return strokes;
                                      }
                                  })
                                  // React to views.
                                  .publish(new Function<Observable<List<SketchStrokeModel>>, ObservableSource<Object>>() {
                                      @Override
                                      public ObservableSource<Object> apply(Observable<List<SketchStrokeModel>> shared)
                                          throws Exception {
                                          return Observable.mergeArray(
                                              shared.compose(mToUiModelTransformer),
                                              shared.compose(mUndoRedoManipulator.onSpyingStrokesUpdate(SketchEditorPresenter.this))
                                                    .compose(mPostTouchCanvas));
                                      }
                                  }));
                    }
                }))
            .compose(mUiModelObserver);
    }

    // FIXME: Race condition (single source of truth) in mUiModelObserver.
    @Override
    public ObservableTransformer<InputStream, ?> setBackground() {
        return new ObservableTransformer<InputStream, Object>() {
            @Override
            public ObservableSource<Object> apply(Observable<InputStream> upstream) {
                return upstream
                    .observeOn(mUiScheduler)
                    .map(new Function<InputStream, Object>() {
                        @Override
                        public Object apply(InputStream inputStream)
                            throws Exception {
                            // Init background preview.
                            mSketchView.setBackground(inputStream);
                            // Close the given input stream.
                            inputStream.close();

                            return ObservableConst.IGNORED;
                        }
                    })
                    .compose(mToUiModelTransformer)
                    .compose(mUiModelObserver);
            }
        };
    }

    @Override
    public ObservableTransformer<GestureEvent, ?> touchCanvas() {
        return new ObservableTransformer<GestureEvent, Object>() {
            @Override
            public ObservableSource<Object> apply(Observable<GestureEvent> upstream) {
                // Have manipulator to convert gesture event to model level event.
                return upstream
                    // Skip when the view is animating or the presenter is
                    // flagged DEAD.
                    .filter(new Predicate<GestureEvent>() {
                        @Override
                        public boolean test(GestureEvent ignored)
                            throws Exception {
                            return mIsAlive.get() && !mSketchView.isAnimating();
                        }
                    })
                    .publish(new Function<Observable<GestureEvent>, ObservableSource<Object>>() {
                        @Override
                        public ObservableSource<Object> apply(Observable<GestureEvent> shared) throws Exception {
                            return Observable
                                .mergeArray(
                                    // Draw strokes.
                                    shared.ofType(DragEvent.class)
                                          // Update the canvas model.
                                          .compose(mDrawStrokeManipulator.drawStroke(mSketchModel))
                                          .compose(mDrawStrokeOnCanvas),
                                    // Draw dots.
                                    shared.ofType(SingleTapEvent.class)
                                          // Update the canvas model.
                                          .compose(mDrawStrokeManipulator.drawDot(mSketchModel))
                                          .compose(mDrawStrokeOnCanvas),
                                    // Pinch in or out canvas.
                                    shared.ofType(PinchEvent.class)
                                          .compose(mPinchCanvasController.pinchCanvas())
                                          .compose(mUpdateCanvasMatrix)
                                )
                                // Handle undo/redo (optional).
                                .compose(mUndoRedoManipulator.onSpyingStrokesUpdate(SketchEditorPresenter.this))
                                // Post process afterwards.
                                .compose(mPostTouchCanvas)
                                .compose(ObservableConst.FILTER_IGNORED);
                        }
                    });
            }
        };
    }

    @Override
    public ObservableTransformer<Integer, ?> setBrush() {
        return new ObservableTransformer<Integer, Object>() {
            @Override
            public ObservableSource<Object> apply(Observable<Integer> upstream) {
                return upstream.observeOn(mUiScheduler).map(new Function<Integer, Object>() {
                    @Override
                    public Object apply(Integer position)
                        throws Exception {
                        // Update brush.
                        final ISketchBrush brush = mBrushes.get(position);
                        mDrawStrokeManipulator.setBrush(brush);

                        // Update seek-bar.
                        mEditorView.showBrushColor(brush.getBrushColor());

                        return ObservableConst.IGNORED;
                    }
                });
            }
        };
    }

    @Override
    public ObservableTransformer<UiEvent<Integer>, ?> setStrokeWidth() {
        return new ObservableTransformer<UiEvent<Integer>, Object>() {
            @Override
            public ObservableSource<Object> apply(Observable<UiEvent<Integer>> upstream) {
                return upstream.map(new Function<UiEvent<Integer>, Object>() {
                    @Override
                    public Object apply(UiEvent<Integer> event)
                        throws Exception {
                        // The value is from 0 to 100.
                        final int value = event.bundle;

                        if (event.justStart) {
                            // Just start...

                        } else if (event.doing) {
                            // Doing...

                            // Cache the stroke progress value for the calculation
                            // when the pinch canvas happens.
                            mStrokeWidthProgress = value;

                            // Update stroke width.
                            mDrawStrokeManipulator.setBrushSize(getStrokeBaseWidth(), value);

                            // Show stroke color and width preview.
                            if (event.isTriggeredByUser) {
                                if (mDrawStrokeManipulator.getBrush() != null) {
                                    mEditorView.showStrokeColorAndWidthPreview(
                                        mDrawStrokeManipulator.getBrush()
                                                              .getBrushColor());
                                } else {
                                    mEditorView.showStrokeColorAndWidthPreview(0x00FFFFFF);
                                }
                            }
                        } else {
                            // Stop...

                            // Send analytics event.
                            if (event.isTriggeredByUser) {
                                mLogger.sendEvent("Doodle editor - change stroke size",
                                                  "size", String.valueOf(value));
                            }

                            // Hide stroke color and width preview.
                            mEditorView.hideStrokeColorAndWidthPreview();
                        }

                        return ObservableConst.IGNORED;
                    }
                });
            }
        };
    }

    @Override
    public ObservableTransformer<Object, ?> undo() {
        return new ObservableTransformer<Object, Object>() {
            @Override
            public ObservableSource<Object> apply(Observable<Object> upstream) {
                if (mUndoRedoManipulator != null) {
                    return upstream.compose(mUndoRedoManipulator.undo(SketchEditorPresenter.this))
                                   .compose(mUpdateCanvasStrokes)
                                   .compose(mPostTouchCanvas)
                                   .compose(ObservableConst.FILTER_IGNORED);
                } else {
                    return upstream;
                }
            }
        };
    }

    @Override
    public ObservableTransformer<Object, ?> redo() {
        return new ObservableTransformer<Object, Object>() {
            @Override
            public ObservableSource<Object> apply(Observable<Object> upstream) {
                if (mUndoRedoManipulator != null) {
                    return upstream.compose(mUndoRedoManipulator.redo(SketchEditorPresenter.this))
                                   .compose(mUpdateCanvasStrokes)
                                   .compose(mPostTouchCanvas)
                                   .compose(ObservableConst.FILTER_IGNORED);
                } else {
                    return upstream;
                }
            }
        };
    }

    @Override
    public ObservableTransformer<Object, ?> clearStrokes() {
        return new ObservableTransformer<Object, Object>() {
            @Override
            public ObservableSource<Object> apply(Observable<Object> upstream) {
                if (mUndoRedoManipulator != null) {
                    return upstream
                        .ofType(Boolean.class)
                        // Only have downstream do thing if receiving a TRUE.
                        .filter(new Predicate<Boolean>() {
                            @Override
                            public boolean test(Boolean ifClear)
                                throws Exception {
                                if (ifClear) {
                                    // Send analytics event.
                                    mLogger.sendEvent("Doodle editor - clear");

                                    // Enable flag that apply change for close
                                    // button.
                                    mIfApplyChangeForClose.set(true);
                                }

                                return ifClear;
                            }
                        })
                        .compose(mUndoRedoManipulator.clearAll(SketchEditorPresenter.this))
                        .compose(mUpdateCanvasStrokes)
                        .compose(mPostTouchCanvas)
                        .compose(ObservableConst.FILTER_IGNORED);
                } else {
                    return upstream;
                }
            }
        };
    }

    @Override
    public ObservableTransformer<Object, ?> done() {
        return new ObservableTransformer<Object, Object>() {
            @Override
            public ObservableSource<Object> apply(Observable<Object> upstream) {
                return upstream
                    // Don't animate if the animation is running already.
                    .filter(new Predicate<Object>() {
                        @Override
                        public boolean test(Object o) throws Exception {
                            return !mSketchView.isAnimating();
                        }
                    })
                    .observeOn(mUiScheduler)
                    .flatMap(new Function<Object, ObservableSource<?>>() {
                        @Override
                        public ObservableSource<?> apply(Object ignored)
                            throws Exception {
                            return mSketchView.resetAnimation();
                        }
                    })
                    .ofType(Integer.class)
                    .map(new Function<Integer, Object>() {
                        @Override
                        public Object apply(Integer progress)
                            throws Exception {
                            // Send analytics event.
                            mLogger.sendEvent("Doodle editor - finish doodle",
                                              "stroke_count", String.valueOf(mSketchModel.getStrokeSize()));

                            // Prepare the extra data like stroke color and
                            // width (0-100).
                            final int strokeSize = mSketchModel.getStrokeSize();
                            final int strokeWidth = mStrokeWidthProgress;
                            final int color = strokeSize > 0 ?
                                mSketchModel.getStrokeAt(strokeSize - 1).getColor() : 0;

                            // Close editor when the animation ends.
                            if (progress == 100) {
                                mEditorView.closeWithUpdate(
                                    mSketchModel.clone(),
                                    color,
                                    strokeWidth);
                            }

                            return progress;
                        }
                    });
            }
        };
    }

    @Override
    public ObservableTransformer<Object, ?> close() {
        return new ObservableTransformer<Object, Object>() {
            @Override
            public ObservableSource<Object> apply(Observable<Object> upstream) {
                return upstream
                    // Don't animate if the animation is running already.
                    .filter(new Predicate<Object>() {
                        @Override
                        public boolean test(Object o) throws Exception {
                            return !mSketchView.isAnimating();
                        }
                    })
                    .flatMap(new Function<Object, ObservableSource<?>>() {
                        @Override
                        public ObservableSource<?> apply(Object ignored)
                            throws Exception {
                            // Send analytics event.
                            mLogger.sendEvent("Doodle editor - cancel");

                            // A flag indicating the presenter is dead.
                            mIsAlive.set(false);
                            // Ask view to play reset animation.
                            return mSketchView.resetAnimation();
                        }
                    })
                    .ofType(Integer.class)
                    .map(new Function<Integer, Object>() {
                        @Override
                        public Object apply(Integer progress)
                            throws Exception {
                            // Close editor when the animation ends.
                            if (progress == 100) {
                                if (mIfApplyChangeForClose.get()) {
                                    // Prepare the extra data like stroke color and
                                    // width (0-100).
                                    final int strokeSize = mSketchModel.getStrokeSize();
                                    final int strokeWidth = mStrokeWidthProgress;
                                    final int color = strokeSize > 0 ?
                                        mSketchModel.getStrokeAt(strokeSize - 1).getColor() : 0;

                                    mEditorView.closeWithUpdate(
                                        mSketchModel.clone(),
                                        color,
                                        strokeWidth);
                                } else {
                                    mEditorView.close();
                                }
                            }

                            return progress;
                        }
                    });
            }
        };
    }

    ///////////////////////////////////////////////////////////////////////////
    // Protected / Private Methods ////////////////////////////////////////////

    private ObservableTransformer<Object, ?> mPostTouchCanvas =
        new ObservableTransformer<Object, Object>() {
            @Override
            public ObservableSource<Object> apply(Observable<Object> upstream) {
                return upstream.publish(new Function<Observable<Object>, ObservableSource<Object>>() {
                    @Override
                    public ObservableSource<Object> apply(Observable<Object> shared)
                        throws Exception {
                        return Observable.mergeArray(
                            // Fullscreen mode.
                            shared.ofType(DrawStrokeEvent.class)
                                  .compose(mEnterFullscreenMode),
                            // Undo/redo.
                            shared.ofType(UndoRedoEvent.class)
                                  .compose(mUpdateUndoRedoUi),
                            // DEBUG.
                            shared.ofType(GestureEvent.class)
                                  .compose(mDebug)
                        );
                    }
                });
            }
        };

    private ObservableTransformer<DrawStrokeEvent, ?> mEnterFullscreenMode =
        new ObservableTransformer<DrawStrokeEvent, Object>() {
            @Override
            public ObservableSource<Object> apply(Observable<DrawStrokeEvent> upstream) {
                return upstream
                    .observeOn(mUiScheduler)
                    .map(new Function<DrawStrokeEvent, Object>() {
                        @Override
                        public Object apply(DrawStrokeEvent event)
                            throws Exception {
                            mEditorView.enableFullscreenMode(event.drawing);
                            return event;
                        }
                    });
            }
        };

    private ObservableTransformer<UndoRedoEvent, ?> mUpdateUndoRedoUi =
        new ObservableTransformer<UndoRedoEvent, Object>() {
            @Override
            public ObservableSource<Object> apply(Observable<UndoRedoEvent> upstream) {
                return upstream
                    .observeOn(mUiScheduler)
                    .map(new Function<UndoRedoEvent, Object>() {
                        @Override
                        public Object apply(UndoRedoEvent event)
                            throws Exception {
                            if (event.sizeOfRedo == 0 && event.sizeOfUndo == 0) {
                                mEditorView.showOrHideUndoButton(false, false);
                                mEditorView.showOrHideRedoButton(false, false);
                                mEditorView.showOrHideDoneButton(false);
                            } else {
                                mEditorView.showOrHideUndoButton(true, event.sizeOfUndo > 0);
                                mEditorView.showOrHideRedoButton(true, event.sizeOfRedo > 0);
                                mEditorView.showOrHideDoneButton(true);
                            }

                            mEditorView.showOrHideClearButton(event.sizeOfUndo > 0);
                            return event;
                        }
                    });
            }
        };

    private ObservableTransformer<? super GestureEvent, ?> mDebug =
        new ObservableTransformer<GestureEvent, Object>() {
            @Override
            public ObservableSource<Object> apply(Observable<GestureEvent> upstream) {
                return upstream.map(new Function<GestureEvent, Object>() {
                    @Override
                    public Object apply(GestureEvent event)
                        throws Exception {
                        if (!event.justStart && !event.doing) {
                            mLogger.d("sketch", "GestureStop: model=" + mSketchModel);
                        }

                        return event;
                    }
                });
            }
        };

    private ObservableTransformer<DrawStrokeEvent, DrawStrokeEvent> mDrawStrokeOnCanvas =
        new ObservableTransformer<DrawStrokeEvent, DrawStrokeEvent>() {
            @Override
            public ObservableSource<DrawStrokeEvent> apply(Observable<DrawStrokeEvent> upstream) {
                return upstream.map(new Function<DrawStrokeEvent, DrawStrokeEvent>() {
                    @Override
                    public DrawStrokeEvent apply(DrawStrokeEvent event)
                        throws Exception {
                        if (event.justStart) {
                            // Start...
                            mSketchView.drawStrokeFrom(event.strokes.get(0),
                                                       event.from);
                        } else if (event.drawing) {
                            // Drawing...
                            mSketchView.drawStrokeFrom(event.strokes.get(0),
                                                       event.from);
                        } else {
                            // Just stop...
                            mSketchView.debugStrokes(mSketchModel.getAllStrokes());
                        }
                        return event;
                    }
                });
            }
        };

    private ObservableTransformer<PinchEvent, ?> mUpdateCanvasMatrix =
        new ObservableTransformer<PinchEvent, Object>() {
            @Override
            public ObservableSource<Object> apply(Observable<PinchEvent> upstream) {
                return upstream.map(new Function<PinchEvent, Object>() {
                    @Override
                    public Object apply(PinchEvent event)
                        throws Exception {
                        if (event.doing) {
                            // Doing...

                            // We only care about the update matrix.
                            mSketchView.updateCanvasMatrix(event.parentToTargetMatrix);
                        } else if (!event.justStart) {
                            // Stop...

                            // Update the stroke width after canvas is scaled.
                            mDrawStrokeManipulator.setBrushSize(getStrokeBaseWidth(), mStrokeWidthProgress);

                            // Let view to do certain post-process after stop
                            // updating canvas matrix.
                            mSketchView.stopUpdatingCanvasMatrix(
                                (event.pointer1.x + event.pointer2.x) / 2f,
                                (event.pointer1.y + event.pointer2.y) / 2f);

                            // Let view to sharpen the strokes after stop
                            // updating canvas matrix.
                            mSketchView.drawAndSharpenStrokes(mSketchModel.getAllStrokes());
                        }

                        return ObservableConst.IGNORED;
                    }
                });
            }
        };

    private ObservableTransformer<List<SketchStrokeModel>, ?> mUpdateCanvasStrokes =
        new ObservableTransformer<List<SketchStrokeModel>, UndoRedoEvent>() {
            @Override
            public ObservableSource<UndoRedoEvent> apply(Observable<List<SketchStrokeModel>> upstream) {
                return upstream
                    .observeOn(mUiScheduler)
                    .map(new Function<List<SketchStrokeModel>, UndoRedoEvent>() {
                        @Override
                        public UndoRedoEvent apply(List<SketchStrokeModel> strokes)
                            throws Exception {
                            mSketchView.eraseCanvas();
                            mSketchView.drawStrokes(strokes);

                            // DEBUG.
                            mSketchView.debugStrokes(mSketchModel.getAllStrokes());

                            return UndoRedoEvent.create(mUndoRedoManipulator.sizeOfUndo(),
                                                        mUndoRedoManipulator.sizeOfRedo());
                        }
                    });
            }
        };

    private ObservableTransformer<Object, UiModel> mToUiModelTransformer =
        new ObservableTransformer<Object, UiModel>() {
            @Override
            public ObservableSource<UiModel> apply(Observable<Object> upstream) {
                return upstream
                    .map(new Function<Object, UiModel>() {
                        @Override
                        public UiModel apply(Object o) throws Exception {
                            return UiModel.succeed(o);
                        }
                    })
                    .onErrorReturn(new Function<Throwable, UiModel>() {
                        @Override
                        public UiModel apply(Throwable error) throws Exception {
                            return UiModel.failed(error);
                        }
                    })
                    .startWith(UiModel.inProgress(ObservableConst.IGNORED));
            }
        };

    private ObservableTransformer<Object, Object> mUiModelObserver =
        new ObservableTransformer<Object, Object>() {
            @Override
            public ObservableSource<Object> apply(Observable<Object> upstream) {
                return upstream
                    .ofType(UiModel.class)
                    .observeOn(mUiScheduler)
                    .map(new Function<UiModel, Object>() {
                        @Override
                        public Object apply(UiModel vm) throws Exception {
                            if (vm.isInProgress) {
                                // Increase the number of ongoing tasks by one.
                                mCountOfDoing.incrementAndGet();

                                mEditorView.showProgress("Loading...");
                            } else if (vm.isSuccessful) {
                                // Decrease the number of ongoing tasks by one.
                                if (mCountOfDoing.decrementAndGet() == 0) {
                                    mEditorView.hideProgress();
                                }
                            } else {
                                if (mCountOfDoing.decrementAndGet() == 0) {
                                    mEditorView.hideProgress();
//                                    mEditorView.showErrorAlertThenClose(vm.error);
                                }
                            }

                            return ObservableConst.IGNORED;
                        }
                    })
                    .compose(ObservableConst.FILTER_IGNORED);
            }
        };

    private void prepareBrushes(final int brushColor,
                                final int brushSize) {
        mBrushes.addAll(
            new SketchBrushFactory()
                .addEraserBrush()
                .addColorBrush(0xFFFFFFFF)
                .addColorBrush(0xFF000000)
                .addColorBrush(0xFF3897F0)
                .addColorBrush(0xFF70C050)
                .addColorBrush(0xFFFDCB5C)
                .addColorBrush(0xFFFD8D32)
                .addColorBrush(0xFFED4956)
                .addColorBrush(0xFFD13076)
                .addColorBrush(0xFFA307BA)
                .addColorBrush(0xFFFFB7C5)
                .addColorBrush(0xFFD1AF94)
                .addColorBrush(0xFF97D5E0)
                .addColorBrush(0xFF4FC3C6)
                .addColorBrush(0xFF0C4C8A)
                .addColorBrush(0xFF5C7148)
                .addColorBrush(0xFF262626)
                .addColorBrush(0xFF595959)
                .addColorBrush(0xFF7F7F7F)
                .addColorBrush(0xFF999999)
                .addColorBrush(0xFFB3B3B3)
                .addColorBrush(0xFFCCCCCC)
                .addColorBrush(0xFFE6E6E6)
                .build());

        // Update brush colors and set default color selection.
        if (brushColor == 0) {
            mEditorView.setBrushItemsAndSelectAt(mBrushes, 3);
        } else {
            final int rgb = brushColor & 0xFFFFFF;

            for (int i = 0; i < mBrushes.size(); ++i) {
                final ISketchBrush brush = mBrushes.get(i);
                final int otherRgb = brush.getBrushColor() & 0xFFFFFF;

                if (rgb == otherRgb) {
                    mEditorView.setBrushItemsAndSelectAt(mBrushes, i);
                    break;
                }
            }
        }

        // Set default brush size.
        if (brushSize < 0 || brushSize > 100) {
            mEditorView.setBrushSize(20);
        } else {
            mEditorView.setBrushSize(brushSize);
        }
    }

    /**
     * The stroke based width is a reference to determine the exact value. In
     * order to make the stroke width consistent to the screen resolution, some
     * calculation will be applied to.
     */
    private int getStrokeBaseWidth() {
        float scale = mSketchView.getMatrixOfTargetToParent().getScaleX();

        return (int) (mSketchModel.getWidth() * scale);
    }

    @Override
    public SketchModel getSketchModel() {
        return mSketchModel;
    }
}
