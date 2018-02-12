package com.paper.exp.simulation;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

import com.paper.exp.simulation.CollisionSystemContract.SimulationListener;

import org.jetbrains.annotations.NotNull;

import io.reactivex.Observable;

public class CollisionSystemView
    extends View
    implements CollisionSystemContract.View {

    // Rendering.
    private final Paint mParticlePaint = new Paint();

    private SimulationListener mListener;

    public CollisionSystemView(Context context) {
        super(context);
    }

    public CollisionSystemView(Context context,
                               @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CollisionSystemView(Context context,
                               @Nullable AttributeSet attrs,
                               int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mParticlePaint.setStyle(Paint.Style.FILL);
        mParticlePaint.setColor(Color.BLACK);
    }

    @Override
    public void schedulePeriodicRendering(@NotNull SimulationListener listener) {
        mListener = listener;
        postInvalidate();
    }

    @Override
    public void unScheduleAll() {
        mListener = null;
    }

    @NotNull
    @Override
    public Paint getParticlePaint() {
        return mParticlePaint;
    }

    @NotNull
    @Override
    public Observable<Object> onClickBack() {
        return Observable.just((Object) 0);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Protected / Private Methods ////////////////////////////////////////////

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (mListener != null) {
            mListener.onUpdateSimulation(canvas);
            postInvalidate();
        }
    }

    @Override
    public void showToast(@NotNull String text) {
        // DUMMY.
    }

    @Override
    public int getCanvasWidth() {
        return getWidth();
    }

    @Override
    public int getCanvasHeight() {
        return getHeight();
    }
}
