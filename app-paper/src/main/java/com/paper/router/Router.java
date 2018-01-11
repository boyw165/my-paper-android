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

import ru.terrakok.cicerone.Navigator;
import ru.terrakok.cicerone.commands.Back;
import ru.terrakok.cicerone.commands.Forward;

public final class Router {

    public static final int LEVEL_APPLICATION = 0;
    public static final int LEVEL_ACTIVITY = 1;
    public static final int LEVEL_VIEW = 2;

    private final InnerRouter[] mRouters;

    public Router(Handler uiHandler) {
        if (uiHandler.getLooper() != Looper.getMainLooper()) {
            throw new IllegalThreadStateException(
                "Given handler is not main thread.");
        }

        // Fixed size of the routers.
        this.mRouters = new InnerRouter[]{
            new InnerRouter("application", uiHandler),
            new InnerRouter("activity", uiHandler),
            new InnerRouter("view", uiHandler)
        };
    }

    /**
     * Set an active Navigator for the Cicerone and start receive commands.
     *
     * @param navigator new active Navigator
     */
    public final void setNavigator(int level,
                                   INavigator navigator) {
        throwExceptionIfNotMainThread();

        mRouters[level].setNavigator(navigator);
    }

    /**
     * Remove the current Navigator and stop receive commands.
     */
    public final void unsetNavigator(int level) {
        throwExceptionIfNotMainThread();

        mRouters[level].unsetNavigator();
    }

    public final void bindContextToNavigator(int level,
                                             Context context) {
        throwExceptionIfNotSupportedLevel(level);

        mRouters[level].bindContextToNavigator(context);
    }

    public final void unBindContextFromNavigator(int level) {
        throwExceptionIfNotSupportedLevel(level);

        mRouters[level].unBindContextFromNavigator();
    }

    /**
     * Open new screen and add it to the screens chain.
     *
     * @param screenKey screen key
     */
    public void navigateTo(String screenKey) {
        throwExceptionIfNotMainThread();

        final int level = getLevelFromScreenKey(screenKey);
        throwExceptionIfNotSupportedLevel(level);

        mRouters[level].navigateTo(screenKey, null);
    }

    /**
     * Open new screen and add it to screens chain.
     *
     * @param screenKey screen key
     * @param data      initialisation parameters for the new screen
     */
    public void navigateTo(String screenKey,
                           Object data) {
        throwExceptionIfNotMainThread();

        final int level = getLevelFromScreenKey(screenKey);
        throwExceptionIfNotSupportedLevel(level);

        mRouters[level].addPendingCommand(new Forward(screenKey, data));
    }

    /**
     * Return to the previous screen in the chain.
     * Behavior in the case when the current screen is the root depends on
     * the processing of the {@link Back} command in a {@link Navigator} implementation.
     */
    public void exit() {
        throwExceptionIfNotMainThread();

        mRouters[LEVEL_ACTIVITY].addPendingCommand(new Back());
    }

    /**
     * Return to the previous screen in the chain and send resultHolder data.
     *
     * @param requestCode resultHolder data key
     * @param result      resultHolder data
     */
    public void exitWithResult(int requestCode, Object result) {
        throwExceptionIfNotMainThread();

        mRouters[LEVEL_ACTIVITY].addPendingCommand(new Back());
        mRouters[LEVEL_ACTIVITY].sendDelayedResult(requestCode, result);
    }

    /**
     * Subscribe to the screen resultHolder.<br>
     * <b>Note:</b> only one listenerHolder can subscribe to a unique requestCode!<br>
     * You must call a <b>removeResultListener()</b> to avoid a memory leak.
     *
     * @param requestCode key for filter results
     * @param listener    resultHolder listenerHolder
     */
    public final void addResultListener(int requestCode,
                                        ru.terrakok.cicerone.result.ResultListener listener) {
        throwExceptionIfNotMainThread();

        mRouters[LEVEL_ACTIVITY].addResultListener(requestCode, listener);
    }

    /**
     * Unsubscribe from the screen resultHolder.
     *
     * @param requestCode key for filter results
     */
    public final void removeResultListener(int requestCode) {
        throwExceptionIfNotMainThread();

        mRouters[LEVEL_ACTIVITY].removeResultListener(requestCode);
    }

    public void dispatchResultOnResume() {
        throwExceptionIfNotMainThread();

        mRouters[LEVEL_ACTIVITY].dispatchResultOnResume();
    }

    ///////////////////////////////////////////////////////////////////////////
    // Protected / Private Methods ////////////////////////////////////////////

    private void throwExceptionIfNotMainThread() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            throw new IllegalThreadStateException("Should be in the Main thread.");
        }
    }

    private void throwExceptionIfNotSupportedLevel(int level) {
        if (level < LEVEL_APPLICATION ||
            level >= LEVEL_VIEW) {
            throw new IllegalStateException("Not supported level.");
        }
    }

    private int getLevelFromScreenKey(String screenKey) {
        int count = 0;
        final char[] a = screenKey.toCharArray();
        for (char c : a) {
            if (c == '/') {
                ++count;
            }
        }
        return count - 1;
    }
}
