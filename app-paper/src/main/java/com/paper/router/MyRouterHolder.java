// Copyright Jan 2018-present CardinalBlue
//
// Author: jack.huang@cardinalblue.com
//         boy@cardinalblue.com
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

package com.paper.router;

import android.os.Looper;

import java.util.Stack;

public class MyRouterHolder extends Stack<MyRouter> {

    @Override
    public MyRouter push(MyRouter item) {
        throwExceptionIfNotMainThread();

        // TODO: Add the linkage.

        return super.push(item);
    }

    @Override
    public synchronized MyRouter pop() {
        throwExceptionIfNotMainThread();
        return super.pop();
    }

    @Override
    public synchronized MyRouter peek() {
        throwExceptionIfNotMainThread();
        return super.peek();
    }

    @Override
    public boolean empty() {
        throwExceptionIfNotMainThread();
        return super.empty();
    }

    @Override
    public synchronized int search(Object o) {
        throwExceptionIfNotMainThread();
        return super.search(o);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Protected / Private Methods ////////////////////////////////////////////

    private void throwExceptionIfNotMainThread() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            throw new IllegalThreadStateException("Should be in the Main thread.");
        }
    }
}
