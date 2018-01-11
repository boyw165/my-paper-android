package com.paper.router;

/**
 * @author Konstantin Tskhovrebov (aka terrakok) on 04.07.17.
 */

public interface IResultListener {

    /**
     * Received result from screen.
     *
     * @param resultData
     */
    void onResult(Object resultData);
}
