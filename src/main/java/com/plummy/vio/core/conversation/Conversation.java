package com.plummy.vio.core.conversation;

import com.plummy.vio.core.message.Message;
import com.plummy.vio.core.storage.Storage;

import java.util.List;

public interface Conversation extends Storage.Item<String> {

    ConversationType getType();

    List<Message> getMessages();

    void addMessage(Message message);

    <T> T accept(ConversationVisitor<T> visitor);
}
