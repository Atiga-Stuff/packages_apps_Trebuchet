/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.launcher3.taskbar.bubbles;

import android.annotation.Nullable;
import android.app.Notification;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Outline;
import android.graphics.Path;
import android.graphics.Rect;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.ImageView;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.android.launcher3.R;
import com.android.launcher3.icons.DotRenderer;
import com.android.launcher3.icons.IconNormalizer;
import com.android.wm.shell.animation.Interpolators;
import com.android.wm.shell.common.bubbles.BubbleInfo;

// TODO: (b/276978250) This is will be similar to WMShell's BadgedImageView, it'd be nice to share.

/**
 * View that displays a bubble icon, along with an app badge on either the left or
 * right side of the view.
 */
public class BubbleView extends ConstraintLayout {

    public static final int DEFAULT_PATH_SIZE = 100;

    private final ImageView mBubbleIcon;
    private final ImageView mAppIcon;
    private int mBubbleSize;

    private float mDragTranslationX;
    private float mOffsetX;

    private DotRenderer mDotRenderer;
    private DotRenderer.DrawParams mDrawParams;
    private int mDotColor;
    private Rect mTempBounds = new Rect();

    // Whether the dot is animating
    private boolean mDotIsAnimating;
    // What scale value the dot is animating to
    private float mAnimatingToDotScale;
    // The current scale value of the dot
    private float mDotScale;

    // TODO: (b/273310265) handle RTL
    // Whether the bubbles are positioned on the left or right side of the screen
    private boolean mOnLeft = false;

    private BubbleBarItem mBubble;

    public BubbleView(Context context) {
        this(context, null);
    }

    public BubbleView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BubbleView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public BubbleView(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        // We manage positioning the badge ourselves
        setLayoutDirection(LAYOUT_DIRECTION_LTR);

        LayoutInflater.from(context).inflate(R.layout.bubble_view, this);
        mBubbleIcon = findViewById(R.id.icon_view);
        mAppIcon = findViewById(R.id.app_icon_view);

        mDrawParams = new DotRenderer.DrawParams();

        setFocusable(true);
        setClickable(true);
        setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                BubbleView.this.getOutline(outline);
            }
        });
    }

    private void getOutline(Outline outline) {
        updateBubbleSizeAndDotRender();
        final int normalizedSize = IconNormalizer.getNormalizedCircleSize(mBubbleSize);
        final int inset = (mBubbleSize - normalizedSize) / 2;
        outline.setOval(inset, inset, inset + normalizedSize, inset + normalizedSize);
    }

    private void updateBubbleSizeAndDotRender() {
        int updatedBubbleSize = Math.min(getWidth(), getHeight());
        if (updatedBubbleSize == mBubbleSize) return;
        mBubbleSize = updatedBubbleSize;
        if (mBubble == null || mBubble instanceof BubbleBarOverflow) return;
        Path dotPath = ((BubbleBarBubble) mBubble).getDotPath();
        mDotRenderer = new DotRenderer(mBubbleSize, dotPath, DEFAULT_PATH_SIZE);
    }

    /**
     * Set translation-x while this bubble is being dragged.
     * Translation applied to the view is a sum of {@code translationX} and offset defined by
     * {@link #setOffsetX(float)}.
     */
    public void setDragTranslationX(float translationX) {
        mDragTranslationX = translationX;
        applyDragTranslation();
    }

    /**
     * Get translation value applied via {@link #setDragTranslationX(float)}.
     */
    public float getDragTranslationX() {
        return mDragTranslationX;
    }

    /**
     * Set offset on x-axis while dragging.
     * Used to counter parent translation in order to keep the dragged view at the current position
     * on screen.
     * Translation applied to the view is a sum of {@code offsetX} and translation defined by
     * {@link #setDragTranslationX(float)}
     */
    public void setOffsetX(float offsetX) {
        mOffsetX = offsetX;
        applyDragTranslation();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        updateBubbleSizeAndDotRender();
    }

    private void applyDragTranslation() {
        setTranslationX(mDragTranslationX + mOffsetX);
    }

    @Override
    public void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);

        if (!shouldDrawDot()) {
            return;
        }

        getDrawingRect(mTempBounds);

        mDrawParams.dotColor = mDotColor;
        mDrawParams.iconBounds = mTempBounds;
        mDrawParams.leftAlign = mOnLeft;
        mDrawParams.scale = mDotScale;

        mDotRenderer.draw(canvas, mDrawParams);
    }

    /** Sets the bubble being rendered in this view. */
    public void setBubble(BubbleBarBubble bubble) {
        mBubble = bubble;
        mBubbleIcon.setImageBitmap(bubble.getIcon());
        mAppIcon.setImageBitmap(bubble.getBadge());
        mDotColor = bubble.getDotColor();
        mDotRenderer = new DotRenderer(mBubbleSize, bubble.getDotPath(), DEFAULT_PATH_SIZE);
        String contentDesc = bubble.getInfo().getTitle();
        if (TextUtils.isEmpty(contentDesc)) {
            contentDesc = getResources().getString(R.string.bubble_bar_bubble_fallback_description);
        }
        String appName = bubble.getInfo().getAppName();
        if (!TextUtils.isEmpty(appName)) {
            contentDesc = getResources().getString(R.string.bubble_bar_bubble_description,
                    contentDesc, appName);
        }
        setContentDescription(contentDesc);
    }

    /**
     * Sets that this bubble represents the overflow. The overflow appears in the list of bubbles
     * but does not represent app content, instead it shows recent bubbles that couldn't fit into
     * the list of bubbles. It doesn't show an app icon because it is part of system UI / doesn't
     * come from an app.
     */
    public void setOverflow(BubbleBarOverflow overflow, Bitmap bitmap) {
        mBubble = overflow;
        mBubbleIcon.setImageBitmap(bitmap);
        mAppIcon.setVisibility(GONE); // Overflow doesn't show the app badge
        setContentDescription(getResources().getString(R.string.bubble_bar_overflow_description));
    }

    /** Returns the bubble being rendered in this view. */
    @Nullable
    public BubbleBarItem getBubble() {
        return mBubble;
    }

    void updateDotVisibility(boolean animate) {
        final float targetScale = hasUnseenContent() ? 1f : 0f;
        if (animate) {
            animateDotScale(targetScale);
        } else {
            mDotScale = targetScale;
            mAnimatingToDotScale = targetScale;
            invalidate();
        }
    }

    void updateBadgeVisibility(boolean show) {
        if (mBubble instanceof BubbleBarOverflow) {
            // The overflow bubble does not have a badge, so just bail.
            return;
        }
        BubbleBarBubble bubble = (BubbleBarBubble) mBubble;
        Bitmap appBadgeBitmap = bubble.getBadge();
        int translationX = mOnLeft
                ? -(bubble.getIcon().getWidth() - appBadgeBitmap.getWidth())
                : 0;
        mAppIcon.setTranslationX(translationX);
        mAppIcon.setVisibility(show ? VISIBLE : GONE);
    }

    boolean hasUnseenContent() {
        return mBubble != null
                && mBubble instanceof BubbleBarBubble
                && !((BubbleBarBubble) mBubble).getInfo().isNotificationSuppressed();
    }

    /**
     * Used to determine if we can skip drawing frames.
     *
     * <p>Generally we should draw the dot when it is requested to be shown and there is unseen
     * content. But when the dot is removed, we still want to draw frames so that it can be scaled
     * out.
     */
    private boolean shouldDrawDot() {
        // if there's no dot there's nothing to draw, unless the dot was removed and we're in the
        // middle of removing it
        return hasUnseenContent() || mDotIsAnimating;
    }

    /** Updates the dot scale to the specified fraction from 0 to 1. */
    private void setDotScale(float fraction) {
        if (!shouldDrawDot()) {
            return;
        }
        mDotScale = fraction;
        invalidate();
    }

    void showDotIfNeeded(float fraction) {
        if (!hasUnseenContent()) {
            return;
        }
        setDotScale(fraction);
    }

    void showDotIfNeeded(boolean animate) {
        // only show the dot if we have unseen content
        if (!hasUnseenContent()) {
            return;
        }
        if (animate) {
            animateDotScale(1f);
        } else {
            setDotScale(1f);
        }
    }

    void hideDot() {
        animateDotScale(0f);
    }

    /** Marks this bubble such that it no longer has unseen content, and hides the dot. */
    void markSeen() {
        if (mBubble instanceof BubbleBarBubble bubble) {
            BubbleInfo info = bubble.getInfo();
            info.setFlags(
                    info.getFlags() | Notification.BubbleMetadata.FLAG_SUPPRESS_NOTIFICATION);
            hideDot();
        }
    }

    /** Animates the dot to the given scale. */
    private void animateDotScale(float toScale) {
        boolean isDotScaleChanging = Float.compare(mDotScale, toScale) != 0;

        // Don't restart the animation if we're already animating to the given value or if the dot
        // scale is not changing
        if ((mDotIsAnimating && mAnimatingToDotScale == toScale) || !isDotScaleChanging) {
            return;
        }
        mDotIsAnimating = true;
        mAnimatingToDotScale = toScale;

        final boolean showDot = toScale > 0f;

        clearAnimation();
        animate()
                .setDuration(200)
                .setInterpolator(Interpolators.FAST_OUT_SLOW_IN)
                .setUpdateListener((valueAnimator) -> {
                    float fraction = valueAnimator.getAnimatedFraction();
                    fraction = showDot ? fraction : 1f - fraction;
                    setDotScale(fraction);
                }).withEndAction(() -> {
                    setDotScale(showDot ? 1f : 0f);
                    mDotIsAnimating = false;
                }).start();
    }

    @Override
    public String toString() {
        String toString = mBubble != null ? mBubble.getKey() : "null";
        return "BubbleView{" + toString + "}";
    }
}
