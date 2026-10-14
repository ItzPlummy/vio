package com.plummy.vio.provider;

import com.plummy.vio.conversation.Conversation;
import com.plummy.vio.message.ConversationMessage;

public interface AgentProvider {

    ConversationMessage request(Conversation conversation);
}
