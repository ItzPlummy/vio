package com.plummy.vio.network;

import com.plummy.vio.Vio;
import com.plummy.vio.agent.Agent;
import com.plummy.vio.conversation.Conversation;
import com.plummy.vio.message.*;
import com.plummy.vio.network.payload.HistoryPayload;
import com.plummy.vio.section.SectionContentSerializer;
import com.plummy.vio.storage.ConversationStorage;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = Vio.MODID)
public class HistorySender {

    // Keeps the payload well below the 1 MiB packet limit, even for 3-byte characters.
    private static final int MAX_TOTAL_LENGTH = 200_000;

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            send(player);
        }
    }

    public static void send(ServerPlayer player) {
        Agent agent = Vio.getAgent();
        ConversationStorage storage = Vio.getConversationStorage();

        if (agent == null || storage == null || !Networking.hasClient(player)) {
            return;
        }

        List<HistoryPayload.Entry> entries = List.of();

        try {
            Conversation conversation = storage.get(player.getUUID());

            if (conversation != null) {
                entries = toEntries(conversation);
            }
        } catch (RuntimeException e) {
            Vio.getLogger().error("Failed to load the conversation history of {}", player.getUUID(), e);
        }

        Networking.send(player, new HistoryPayload(agent.isBusy(player.getUUID()), entries));
    }

    private static List<HistoryPayload.Entry> toEntries(Conversation conversation) {
        EntryMapper mapper = new EntryMapper();
        List<HistoryPayload.Entry> entries = new ArrayList<>();

        for (Message message : conversation.getMessages()) {
            HistoryPayload.Entry entry = message.accept(mapper);

            if (entry != null) {
                entries.add(entry);
            }
        }

        // Keeps the newest entries that fit both limits.
        int start = entries.size();
        int totalLength = 0;

        while (start > 0 && entries.size() - start < HistoryPayload.MAX_ENTRIES) {
            int length = entries.get(start - 1).getText().length();

            if (totalLength + length > MAX_TOTAL_LENGTH) {
                break;
            }

            totalLength += length;
            start--;
        }

        return entries.subList(start, entries.size());
    }

    private static class EntryMapper implements MessageVisitor<HistoryPayload.Entry> {

        @Override
        public HistoryPayload.Entry visitPlayerRequestMessage(PlayerRequestMessage message) {
            // Only the player's own text: the context sections are for the model.
            return new HistoryPayload.Entry(HistoryPayload.Kind.PLAYER, message.getRequest().getRequest(), message.getTime(), message.isVoice());
        }

        @Override
        public HistoryPayload.Entry visitToolRequestMessage(ToolRequestMessage message) {
            return null;
        }

        @Override
        public HistoryPayload.Entry visitToolResponseMessage(ToolResponseMessage message) {
            return null;
        }

        @Override
        public HistoryPayload.Entry visitAgentResponseMessage(AgentResponseMessage message) {
            return new HistoryPayload.Entry(HistoryPayload.Kind.AGENT, SectionContentSerializer.getInstance().getMessageContent(message), message.getTime(), false);
        }
    }
}
