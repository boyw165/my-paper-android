package com.paper.exp.simulation;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

import com.paper.R;
import com.paper.exp.simulation.CollisionSystemContract.SimulationListener;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import io.reactivex.Observable;

public class CollisionSystemView
    extends View
    implements CollisionSystemContract.View {

    // Rendering.
    private final Paint mParticlePaint = new Paint();
    private final Paint mTextPaint = new Paint();
    private final Paint mBoundPaint = new Paint();
    private final RectF mBoundRect = new RectF();
    private final RectF mOval = new RectF();
    private float mTextSize = 0f;

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

        mTextSize = context.getResources().getDimension(R.dimen.debug_text_size_1);

        mTextPaint.setColor(Color.GREEN);
        mTextPaint.setTextSize(mTextSize);
        mTextPaint.setTextAlign(Paint.Align.LEFT);

        mBoundPaint.setColor(Color.LTGRAY);
        mBoundPaint.setStyle(Paint.Style.FILL);
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
    public Observable<Object> onClickBack() {
        return Observable.just((Object) 0);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Protected / Private Methods ////////////////////////////////////////////

    @Override
    protected void onMeasure(int widthSpec,
                             int heightSpec) {
        final int width = MeasureSpec.getSize(widthSpec);
        final int height = width;

        mBoundRect.set(0, 0, width, height);

        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Boundary.
        canvas.drawRect(mBoundRect, mBoundPaint);

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
    public void drawDebugText(@NotNull Canvas canvas,
                              @NotNull String text) {
        float x = mTextSize;
        float y = mTextSize;
        for (String line : text.split("\n")) {
            canvas.drawText(line, x, y, mTextPaint);
            y += mTextSize;
        }
    }

    @Override
    public void drawParticles(@NotNull Canvas canvas,
                              @NotNull List<Particle> particles) {
        final int canvasWidth = getWidth();
        final int canvasHeight = getHeight();

        for (int i = 0; i < particles.size(); ++i) {
            final Particle particle = particles.get(i);
            final double x = particle.getCenterX();
            final double y = particle.getCenterY();
            final double r = particle.getRadius();

            // Paint first one in red and the rest in black.
            if (i == 0) {
                mParticlePaint.setColor(Color.RED);
            } else {
                mParticlePaint.setColor(Color.BLACK);
            }

            mOval.set((float) ((x - r) * canvasWidth),
                      (float) ((y - r) * canvasHeight),
                      (float) ((x + r) * canvasWidth),
                      (float) ((y + r) * canvasHeight));

            canvas.drawOval(mOval, mParticlePaint);
        }
    }
}
