package com.plummy.vio.core.conversation;

public interface ConversationVisitor<T> {

    T visit(PlayerConversation conversation);
}
