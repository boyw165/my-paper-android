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

import com.cardinalblue.lib.doodle.event.DrawStrokeEvent;
import com.cardinalblue.lib.doodle.event.UndoRedoEvent;
import com.cardinalblue.lib.doodle.protocol.ILogger;
import com.cardinalblue.lib.doodle.protocol.ISketchModel;
import com.cardinalblue.lib.doodle.protocol.ISketchStroke;
import com.cardinalblue.lib.doodle.protocol.SketchContract;
import com.cardinalblue.lib.history.UndoRedoList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.ObservableTransformer;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;

public class SketchUndoRedoManipulator implements SketchContract.ISketchUndoRedoManipulator {

    // Given...
    private final ILogger mLogger;

    // Record.
    private final UndoRedoList<List<ISketchStroke>> mRecords;

    public SketchUndoRedoManipulator(ILogger logger) {
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
    public ObservableTransformer<Object, List<ISketchStroke>> undo(final ISketchModel sketchModel) {
        return new ObservableTransformer<Object, List<ISketchStroke>>() {
            @Override
            public ObservableSource<List<ISketchStroke>> apply(Observable<Object> upstream) {
                return upstream
                    .filter(new Predicate<Object>() {
                        @Override
                        public boolean test(Object ignored) throws Exception {
                            return mRecords.sizeOfUndo() > 0;
                        }
                    })
                    .map(new Function<Object, List<ISketchStroke>>() {
                        @Override
                        public List<ISketchStroke> apply(Object o)
                            throws Exception {
                            // Send analytics event.
                            mLogger.sendEvent("Doodle editor - undo");

                            List<ISketchStroke> prev = mRecords.undo();
                            if (prev == null) {
                                prev = Collections.emptyList();
                            }

                            // TODO: Recursively clone.
                            sketchModel.setStrokes(prev);

                            return prev;
                        }
                    });
            }
        };
    }

    @Override
    public ObservableTransformer<Object, List<ISketchStroke>> undoAll(final ISketchModel sketchModel) {
        return new ObservableTransformer<Object, List<ISketchStroke>>() {
            @Override
            public ObservableSource<List<ISketchStroke>> apply(Observable<Object> upstream) {
                return upstream.map(new Function<Object, List<ISketchStroke>>() {
                    @Override
                    public List<ISketchStroke> apply(Object ignored)
                        throws Exception {
                        // Rollback all the undo and keep all the redo records.
                        while (mRecords.sizeOfUndo() > 0) {
                            mRecords.undo();
                        }

                        sketchModel.clearStrokes();

                        return Collections.emptyList();
                    }
                });
            }
        };
    }

    @Override
    public ObservableTransformer<Object, List<ISketchStroke>> redo(final ISketchModel sketchModel) {
        return new ObservableTransformer<Object, List<ISketchStroke>>() {
            @Override
            public ObservableSource<List<ISketchStroke>> apply(Observable<Object> upstream) {
                return upstream
                    .filter(new Predicate<Object>() {
                        @Override
                        public boolean test(Object ignored) throws Exception {
                            return mRecords.sizeOfRedo() > 0;
                        }
                    })
                    .map(new Function<Object, List<ISketchStroke>>() {
                        @Override
                        public List<ISketchStroke> apply(Object o)
                            throws Exception {
                            // Send analytics event.
                            mLogger.sendEvent("Doodle editor - redo");

                            final List<ISketchStroke> next = mRecords.redo();

                            // TODO: Recursively clone.
                            sketchModel.setStrokes(next);

                            return next;
                        }
                    });
            }
        };
    }

    @Override
    public ObservableTransformer<Object, List<ISketchStroke>> clearAll(final ISketchModel sketchModel) {
        return new ObservableTransformer<Object, List<ISketchStroke>>() {
            @Override
            public ObservableSource<List<ISketchStroke>> apply(Observable<Object> upstream) {
                return upstream.map(new Function<Object, List<ISketchStroke>>() {
                    @Override
                    public List<ISketchStroke> apply(Object ignored)
                        throws Exception {
                        mRecords.clear();
                        sketchModel.clearStrokes();

                        return Collections.emptyList();
                    }
                });
            }
        };
    }

    @Override
    public ObservableTransformer<Object, ?> onSpyingStrokesUpdate(final ISketchModel sketchModel) {
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
                                        // Stop drawing... it's about time to add undo record.
                                        if (sketchModel.getAllStrokes().size() > 0 &&
                                            event.isModelChanged) {
                                            List<ISketchStroke> src = sketchModel.getAllStrokes();
                                            // TODO: Recursively clone.
                                            List<ISketchStroke> copy = new ArrayList<>(src);
                                            mRecords.add(copy);
                                        }

                                        return UndoRedoEvent.create(mRecords.sizeOfUndo(),
                                                                    mRecords.sizeOfRedo());
                                    }
                                }),
                            // By list of ISketchStroke.
                            Observable
                                .just(o)
                                .ofType(List.class)
                                .map(new Function<List, UndoRedoEvent>() {
                                    @Override
                                    public UndoRedoEvent apply(List list) throws Exception {
                                        final List<ISketchStroke> strokes = new ArrayList<>();
                                        for (int i = 0; i < list.size(); ++i) {
                                            final Object data = list.get(i);
                                            if (data instanceof ISketchStroke) {
                                                // Accumulate strokes.
                                                strokes.add((ISketchStroke) data);

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
