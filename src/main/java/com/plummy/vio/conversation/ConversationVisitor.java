package com.plummy.vio.conversation;

public interface ConversationVisitor<T> {

    T visitPlayerConversation(PlayerConversation conversation);
}
