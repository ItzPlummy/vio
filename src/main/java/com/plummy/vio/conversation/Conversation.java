package com.plummy.vio.conversation;

import com.plummy.vio.message.Message;
import com.plummy.vio.storage.StorageItem;

import java.util.List;

public interface Conversation<K> extends StorageItem<K> {

    List<Message> getMessages();

    void addMessage(Message message);

    <T> T accept(ConversationVisitor<T> visitor);
}
