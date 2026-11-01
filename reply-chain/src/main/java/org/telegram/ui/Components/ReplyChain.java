package org.telegram.ui.Components;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Walks reply_to links from a message up to the start of the conversation thread.
 *
 * In Telegram for Android: {@link Source#get(int)} is ChatActivity's messagesDict lookup;
 * ids that are not loaded yet are requested with MessagesController.loadMessages /
 * messages.getMessages and the sheet updates when they arrive ({@link #missing}).
 */
public class ReplyChain {

    public interface Message {
        int getId();

        /** 0 when the message is not a reply. */
        int getReplyToId();
    }

    public interface Source {
        Message get(int id);
    }

    public static final int LIMIT = 30;

    /** Oldest first; the last item is the message the user tapped. */
    public final ArrayList<Message> messages = new ArrayList<>();
    /** First id that could not be resolved locally, or 0. */
    public int missing;

    public static ReplyChain build(Message from, Source source) {
        ReplyChain chain = new ReplyChain();
        Message m = from;
        while (m != null && chain.messages.size() < LIMIT) {
            chain.messages.add(m);
            int parent = m.getReplyToId();
            if (parent == 0) {
                break;
            }
            Message next = source.get(parent);
            if (next == null) {
                chain.missing = parent;
                break;
            }
            m = next;
        }
        Collections.reverse(chain.messages);
        return chain;
    }

    /** How many messages a quote leads back to; the badge on the quote shows it when > 1. */
    public static int depth(Message from, Source source) {
        int d = 0;
        Message m = from;
        while (m != null && m.getReplyToId() != 0 && d < LIMIT) {
            d++;
            m = source.get(m.getReplyToId());
        }
        return d;
    }
}
