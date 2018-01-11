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

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;

import ru.terrakok.cicerone.Navigator;
import ru.terrakok.cicerone.commands.Back;
import ru.terrakok.cicerone.commands.Command;
import ru.terrakok.cicerone.commands.Forward;
import ru.terrakok.cicerone.result.ResultListener;

final class InnerRouter {

    // Command buffer.
    private final Queue<Command> mCommandBuffer = new ArrayBlockingQueue<>(1 << 6);

    // FutureResult pool.
    private Map<Integer, ListenerAndResult> mResultsBuffer = new HashMap<>();

    // Navigator holder.
    private INavigator mNavigator = null;

    // The handler of main looper.
    private final Handler mUiHandler;
    private final String mName;

    InnerRouter(String name,
                Handler uiHandler) {
        throwExceptionIfNotUiThread();

        mName = name;
        mUiHandler = uiHandler;
    }

    @Override
    public String toString() {
        return super.toString();
    }

    /**
     * Set an active Navigator for the Cicerone and start receive commands.
     *
     * @param navigator new active Navigator
     */
    void setNavigator(INavigator navigator) {
        throwExceptionIfNotUiThread();

        if (mNavigator != null) {
            mNavigator.onExit();
        }
        mNavigator = navigator;
        mNavigator.onEnter();

        processPendingCommand();
    }

    /**
     * Remove the current Navigator and stop receive commands.
     */
    void unsetNavigator() {
        throwExceptionIfNotUiThread();

        if (mNavigator != null) {
            mNavigator.onExit();
            mNavigator = null;
        }
    }

    void bindContextToNavigator(final Context context) {
        throwExceptionIfNotUiThread();

        mNavigator.bindContext(context);
    }

    void unBindContextFromNavigator() {
        throwExceptionIfNotUiThread();

        mNavigator.unBindContext();
    }

    /**
     * Subscribe to the screen resultHolder.<br>
     * <b>Note:</b> only one listenerHolder can subscribe to a unique requestCode!<br>
     * You must call a <b>removeResultListener()</b> to avoid a memory leak.
     *
     * @param requestCode key for filter results
     * @param listener    resultHolder listenerHolder
     */
    void addResultListener(int requestCode, ru.terrakok.cicerone.result.ResultListener listener) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            throw new IllegalThreadStateException(
                "Should be called in the main thread.");
        }

        if (mResultsBuffer.containsKey(requestCode)) {
            final ListenerAndResult buffer = mResultsBuffer.get(requestCode);
            buffer.listenerHolder.set(listener);
        } else {
            final ListenerAndResult buffer = new ListenerAndResult();
            buffer.listenerHolder.set(listener);

            mResultsBuffer.put(requestCode, buffer);
        }
    }

    /**
     * Unsubscribe from the screen resultHolder.
     *
     * @param requestCode key for filter results
     */
    void removeResultListener(int requestCode) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            throw new IllegalThreadStateException(
                "Should be called in the main thread.");
        }

        if (mResultsBuffer.containsKey(requestCode)) {
            mResultsBuffer.remove(requestCode);
        }
    }

    /**
     * Call this in onResume to get the buffer resultHolder by requestCode.
     */
    void dispatchResultOnResume() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            throw new IllegalThreadStateException(
                "Should be called in the main thread.");
        }

        for (Integer requestCode : mResultsBuffer.keySet()) {
            final ListenerAndResult buffer = mResultsBuffer.get(requestCode);
            final Object result = buffer.resultHolder.get();
            final ru.terrakok.cicerone.result.ResultListener listener = buffer.listenerHolder.get();
            if (result != null && listener != null) {
                listener.onResult(result);
                // Clear the cache.
                buffer.resultHolder.set(null);
            }
        }
    }

    /**
     * Open new screen and add it to the screens chain.
     *
     * @param screenKey screen key
     */
    void navigateTo(String screenKey) {
        navigateTo(screenKey, null);
    }

    /**
     * Open new screen and add it to screens chain.
     *
     * @param screenKey screen key
     * @param data      initialisation parameters for the new screen
     */
    void navigateTo(String screenKey, Object data) {
        addPendingCommand(new Forward(screenKey, data));
    }

    /**
     * Return to the previous screen in the chain.
     * Behavior in the case when the current screen is the root depends on
     * the processing of the {@link Back} command in a {@link Navigator} implementation.
     */
    void exit() {
        addPendingCommand(new Back());
    }

    /**
     * Return to the previous screen in the chain and send resultHolder data.
     *
     * @param requestCode resultHolder data key
     * @param result      resultHolder data
     */
    void exitWithResult(int requestCode, Object result) {
        exit();
        sendDelayedResult(requestCode, result);
    }

    /**
     * Send a delayed resultHolder until the {@link #dispatchResultOnResume()} is
     * called.
     *
     * @param requestCode resultHolder data key
     * @param result      resultHolder data
     */
    void sendDelayedResult(int requestCode, Object result) {
        if (mResultsBuffer.containsKey(requestCode)) {
            mResultsBuffer.get(requestCode)
                .resultHolder
                .set(result);
        } else {
            mResultsBuffer.put(requestCode, new ListenerAndResult(result));
        }
    }

    void addPendingCommand(Command command) {
        mCommandBuffer.offer(command);
        processPendingCommand();
    }

    ///////////////////////////////////////////////////////////////////////////
    // Protected / Private Methods ////////////////////////////////////////////

    private void processPendingCommand() {
        // Drop pending runnable.
        mUiHandler.removeCallbacks(mProcessCommandRunnable);
        // Send new runnable.
        mUiHandler.post(mProcessCommandRunnable);
    }

    private Runnable mProcessCommandRunnable = new Runnable() {
        @Override
        public void run() {
            if (!mCommandBuffer.isEmpty() &&
                mNavigator != null) {
                final Command command = mCommandBuffer.poll();
                mNavigator.applyCommandAndWait(command, mFutureResult);
            }
        }
    };

    private INavigator.FutureResult mFutureResult = new INavigator.FutureResult() {
        @Override
        public void finish() {
            // Keep processing the pending commands.
            processPendingCommand();
        }
    };

    private void throwExceptionIfNotUiThread() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            throw new IllegalThreadStateException(
                "Should be called in the main thread.");
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Clazz //////////////////////////////////////////////////////////////////

    private static class ListenerAndResult {

        final AtomicReference<ResultListener> listenerHolder = new AtomicReference<>();
        final AtomicReference<Object> resultHolder = new AtomicReference<>();

        ListenerAndResult() {
            // EMPTY constructor.
        }

        ListenerAndResult(Object result) {
            resultHolder.set(result);
        }
    }
}
