// Copyright (c) 2017-present boyw165
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

package com.my.core.benchmark;

import com.my.core.protocol.ILogger;
import com.my.core.protocol.IProfiler;
import com.my.core.protocol.ISystemClock;

import java.util.Locale;
import java.util.Stack;

/**
 * Log class name, method, line number and interval.
 *
 * <br/>
 * Usage:
 * <pre>
 *     final IProfiler profiler = new LogProfiler(new FabricLogger());
 *
 *     profiler.startProfiling("Bla bla bla");
 *
 *     // ... code you want to profile ...
 *
 *     profiler.stopProfiling();
 * </pre>
 */
public class LogProfiler implements IProfiler,
                                    ISystemClock {

    private static final String TAG = "profiler";
    private final Object mMutex = new Object();

    // Given...
    private final ILogger mLogger;

    private Stack<MessageAndTimestampRecord> mRecords = new Stack<>();

    public LogProfiler(ILogger logger) {
        mLogger = logger;
    }

    @Override
    public void startProfiling() {
        startProfiling("");
    }

    @Override
    public void startProfiling(String message) {
        synchronized (mMutex) {
            mRecords.push(new MessageAndTimestampRecord(
                message, getCurrentTimeMillis()));
        }
    }

    @Override
    public void stopProfiling() {
        synchronized (mMutex) {
            if (mRecords.isEmpty()) return;

            final String fullClassName = Thread.currentThread().getStackTrace()[3].getClassName();
            final String className = fullClassName.substring(fullClassName.lastIndexOf(".") + 1);
            final String methodName = Thread.currentThread().getStackTrace()[3].getMethodName();
            final int lineNumber = Thread.currentThread().getStackTrace()[3].getLineNumber();

            final MessageAndTimestampRecord record = mRecords.pop();
            final long interval = getCurrentTimeMillis() - record.timestamp;

            // Compose message.
            final String msg = String.format(
                Locale.ENGLISH,
                "%s::%s L%d - %s (took %d ms)",
                className, methodName, lineNumber,
                record.message,
                interval);

            if (mLogger != null) {
                mLogger.d(TAG, msg);
            }
        }
    }

    @Override
    public long getCurrentTimeMillis() {
        return System.currentTimeMillis();
    }

    @Override
    public long getCurrentTimeNanos() {
        return System.nanoTime();
    }

    ///////////////////////////////////////////////////////////////////////////
    // Clazz //////////////////////////////////////////////////////////////////

    private static class MessageAndTimestampRecord {

        final String message;
        final long timestamp;

        MessageAndTimestampRecord(String message,
                                  long timestamp) {
            this.message = message;
            this.timestamp = timestamp;
        }
    }
}
