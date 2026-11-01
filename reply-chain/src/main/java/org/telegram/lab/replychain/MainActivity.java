package org.telegram.lab.replychain;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;
import android.util.SparseArray;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.lab.LabActivity;
import org.telegram.lab.R;
import org.telegram.lab.Tg;
import org.telegram.lab.TouchDriver;
import org.telegram.lab.ui.AvatarDrawable;
import org.telegram.lab.ui.ChatScreen;
import org.telegram.lab.ui.MessageCell;
import org.telegram.lab.ui.SheetView;
import org.telegram.lab.ui.WallpaperDrawable;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.Components.ReplyChain;
import org.telegram.ui.Components.ReplyChainLayout;

public class MainActivity extends LabActivity {

    private class Msg implements ReplyChain.Message {
        final int id, replyTo;
        final String sender, text, time;
        ChainCell cell;

        Msg(int id, String sender, String text, int replyTo, String time) {
            this.id = id;
            this.sender = sender;
            this.text = text;
            this.replyTo = replyTo;
            this.time = time;
        }

        boolean out() {
            return sender == null;
        }

        @Override
        public int getId() {
            return id;
        }

        @Override
        public int getReplyToId() {
            return replyTo;
        }
    }

    private ChatScreen chat;
    private final SparseArray<Msg> messages = new SparseArray<>();
    private SheetView sheet;
    private ReplyChainLayout chainLayout;
    private Msg last;

    @Override
    protected void onCreateScreen(FrameLayout content) {
        chat = new ChatScreen(this, WallpaperDrawable.BLUE);
        content.addView(chat);
        chat.header.setTitle("Weekend Trip 🏕").setSubtitle("5 members, 3 online", false).setAvatar("Weekend Trip");
        chat.header.addAction(R.drawable.tg_ic_call);
        chat.header.addAction(R.drawable.tg_ic_ab_other);
        chat.input.gift.setVisibility(android.view.View.GONE);

        chat.addDate("Today");
        add(1, "Anna", "Ok team, where do we stay for the weekend trip?", 0, "10:02");
        add(2, "Ben", "The hotel in town is 120 a night 😬", 0, "10:03");
        add(3, "Chloe", "What about the cabin by the lake? 🏡", 1, "10:05");
        add(4, "Ben", "Who's driving?", 0, "10:06");
        add(5, null, "I can drive, my car fits five", 4, "10:07");
        add(6, "Dan", "Is the cabin free on the 14th?", 3, "10:11");
        add(7, "Anna", "Bring board games! 🎲", 0, "10:12");
        add(8, "Chloe", "Just checked: free, 2 nights for 180 in total", 6, "10:20");
        add(9, "Ben", "🔥🔥🔥", 0, "10:21");
        last = add(10, "Dan", "Book it before someone else does!", 8, "10:24");
        chat.scrollToBottom();
    }

    private int color(String sender) {
        return sender == null ? Tg.replyOut : Tg.nameColors[Math.abs(sender.hashCode()) % Tg.nameColors.length];
    }

    private Msg add(int id, String sender, String text, int replyTo, String time) {
        Msg m = new Msg(id, sender, text, replyTo, time);
        messages.put(id, m);
        m.cell = cell(m, true);
        chat.add(m.cell);
        return m;
    }

    private ChainCell cell(Msg m, boolean inChat) {
        ChainCell c = new ChainCell(m);
        c.setText(m.text).setTime(m.time);
        if (m.out()) {
            c.setStatus(MessageCell.STATUS_READ);
        } else {
            c.setGroup(true, m.sender);
            c.setSender(m.sender, color(m.sender));
        }
        if (m.replyTo != 0) {
            Msg p = messages.get(m.replyTo);
            c.setReply(p.out() ? "You" : p.sender, p.text, color(p.sender));
            if (inChat) {
                c.depth = ReplyChain.depth(m, messages::get);
                c.replyExtraWidth = c.depth > 1 ? AndroidUtilities.dp(52) : 0;
                c.setOnReplyClickListener(v -> openChain(m));
            }
        }
        c.setSpacing(3, 3);
        return c;
    }

    private void openChain(Msg from) {
        ReplyChain chain = ReplyChain.build(from, messages::get);
        sheet = new SheetView(this);
        sheet.header("Reply thread", chain.messages.size() + " messages · tap one to see it in the chat");

        chainLayout = new ReplyChainLayout(this, Tg.accent);
        chainLayout.setPadding(0, AndroidUtilities.dp(4), 0, AndroidUtilities.dp(8));
        chainLayout.setBackgroundColor(0xFFF2F4F8);
        for (ReplyChain.Message rm : chain.messages) {
            Msg m = (Msg) rm;
            ChainCell c = cell(m, false);
            c.setReply(null, null, 0);
            c.setTail(true);
            if (m == from) {
                c.current = true;
            }
            c.setOnClickListener(v -> jump(m));
            chainLayout.addView(c, new LinearLayout.LayoutParams(-1, -2));
        }
        sheet.panel.addView(chainLayout, new LinearLayout.LayoutParams(-1, -2));
        TextView show = Tg.text(this, 16, Tg.accent, true);
        show.setText("Show original message in chat");
        show.setGravity(Gravity.CENTER);
        show.setPadding(0, AndroidUtilities.dp(14), 0, AndroidUtilities.dp(6));
        show.setOnClickListener(v -> jump(messages.get(from.replyTo)));
        sheet.panel.addView(show);
        sheet.show(chat.overlay);
        chainLayout.post(chainLayout::reveal);
    }

    private void jump(Msg m) {
        sheet.dismiss();
        chat.postDelayed(() -> chat.scrollToView(m.cell, true), 250);
    }

    /** MessageCell with the "thread depth" badge on its quote. */
    private class ChainCell extends MessageCell {
        int depth;
        boolean current;
        final Msg msg;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final TextPaint text = Tg.paint(12.5f, 0, true);
        private final Drawable icon = Tg.icon(MainActivity.this, R.drawable.tg_msg_viewreplies, Tg.accent);

        ChainCell(Msg m) {
            super(MainActivity.this, m.out());
            msg = m;
        }

        @Override
        protected void onDrawReplyExtra(Canvas canvas, RectF r) {
            if (depth < 2) {
                return;
            }
            int c = Tg.accent;
            String s = String.valueOf(depth + 1);
            float w = text.measureText(s) + AndroidUtilities.dp(30);
            float h = AndroidUtilities.dp(22);
            float right = r.right - AndroidUtilities.dp(6), top = r.top + AndroidUtilities.dp(9);
            paint.setColor(Tg.alpha(c, 0.14f));
            canvas.drawRoundRect(right - w, top, right, top + h, h / 2, h / 2, paint);
            int is = AndroidUtilities.dp(15);
            icon.setBounds((int) (right - w + AndroidUtilities.dp(6)), (int) (top + (h - is) / 2), (int) (right - w + AndroidUtilities.dp(6) + is), (int) (top + (h + is) / 2));
            icon.draw(canvas);
            text.setColor(c);
            canvas.drawText(s, right - AndroidUtilities.dp(8) - text.measureText(s), top + AndroidUtilities.dp(15.5f), text);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            if (current) {
                paint.setColor(Tg.alpha(Tg.accent, 0.08f));
                canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
            }
            super.onDraw(canvas);
        }
    }

    @Override
    protected void onAutoplay(TouchDriver d) {
        d.wait(900)
                .zoom(1.6f, () -> last.cell, 900)
                .wait(900)
                .tapPoint(() -> {
                    float[] o = d.point(last.cell, 0, 0);
                    return new float[]{o[0] + last.cell.replyRect.centerX(), o[1] + last.cell.replyRect.centerY()};
                })
                .wait(400)
                .resetZoom(700)
                .wait(1800)
                .zoom(1.3f, () -> chainLayout, 800)
                .wait(2200)
                .tap(() -> chainLayout.getChildAt(1))
                .wait(400)
                .resetZoom(800)
                .wait(2200)
                .zoom(1.5f, () -> messages.get(3).cell, 800)
                .wait(1500)
                .resetZoom(800)
                .wait(1000);
    }
}
