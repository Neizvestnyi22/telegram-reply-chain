package org.telegram.ui.Components;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.view.View;
import android.view.animation.PathInterpolator;
import android.widget.LinearLayout;

import org.telegram.messenger.AndroidUtilities;

/**
 * Vertical stack of the chain's bubbles joined by a thread line on the left,
 * revealed one by one from the oldest message. Lives inside a BottomSheet.
 */
public class ReplyChainLayout extends LinearLayout {

    private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dot = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float lineProgress;
    private final int lineX;

    public ReplyChainLayout(Context context, int color) {
        super(context);
        setOrientation(VERTICAL);
        setWillNotDraw(false);
        lineX = AndroidUtilities.dp(25);
        line.setStyle(Paint.Style.STROKE);
        line.setStrokeWidth(AndroidUtilities.dp(2));
        line.setStrokeCap(Paint.Cap.ROUND);
        line.setColor(color);
        line.setAlpha(110);
        line.setPathEffect(new DashPathEffect(new float[]{AndroidUtilities.dp(4), AndroidUtilities.dp(4)}, 0));
        dot.setColor(color);
    }

    /** Children appear with a stagger; call after adding all items. */
    public void reveal() {
        int n = getChildCount();
        for (int i = 0; i < n; i++) {
            View v = getChildAt(i);
            v.setAlpha(0);
            v.setTranslationY(AndroidUtilities.dp(18));
            v.animate().alpha(1).translationY(0).setStartDelay(120 + i * 110L).setDuration(260)
                    .setInterpolator(new PathInterpolator(0.2f, 0.9f, 0.3f, 1)).start();
        }
        android.animation.ValueAnimator va = android.animation.ValueAnimator.ofFloat(0, 1);
        va.setStartDelay(120);
        va.setDuration(110L * n + 200);
        va.addUpdateListener(a -> {
            lineProgress = (float) a.getAnimatedValue();
            invalidate();
        });
        va.start();
    }

    private float nodeY(View v) {
        return v.getBottom() - AndroidUtilities.dp(21);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        int n = getChildCount();
        if (n > 1) {
            // The thread runs through the avatars, which sit at the bottom of each bubble.
            float y0 = nodeY(getChildAt(0));
            float y1 = nodeY(getChildAt(n - 1));
            canvas.drawLine(lineX, y0, lineX, y0 + (y1 - y0) * lineProgress, line);
            for (int i = 0; i < n; i++) {
                View v = getChildAt(i);
                if (v.getAlpha() < 1f && nodeY(v) > y0 + (y1 - y0) * lineProgress) {
                    continue;
                }
                dot.setAlpha((int) (90 * v.getAlpha()));
                canvas.drawCircle(lineX, nodeY(v), AndroidUtilities.dp(22), dot);
            }
        }
        super.dispatchDraw(canvas);
    }
}
