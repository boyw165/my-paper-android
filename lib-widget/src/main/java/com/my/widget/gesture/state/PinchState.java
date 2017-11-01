//  Copyright Oct 2017-present CardinalBlue
//
//  Author: boy@cardinalblue.com
//          jack.huang@cardinalblue.com
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//  http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package com.my.widget.gesture.state;

import android.graphics.PointF;
import android.os.Message;
import android.util.SparseArray;
import android.view.MotionEvent;

import com.my.widget.gesture.IGestureStateOwner;

import static com.my.widget.gesture.IGestureStateOwner.State.STATE_SINGLE_FINGER_PRESSING;

public class PinchState extends BaseGestureState {

    // Pointers.
    private final SparseArray<PointF> mStartPointers = new SparseArray<>();
    private final SparseArray<PointF> mStopPointers = new SparseArray<>();

    public PinchState(IGestureStateOwner owner) {
        super(owner);
    }

    @Override
    public void onEnter(MotionEvent event,
                        Object touchingObject,
                        Object touchingContext) {
        // Hold the first two pointers.
        mStartPointers.clear();
        mStopPointers.clear();
        for (int i = 0; i < 2; ++i) {
            final int id = event.getPointerId(i);

            mStartPointers.put(id, new PointF(event.getX(i),
                                              event.getY(i)));
            mStopPointers.put(id, new PointF(event.getX(i),
                                             event.getY(i)));
        }

        // Dispatch pinch-begin.
        mOwner.getListener().onPinchBegin(
            obtainMyMotionEvent(event), touchingObject, touchingContext,
            new PointF[]{mStartPointers.get(mStartPointers.keyAt(0)),
                         mStartPointers.get(mStartPointers.keyAt(1))});
    }

    @Override
    public void onDoing(MotionEvent event,
                        Object touchingObject,
                        Object touchingContext) {
        final int action = event.getActionMasked();

        switch (action) {
            case MotionEvent.ACTION_MOVE: {
                // Update stop pointers.
                final PointF pointer1 = mStopPointers.get(event.getPointerId(0));
                pointer1.set(event.getX(0), event.getY(0));

                final PointF pointer2 = mStopPointers.get(event.getPointerId(1));
                pointer2.set(event.getX(1), event.getY(1));

                // Dispatch callback.
                mOwner.getListener().onPinch(
                    obtainMyMotionEvent(event), touchingObject, touchingContext,
                    new PointF[]{mStartPointers.get(mStartPointers.keyAt(0)),
                                 mStartPointers.get(mStartPointers.keyAt(1))},
                    new PointF[]{mStopPointers.get(mStopPointers.keyAt(0)),
                                 mStopPointers.get(mStopPointers.keyAt(1))});
                break;
            }

            case MotionEvent.ACTION_POINTER_UP: {
                final int fingers = event.getPointerCount() - 1;

                if (fingers >= 2) {
                    // 0, 1, 2
                    final int upIndex = event.getActionIndex();
                    final int upId = event.getPointerId(upIndex);

                    if (mStartPointers.get(upId) != null) {
                        // TODO: Refresh the start pointers.
                        mOwner.getListener().onPinchEnd(
                            obtainMyMotionEvent(event), touchingObject, touchingContext,
                            new PointF[]{mStartPointers.get(mStartPointers.keyAt(0)),
                                         mStartPointers.get(mStartPointers.keyAt(1))},
                            new PointF[]{mStopPointers.get(mStopPointers.keyAt(0)),
                                         mStopPointers.get(mStopPointers.keyAt(1))});

                        mStartPointers.clear();
                        mStopPointers.clear();

                        int size = 0;
                        for (int i = 0; i < event.getPointerCount(); ++i) {
                            if (i == upIndex) continue;

                            mStartPointers.put(event.getPointerId(i),
                                               new PointF(event.getX(i),
                                                          event.getY(i)));
                            mStopPointers.put(event.getPointerId(i),
                                              new PointF(event.getX(i),
                                                         event.getY(i)));

                            if (++size == 2) break;
                        }

                        mOwner.getListener().onPinchBegin(
                            obtainMyMotionEvent(event), touchingObject, touchingContext,
                            new PointF[]{mStartPointers.get(mStartPointers.keyAt(0)),
                                         mStartPointers.get(mStartPointers.keyAt(1))});
                    }
                } else {
                    // Transit to STATE_SINGLE_FINGER_PRESSING state.
                    mOwner.issueStateTransition(
                        STATE_SINGLE_FINGER_PRESSING,
                        event, touchingObject, touchingContext);
                }
                break;
            }

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                // Transit to STATE_SINGLE_FINGER_PRESSING state.
                mOwner.issueStateTransition(
                    STATE_SINGLE_FINGER_PRESSING,
                    event, touchingObject, touchingContext);
                break;
            }
        }
    }

    @Override
    public void onExit(MotionEvent event,
                       Object touchingObject,
                       Object touchingContext) {
        // Dispatch pinch-end.
        mOwner.getListener().onPinchEnd(
            obtainMyMotionEvent(event), touchingObject, touchingContext,
            new PointF[]{mStartPointers.get(mStartPointers.keyAt(0)),
                         mStartPointers.get(mStartPointers.keyAt(1))},
            new PointF[]{mStopPointers.get(mStopPointers.keyAt(0)),
                         mStopPointers.get(mStopPointers.keyAt(1))});

        // Clear pointers.
        mStartPointers.clear();
        mStopPointers.clear();
    }

    @Override
    public boolean onHandleMessage(Message msg) {
        return false;
    }
}
