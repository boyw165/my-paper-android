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

package com.cardinalblue.lib.doodle.view.adapter;

import com.cardinalblue.lib.doodle.protocol.IBrushListener;
import com.cardinalblue.lib.doodle.protocol.ISketchBrush;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.MainThreadDisposable;

public class BrushAdapterObservable extends Observable<Integer> {

    private final BrushAdapter mSource;

    public BrushAdapterObservable(BrushAdapter source) {
        mSource = source;
    }

    @Override
    protected void subscribeActual(Observer<? super Integer> observer) {
        final Disposable disposable = new Disposable(observer, mSource);

        // Set listener before deliver the disposable to downstream.
        mSource.setListener(disposable);

        // Deliver the disposable to downstream.
        observer.onSubscribe(disposable);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Clazz //////////////////////////////////////////////////////////////////

    private static final class Disposable
        extends MainThreadDisposable
        implements IBrushListener {

        private final BrushAdapter source;
        private final Observer<? super Integer> observer;

        Disposable(Observer<? super Integer> observer,
                   BrushAdapter source) {
            this.observer = observer;
            this.source = source;
        }

        @Override
        protected void onDispose() {
            source.setListener(null);
        }

        @Override
        public void onClickBrush(int position,
                                 ISketchBrush brush) {
            if (!isDisposed()) {
                observer.onNext(position);
            }
        }
    }
}
