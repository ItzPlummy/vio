package com.plummy.vio.tool;

import com.plummy.vio.access.AccessLevel;
import com.plummy.vio.script.ScriptType;
import com.plummy.vio.tool.catalog.editing.*;
import com.plummy.vio.tool.catalog.entity.FindEntitiesTool;
import com.plummy.vio.tool.catalog.entity.GetEntityInfoTool;
import com.plummy.vio.tool.catalog.nbt.GetNbtTool;
import com.plummy.vio.tool.catalog.nbt.ModifyNbtTool;
import com.plummy.vio.tool.catalog.nbt.NbtTarget;
import com.plummy.vio.tool.catalog.nbt.RemoveNbtTool;
import com.plummy.vio.tool.catalog.player.*;
import com.plummy.vio.tool.catalog.registry.*;
import com.plummy.vio.tool.catalog.script.*;
import com.plummy.vio.tool.catalog.server.*;
import com.plummy.vio.tool.catalog.web.WebFetchTool;
import com.plummy.vio.tool.catalog.web.WebSearchTool;
import com.plummy.vio.tool.catalog.world.*;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class ToolRegistry {

    private static final Map<String, Tool> TOOLS = new LinkedHashMap<>();

    public static final ServerTool RUN_COMMAND = register(new RunCommandTool());
    public static final ServerTool GET_BLOCK_INFO = register(new GetBlockInfoTool());
    public static final ServerTool GET_LOOKED_AT = register(new GetLookedAtTool());
    public static final ServerTool FIND_ENTITIES = register(new FindEntitiesTool());
    public static final ServerTool GET_INVENTORY = register(new GetInventoryTool());
    public static final ServerTool LIST_PLAYERS = register(new ListPlayersTool());
    public static final ServerTool SEARCH_REGISTRY = register(new SearchRegistryTool());
    public static final ServerTool GET_RECIPES = register(new GetRecipesTool());

    public static final ServerTool GET_ADVANCEMENTS = register(new GetAdvancementsTool());
    public static final ServerTool GET_ADVANCEMENT_PROGRESS = register(new GetAdvancementProgressTool());
    public static final ServerTool GET_PLAYER_STATS = register(new GetPlayerStatsTool());
    public static final ServerTool GET_PLAYER_STAT = register(new GetPlayerStatTool());

    public static final ServerTool SET_TIME = register(new SetTimeTool());

    public static final ServerTool FIND_BLOCKS = register(new FindBlocksTool());
    public static final ServerTool COUNT_BLOCKS = register(new CountBlocksTool());
    public static final ServerTool GET_REGION_BLOCKS = register(new GetRegionBlocksTool());
    public static final ServerTool GET_TERRAIN_MAP = register(new GetTerrainMapTool());
    public static final ServerTool FIND_SAFE_POSITION = register(new FindSafePositionTool());
    public static final ServerTool GET_SPAWN_INFO = register(new GetSpawnInfoTool());
    public static final ServerTool GET_STRUCTURES_AT = register(new GetStructuresAtTool());

    public static final ServerTool GET_ENTITY_INFO = register(new GetEntityInfoTool());
    public static final ServerTool GET_CONTAINER_CONTENTS = register(new GetContainerContentsTool());

    public static final ServerTool GET_REGISTRY_ENTRY = register(new GetRegistryEntryTool());
    public static final ServerTool GET_ITEM_INFO = register(new GetItemInfoTool());
    public static final ServerTool GET_BLOCK_TYPE_INFO = register(new GetBlockTypeInfoTool());
    public static final ServerTool GET_LOOT_TABLE = register(new GetLootTableTool());
    public static final ServerTool GET_TAG_MEMBERS = register(new GetTagMembersTool());
    public static final ServerTool GET_TAGS_OF = register(new GetTagsOfTool());

    public static final ServerTool GET_BLOCK_NBT = register(new GetNbtTool(NbtTarget.Kind.BLOCK));
    public static final ServerTool GET_ENTITY_NBT = register(new GetNbtTool(NbtTarget.Kind.ENTITY));
    public static final ServerTool GET_STORAGE_NBT = register(new GetNbtTool(NbtTarget.Kind.STORAGE));
    public static final ServerTool MODIFY_BLOCK_NBT = register(new ModifyNbtTool(NbtTarget.Kind.BLOCK));
    public static final ServerTool MODIFY_ENTITY_NBT = register(new ModifyNbtTool(NbtTarget.Kind.ENTITY));
    public static final ServerTool MODIFY_STORAGE_NBT = register(new ModifyNbtTool(NbtTarget.Kind.STORAGE));
    public static final ServerTool REMOVE_BLOCK_NBT = register(new RemoveNbtTool(NbtTarget.Kind.BLOCK));
    public static final ServerTool REMOVE_ENTITY_NBT = register(new RemoveNbtTool(NbtTarget.Kind.ENTITY));
    public static final ServerTool REMOVE_STORAGE_NBT = register(new RemoveNbtTool(NbtTarget.Kind.STORAGE));

    public static final ServerTool GIVE_PLAYER_ITEM = register(new GivePlayerItemTool());
    public static final ServerTool SET_BLOCK = register(new SetBlockTool());
    public static final ServerTool FILL_REGION = register(new FillRegionTool());
    public static final ServerTool FILL_LINE = register(new FillLineTool());
    public static final ServerTool FILL_ELLIPSOID = register(new FillEllipsoidTool());
    public static final ServerTool FILL_CYLINDER = register(new FillCylinderTool());
    public static final ServerTool FILL_CONE = register(new FillConeTool());
    public static final ServerTool FILL_PYRAMID = register(new FillPyramidTool());
    public static final ServerTool CLONE_REGION = register(new CloneRegionTool());
    public static final ServerTool RETEXTURE_REGION = register(new RetextureRegionTool());

    public static final ServerTool OPEN_MENU = register(new OpenMenuTool());
    public static final ServerTool OPEN_CONTAINER = register(new OpenContainerTool());

    public static final ServerTool GET_SERVER_STATUS = register(new GetServerStatusTool());
    public static final ServerTool READ_SERVER_LOG = register(new ReadServerLogTool());
    public static final ServerTool GET_MOD_LIST = register(new GetModListTool());

    public static final ServerTool LIST_SCRIPTS = register(new ListScriptsTool());
    public static final ServerTool READ_SCRIPT = register(new ReadScriptTool());
    public static final BasicTool CREATE_ONE_SHOT_SCRIPT = register(new CreateScriptTool(ScriptType.ONE_SHOT));
    public static final BasicTool CREATE_PERIODIC_SCRIPT = register(new CreateScriptTool(ScriptType.PERIODIC));
    public static final BasicTool CREATE_LISTENER_SCRIPT = register(new CreateScriptTool(ScriptType.LISTENER));
    public static final BasicTool CREATE_COMMAND_SCRIPT = register(new CreateScriptTool(ScriptType.COMMAND));
    public static final ServerTool UPDATE_SCRIPT_SOURCE = register(new UpdateScriptSourceTool());
    public static final ServerTool SET_SCRIPT_ENABLED = register(new SetScriptEnabledTool());
    public static final ServerTool RUN_SCRIPT = register(new RunScriptTool());
    public static final ServerTool READ_SCRIPT_LOG = register(new ReadScriptLogTool());
    public static final ServerTool DELETE_SCRIPT = register(new DeleteScriptTool());

    public static final BasicTool WEB_SEARCH = register(new WebSearchTool());
    public static final BasicTool WEB_FETCH = register(new WebFetchTool());

    private static <T extends Tool> T register(T tool) {
        if (TOOLS.putIfAbsent(tool.getName(), tool) != null) {
            throw new IllegalStateException("Duplicate tool name: %s".formatted(tool.getName()));
        }

        return tool;
    }

    public static Tool get(String name) {
        return TOOLS.get(name);
    }

    public static Collection<Tool> getAll() {
        return Collections.unmodifiableCollection(TOOLS.values());
    }

    public static Collection<Tool> getAll(AccessLevel accessLevel) {
        return TOOLS.values().stream().filter(tool -> tool.isAvailable() && accessLevel.isAtLeast(tool.getAccessLevel())).collect(Collectors.toList());
    }
}
