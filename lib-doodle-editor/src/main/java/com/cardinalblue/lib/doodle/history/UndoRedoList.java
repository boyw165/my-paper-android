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

package com.cardinalblue.lib.doodle.history;

import java.util.Stack;

public class UndoRedoList<T> {

    private final Object mMutex = new Object();

    private int mMax = -1;
    /**
     * The undo records, should be FIFO
     */
    private final Stack<T> mUndoRecords = new Stack<>();
    /**
     * The redo records, should be FIFO
     */
    private final Stack<T> mRedoRecords = new Stack<>();

    public UndoRedoList(int max) {
        this.mMax = max;
    }

    public UndoRedoList() {
        this.mMax = -1;
    }

    public int getRecordMaximum() {
        return mMax;
    }

    public void add(T item) {
        synchronized (mMutex) {
            mUndoRecords.add(item);
            mRedoRecords.clear();
        }
    }

    public void remove(T item) {
        // FIXME: Complete it.
//        synchronized (mMutex) {
//            mUndoRecords.add(item);
//            mRedoRecords.clear();
//        }
    }

    public T peek() {
        return mUndoRecords.peek();
    }

    public void clear() {
        synchronized (mMutex) {
            mUndoRecords.clear();
            mRedoRecords.clear();
        }
    }

    public int sizeOfUndo() {
        synchronized (mMutex) {
            return mUndoRecords.size();
        }
    }

    public int sizeOfRedo() {
        synchronized (mMutex) {
            return mRedoRecords.size();
        }
    }

    public T undo() {
        if (mUndoRecords.isEmpty()) return null;

        synchronized (mMutex) {
            // Pop undos and add the head to redos.
            //
            // undos: 1, 2, 3, 4, 5
            // redos: 6
            //
            // becomes...
            //
            // undos: 1, 2, 3, 4
            // redos: 6, 5
            final T record = mUndoRecords.pop();
            mRedoRecords.add(record);

            if (mUndoRecords.isEmpty()) {
                return null;
            } else {
                return mUndoRecords.peek();
            }
        }
    }

    public T redo() {
        if (mRedoRecords.isEmpty()) return null;

        synchronized (mMutex) {
            // Pop redos and add the head to undos.
            //
            // undos: 1, 2, 3, 4, 5
            // redos: 6, 5
            //
            // becomes...
            //
            // undos: 1, 2, 3, 4, 5
            // redos: 6
            final T record = mRedoRecords.pop();
            mUndoRecords.add(record);

            return record;
        }
    }
}
