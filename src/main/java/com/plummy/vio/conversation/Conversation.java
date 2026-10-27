package com.plummy.vio.conversation;

import com.plummy.vio.message.Message;
import com.plummy.vio.storage.Storage;

import java.util.List;

public interface Conversation extends Storage.Item<String> {

    ConversationType getType();

    List<Message> getMessages();

    void addMessage(Message message);

    <T> T accept(ConversationVisitor<T> visitor);
}
