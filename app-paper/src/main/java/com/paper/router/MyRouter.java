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

public class MyRouter {

    // Command buffer.
    private final Queue<Command> mCommandBuffer = new ArrayBlockingQueue<>(1 << 6);

    // FutureResult pool.
    private Map<Integer, ListenerAndResult> mResultsBuffer = new HashMap<>();

    // Navigator holder.
    private INavigator mNavigator = null;

    // The reference to parent router.
    private MyRouter mParent = null;
    private MyRouter mChild = null;

    // The handler of main looper.
    private final Handler mUiHandler;

    public MyRouter(Handler uiHandler) {
        if (uiHandler.getLooper() != Looper.getMainLooper()) {
            throw new IllegalThreadStateException(
                "Should be called in the main thread.");
        }

        mUiHandler = uiHandler;
    }

    /**
     * Set an active Navigator for the Cicerone and start receive commands.
     *
     * @param navigator new active Navigator
     */
    public final void setNavigator(INavigator navigator) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            throw new IllegalThreadStateException(
                "Should be called in the main thread.");
        }

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
    public final void unsetNavigator() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            throw new IllegalThreadStateException(
                "Should be called in the main thread.");
        }

        if (mNavigator != null) {
            mNavigator.onExit();
            mNavigator = null;
        }
    }

    /**
     * Subscribe to the screen resultHolder.<br>
     * <b>Note:</b> only one listenerHolder can subscribe to a unique requestCode!<br>
     * You must call a <b>removeResultListener()</b> to avoid a memory leak.
     *
     * @param requestCode key for filter results
     * @param listener    resultHolder listenerHolder
     */
    public final void addResultListener(int requestCode, ru.terrakok.cicerone.result.ResultListener listener) {
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
    public final void removeResultListener(int requestCode) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            throw new IllegalThreadStateException(
                "Should be called in the main thread.");
        }

        if (mResultsBuffer.containsKey(requestCode)) {
            mResultsBuffer.remove(requestCode);
        }
    }

    public final void setChild(MyRouter child) {
        mChild = child;
    }

    public final void setParent(MyRouter parent) {
        mParent = parent;
    }

    /**
     * Call this in onResume to get the buffer resultHolder by requestCode.
     */
    public void dispatchResultOnResume() {
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
    public void navigateTo(String screenKey) {
        navigateTo(screenKey, null);
    }

    /**
     * Open new screen and add it to screens chain.
     *
     * @param screenKey screen key
     * @param data      initialisation parameters for the new screen
     */
    public void navigateTo(String screenKey, Object data) {
        addPendingCommand(new Forward(screenKey, data));
    }

    /**
     * Return to the previous screen in the chain.
     * Behavior in the case when the current screen is the root depends on
     * the processing of the {@link Back} command in a {@link Navigator} implementation.
     */
    public void exit() {
        addPendingCommand(new Back());
    }

    /**
     * Return to the previous screen in the chain and send resultHolder data.
     *
     * @param requestCode resultHolder data key
     * @param result      resultHolder data
     */
    public void exitWithResult(int requestCode, Object result) {
        exit();
        sendDelayedResult(requestCode, result);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Protected / Private Methods ////////////////////////////////////////////

    private void addPendingCommand(Command command) {
        mCommandBuffer.offer(command);
        processPendingCommand();
    }

    private void processPendingCommand() {
        // Drop pending runnable.
        mUiHandler.removeCallbacks(mProcessCommandRunnable);
        // Send new runnable.
        mUiHandler.post(mProcessCommandRunnable);
    }

    private Runnable mProcessCommandRunnable = new Runnable() {
        @Override
        public void run() {
            boolean keepGoing = true;
            while (!mCommandBuffer.isEmpty() &&
                   mNavigator != null &&
                   keepGoing) {
                // If it returns true, process next command in the buffer;
                // If it returns false, wait until finish() is called and then process
                // next command in the buffer.
                keepGoing = mNavigator.applyCommand(mCommandBuffer.peek(),
                                                    mFutureResult);
                if (keepGoing) {
                    mCommandBuffer.poll();
                }
            }
        }
    };

    private INavigator.FutureResult mFutureResult = new INavigator.FutureResult() {
        @Override
        public void finish(boolean isHandled) {
            // Discard command if the navigator is done with the it.
            final Command command = mCommandBuffer.poll();

            // If the navigator cannot consume the command, bypass to parent
            // router.
            if (!isHandled && mParent != null) {
                mParent.addPendingCommand(command);
            }

            // Keep processing the pending commands.
            processPendingCommand();
        }
    };

    /**
     * Send a delayed resultHolder until the {@link #dispatchResultOnResume()} is
     * called.
     *
     * @param requestCode resultHolder data key
     * @param result      resultHolder data
     */
    private void sendDelayedResult(int requestCode, Object result) {
        if (mResultsBuffer.containsKey(requestCode)) {
            mResultsBuffer.get(requestCode)
                .resultHolder
                .set(result);
        } else {
            mResultsBuffer.put(requestCode, new ListenerAndResult(result));
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
