package com.plummy.vio.core.conversation;

public interface ConversationVisitor<T> {

    T visitPlayerConversation(PlayerConversation conversation);
}
