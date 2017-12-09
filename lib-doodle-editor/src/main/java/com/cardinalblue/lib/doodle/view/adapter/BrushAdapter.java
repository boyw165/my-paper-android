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

package com.cardinalblue.lib.doodle.view.adapter;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;

import com.bumptech.glide.RequestManager;
import com.cardinalblue.lib.doodle.R;
import com.cardinalblue.lib.doodle.IBrushListener;
import com.cardinalblue.lib.doodle.ISketchBrush;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class BrushAdapter extends RecyclerView.Adapter<BrushAdapter.BrushViewHolder> {

    public static final int PAYLOAD_ITEM_CHECKED = 0;
    public static final int PAYLOAD_ITEM_UNCHECKED = 1;

    // Given...
    private final Context mContext;
    private final LayoutInflater mInflater;
    private final RequestManager mGlide;
    private final float mBorderWidth;
    private final float mSelectedBorderWidth;

    // Selection.
    private ISketchBrush mSelectedOne = null;

    // Animation.
    private final Interpolator mJustSelectedInterpolator = new OvershootInterpolator(5f);
    private final Interpolator mJustUnselectedInterpolator = new LinearInterpolator();

    // Listener.
    private IBrushListener mListener;

    // Data.
    private final List<ISketchBrush> mBrushes = new ArrayList<>();
    private final List<Drawable> mDrawables = new ArrayList<>();

    public BrushAdapter(final Context context,
                        final RequestManager glide,
                        float borderWidth,
                        float selectedBorderWidth) {
        mContext = context;
        mInflater = LayoutInflater.from(context);
        mGlide = glide;

        mBorderWidth = borderWidth;
        mSelectedBorderWidth = selectedBorderWidth;
    }

    @Override
    public BrushViewHolder onCreateViewHolder(ViewGroup parent,
                                              int viewType) {
        return new BrushViewHolder(mInflater.inflate(
            R.layout.view_sketch_brush_item, parent, false));
    }

    @Override
    public void onBindViewHolder(final BrushViewHolder holder,
                                 int position) {
        onBindViewHolder(holder, position, Collections.emptyList());
    }

    @Override
    public void onBindViewHolder(final BrushViewHolder holder,
                                 int position,
                                 List<Object> payloads) {
        final View itemView = holder.itemView;
        final CircleImageView imageView = holder.imageView;
        final ISketchBrush brush = mBrushes.get(position);
        final Drawable drawable = mDrawables.get(position);

        // Click listener.
        itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dispatchOnClickListener(holder.getAdapterPosition());
            }
        });

        // Update the drawable.
        imageView.setImageDrawable(drawable);

        // Handle payload. e.g. the selected highlight, bla bla bla...
        if (payloads.isEmpty()) {
            updateItemView(holder, mSelectedOne == brush, false);
        } else {
            for (Object payload : payloads) {
                updateItemView(holder, payload.equals(PAYLOAD_ITEM_CHECKED), true);
            }
        }
    }

    @Override
    public int getItemCount() {
        return mBrushes.size();
    }

    public void setItems(List<ISketchBrush> brushes) {
        mBrushes.addAll(brushes);

        // Construct color drawable list.
        mDrawables.clear();
        for (ISketchBrush brush : mBrushes) {
            if (brush.isEraser()) {
                mDrawables.add(ContextCompat.getDrawable(
                    mContext, R.drawable.icon_e_d_eraser));
            } else {
                mDrawables.add(new ColorDrawable(
                    brush.getBrushColor()));
            }
        }
    }

    public void selectItem(int position) {
        dispatchOnClickListener(position);
    }

    public void setListener(IBrushListener listener) {
        mListener = listener;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Protected / Private Methods ////////////////////////////////////////////

    private void dispatchOnClickListener(final int position) {
        final ISketchBrush brush = mBrushes.get(position);

        if (mListener != null) {
            mListener.onClickBrush(position, brush);
        }

        // Set new selection.
        notifyItemChanged(position, PAYLOAD_ITEM_CHECKED);
        // Unset old selection.
        if (mSelectedOne != null) {
            int oldPosition = mBrushes.indexOf(mSelectedOne);
            if (oldPosition != position) {
                notifyItemChanged(oldPosition, PAYLOAD_ITEM_UNCHECKED);
            }
        }
        // Remember the selected one.
        mSelectedOne = brush;
    }

    private void updateItemView(BrushViewHolder holder,
                                boolean isSelected,
                                boolean isAnimated) {
        final View itemView = holder.itemView;
        final CircleImageView imageView = holder.imageView;
        final ISketchBrush brush = mBrushes.get(holder.getAdapterPosition());
        final long duration = isAnimated ? 200 : 0;

        if (isSelected) {
            if (brush.isEraser()) {
                imageView.setBorderWidth((int) mBorderWidth);
            } else {
                imageView.setBorderWidth((int) mSelectedBorderWidth);
            }

            ViewCompat.animate(imageView).cancel();
            ViewCompat.animate(imageView)
                      .scaleX(1.3f).scaleY(1.3f)
                      .setInterpolator(mJustSelectedInterpolator)
                      .setDuration(duration)
                      .start();
        } else {
            if (brush.isEraser()) {
                imageView.setBorderWidth(0);
            } else {
                imageView.setBorderWidth((int) mBorderWidth);
            }

            ViewCompat.animate(imageView).cancel();
            ViewCompat.animate(imageView)
                      .scaleX(1f).scaleY(1f)
                      .setInterpolator(mJustUnselectedInterpolator)
                      .setDuration(duration)
                      .start();
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Clazz //////////////////////////////////////////////////////////////////

    static class BrushViewHolder
        extends RecyclerView.ViewHolder {

        final CircleImageView imageView;
        // FIXME: It's not USED.
        final View borderView;

        private BrushViewHolder(View itemView) {
            super(itemView);

            imageView = (CircleImageView) itemView.findViewById(R.id.image);
            borderView = itemView.findViewById(R.id.selected_border);
        }
    }
}
