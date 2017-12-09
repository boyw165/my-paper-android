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

package com.cardinalblue.lib.doodle;

import com.cardinalblue.lib.doodle.event.DrawStrokeEvent;
import com.cardinalblue.lib.doodle.event.UndoRedoEvent;
import com.cardinalblue.lib.doodle.model.UndoRedoList;
import com.paper.shared.model.sketch.Sketch;
import com.paper.shared.model.sketch.SketchStroke;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.ObservableTransformer;
import io.reactivex.Scheduler;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;

public class SketchUndoRedoManipulator implements SketchContract.ISketchUndoRedoManipulator {

    // Given...
    private final Scheduler mWorkerScheduler;
    private final Scheduler mUiScheduler;
    private final ILogger mLogger;

    // Record.
    private final UndoRedoList<List<SketchStroke>> mRecords;

    public SketchUndoRedoManipulator(Scheduler workerScheduler,
                                     Scheduler uiScheduler,
                                     ILogger logger) {
        mWorkerScheduler = workerScheduler;
        mUiScheduler = uiScheduler;
        mLogger = logger;

        mRecords = new UndoRedoList<>();
    }

    @Override
    public int sizeOfUndo() {
        return mRecords.sizeOfUndo();
    }

    @Override
    public int sizeOfRedo() {
        return mRecords.sizeOfRedo();
    }

    @Override
    public ObservableTransformer<Object, List<SketchStroke>> undo(final SketchContract.IModelProvider modelProvider) {
        return new ObservableTransformer<Object, List<SketchStroke>>() {
            @Override
            public ObservableSource<List<SketchStroke>> apply(Observable<Object> upstream) {
                return upstream
                    .filter(new Predicate<Object>() {
                        @Override
                        public boolean test(Object ignored) throws Exception {
                            return mRecords.sizeOfUndo() > 0;
                        }
                    })
                    .map(new Function<Object, List<SketchStroke>>() {
                        @Override
                        public List<SketchStroke> apply(Object o)
                            throws Exception {
                            // Send analytics event.
                            mLogger.sendEvent("Doodle editor - undo");

                            List<SketchStroke> prev = mRecords.undo();
                            if (prev == null) {
                                prev = Collections.emptyList();
                            }

                            // TODO: Recursively clone.
                            modelProvider.getSketchModel().setStrokes(prev);

                            return prev;
                        }
                    });
            }
        };
    }

    @Override
    public ObservableTransformer<Object, List<SketchStroke>> undoAll(final SketchContract.IModelProvider modelProvider) {
        return new ObservableTransformer<Object, List<SketchStroke>>() {
            @Override
            public ObservableSource<List<SketchStroke>> apply(Observable<Object> upstream) {
                return upstream.map(new Function<Object, List<SketchStroke>>() {
                    @Override
                    public List<SketchStroke> apply(Object ignored)
                        throws Exception {
                        // Rollback all the undo and keep all the redo records.
                        while (mRecords.sizeOfUndo() > 0) {
                            mRecords.undo();
                        }

                        modelProvider.getSketchModel().clearStrokes();

                        return Collections.emptyList();
                    }
                });
            }
        };
    }

    @Override
    public ObservableTransformer<Object, List<SketchStroke>> redo(final SketchContract.IModelProvider modelProvider) {
        return new ObservableTransformer<Object, List<SketchStroke>>() {
            @Override
            public ObservableSource<List<SketchStroke>> apply(Observable<Object> upstream) {
                return upstream
                    .filter(new Predicate<Object>() {
                        @Override
                        public boolean test(Object ignored) throws Exception {
                            return mRecords.sizeOfRedo() > 0;
                        }
                    })
                    .map(new Function<Object, List<SketchStroke>>() {
                        @Override
                        public List<SketchStroke> apply(Object o)
                            throws Exception {
                            // Send analytics event.
                            mLogger.sendEvent("Doodle editor - redo");

                            final List<SketchStroke> next = mRecords.redo();

                            // TODO: Recursively clone.
                            modelProvider.getSketchModel().setStrokes(next);

                            return next;
                        }
                    });
            }
        };
    }

    @Override
    public ObservableTransformer<Object, List<SketchStroke>> clearAll(final SketchContract.IModelProvider modelProvider) {
        return new ObservableTransformer<Object, List<SketchStroke>>() {
            @Override
            public ObservableSource<List<SketchStroke>> apply(Observable<Object> upstream) {
                return upstream.map(new Function<Object, List<SketchStroke>>() {
                    @Override
                    public List<SketchStroke> apply(Object ignored)
                        throws Exception {
                        mRecords.clear();
                        modelProvider.getSketchModel().clearStrokes();

                        return Collections.emptyList();
                    }
                });
            }
        };
    }

    @Override
    public ObservableTransformer<Object, ?> onSpyingStrokesUpdate(final SketchContract.IModelProvider modelProvider) {
        return new ObservableTransformer<Object, Object>() {
            @Override
            public ObservableSource<Object> apply(@NonNull Observable<Object> upstream) {
                return upstream.flatMap(new Function<Object, ObservableSource<Object>>() {
                    @Override
                    public ObservableSource<Object> apply(Object o) throws Exception {
                        return Observable.mergeArray(
                            // Also emit the original data from upstream to
                            // downstream.
                            Observable.just(o),
                            // By draw-stroke-event.
                            Observable
                                .just(o)
                                .ofType(DrawStrokeEvent.class)
                                // Only accept when STOP is received.
                                .filter(new Predicate<DrawStrokeEvent>() {
                                    @Override
                                    public boolean test(DrawStrokeEvent event) throws Exception {
                                        return !event.justStart && !event.drawing;
                                    }
                                })
                                .map(new Function<DrawStrokeEvent, UndoRedoEvent>() {
                                    @Override
                                    public UndoRedoEvent apply(DrawStrokeEvent event) throws Exception {
                                        final Sketch sketch = modelProvider.getSketchModel();

                                        // Stop drawing... it's about time to add undo record.
                                        if (sketch.getAllStrokes().size() > 0 &&
                                            event.isModelChanged) {
                                            List<SketchStroke> src = sketch.getAllStrokes();
                                            // TODO: Recursively clone.
                                            List<SketchStroke> copy = new ArrayList<>(src);
                                            mRecords.add(copy);
                                        }

                                        return UndoRedoEvent.create(mRecords.sizeOfUndo(),
                                                                    mRecords.sizeOfRedo());
                                    }
                                }),
                            // By list of SketchStroke.
                            Observable
                                .just(o)
                                .ofType(List.class)
                                .map(new Function<List, UndoRedoEvent>() {
                                    @Override
                                    public UndoRedoEvent apply(List list) throws Exception {
                                        final List<SketchStroke> strokes = new ArrayList<>();
                                        for (int i = 0; i < list.size(); ++i) {
                                            final Object data = list.get(i);
                                            if (data instanceof SketchStroke) {
                                                // Accumulate strokes.
                                                strokes.add((SketchStroke) data);

                                                // Add to record.
                                                mRecords.add(new ArrayList<>(strokes));
                                            }
                                        }

                                        return UndoRedoEvent.create(mRecords.sizeOfUndo(),
                                                                    mRecords.sizeOfRedo());
                                    }
                                }));
                    }
                });
            }
        };
    }
}
