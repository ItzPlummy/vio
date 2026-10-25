package com.plummy.vio.core.tool;

import com.plummy.vio.core.access.AccessLevel;
import com.plummy.vio.core.script.ScriptType;
import com.plummy.vio.core.tool.block.GetBlockInfoTool;
import com.plummy.vio.core.tool.block.GetContainerContentsTool;
import com.plummy.vio.core.tool.editing.*;
import com.plummy.vio.core.tool.entity.FindEntitiesTool;
import com.plummy.vio.core.tool.entity.GetEntityInfoTool;
import com.plummy.vio.core.tool.knowledge.*;
import com.plummy.vio.core.tool.monitoring.GetModListTool;
import com.plummy.vio.core.tool.monitoring.GetServerStatusTool;
import com.plummy.vio.core.tool.monitoring.ReadServerLogTool;
import com.plummy.vio.core.tool.nbt.GetNbtTool;
import com.plummy.vio.core.tool.nbt.ModifyNbtTool;
import com.plummy.vio.core.tool.nbt.NbtTarget;
import com.plummy.vio.core.tool.nbt.RemoveNbtTool;
import com.plummy.vio.core.tool.player.*;
import com.plummy.vio.core.tool.script.*;
import com.plummy.vio.core.tool.web.WebFetchTool;
import com.plummy.vio.core.tool.web.WebSearchTool;
import com.plummy.vio.core.tool.world.*;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class ToolRegistry {

    private static final Map<String, Tool> TOOLS = new LinkedHashMap<>();

    public static ServerTool RUN_COMMAND = register(new RunCommandTool());
    public static ServerTool GET_BLOCK_INFO = register(new GetBlockInfoTool());
    public static ServerTool GET_LOOKED_AT = register(new GetLookedAtTool());
    public static ServerTool FIND_ENTITIES = register(new FindEntitiesTool());
    public static ServerTool GET_INVENTORY = register(new GetInventoryTool());
    public static ServerTool LIST_PLAYERS = register(new ListPlayersTool());
    public static ServerTool SEARCH_REGISTRY = register(new SearchRegistryTool());
    public static ServerTool GET_RECIPES = register(new GetRecipesTool());

    public static ServerTool GET_ADVANCEMENTS = register(new GetAdvancementsTool());
    public static ServerTool GET_ADVANCEMENT_PROGRESS = register(new GetAdvancementProgressTool());
    public static ServerTool GET_PLAYER_STATS = register(new GetPlayerStatsTool());
    public static ServerTool GET_PLAYER_STAT = register(new GetPlayerStatTool());

    public static ServerTool SET_TIME = register(new SetTimeTool());

    public static ServerTool FIND_BLOCKS = register(new FindBlocksTool());
    public static ServerTool COUNT_BLOCKS = register(new CountBlocksTool());
    public static ServerTool GET_REGION_BLOCKS = register(new GetRegionBlocksTool());
    public static ServerTool GET_TERRAIN_MAP = register(new GetTerrainMapTool());
    public static ServerTool FIND_SAFE_POSITION = register(new FindSafePositionTool());
    public static ServerTool GET_SPAWN_INFO = register(new GetSpawnInfoTool());
    public static ServerTool GET_STRUCTURES_AT = register(new GetStructuresAtTool());

    public static ServerTool GET_ENTITY_INFO = register(new GetEntityInfoTool());
    public static ServerTool GET_CONTAINER_CONTENTS = register(new GetContainerContentsTool());

    public static ServerTool GET_REGISTRY_ENTRY = register(new GetRegistryEntryTool());
    public static ServerTool GET_ITEM_INFO = register(new GetItemInfoTool());
    public static ServerTool GET_BLOCK_TYPE_INFO = register(new GetBlockTypeInfoTool());
    public static ServerTool GET_LOOT_TABLE = register(new GetLootTableTool());
    public static ServerTool GET_TAG_MEMBERS = register(new GetTagMembersTool());
    public static ServerTool GET_TAGS_OF = register(new GetTagsOfTool());

    public static ServerTool GET_BLOCK_NBT = register(new GetNbtTool(NbtTarget.Kind.BLOCK));
    public static ServerTool GET_ENTITY_NBT = register(new GetNbtTool(NbtTarget.Kind.ENTITY));
    public static ServerTool GET_STORAGE_NBT = register(new GetNbtTool(NbtTarget.Kind.STORAGE));
    public static ServerTool MODIFY_BLOCK_NBT = register(new ModifyNbtTool(NbtTarget.Kind.BLOCK));
    public static ServerTool MODIFY_ENTITY_NBT = register(new ModifyNbtTool(NbtTarget.Kind.ENTITY));
    public static ServerTool MODIFY_STORAGE_NBT = register(new ModifyNbtTool(NbtTarget.Kind.STORAGE));
    public static ServerTool REMOVE_BLOCK_NBT = register(new RemoveNbtTool(NbtTarget.Kind.BLOCK));
    public static ServerTool REMOVE_ENTITY_NBT = register(new RemoveNbtTool(NbtTarget.Kind.ENTITY));
    public static ServerTool REMOVE_STORAGE_NBT = register(new RemoveNbtTool(NbtTarget.Kind.STORAGE));

    public static ServerTool GIVE_PLAYER_ITEM = register(new GivePlayerItemTool());
    public static ServerTool SET_BLOCK = register(new SetBlockTool());
    public static ServerTool FILL_REGION = register(new FillRegionTool());
    public static ServerTool FILL_LINE = register(new FillLineTool());
    public static ServerTool FILL_ELLIPSOID = register(new FillEllipsoidTool());
    public static ServerTool FILL_CYLINDER = register(new FillCylinderTool());
    public static ServerTool FILL_CONE = register(new FillConeTool());
    public static ServerTool FILL_PYRAMID = register(new FillPyramidTool());
    public static ServerTool CLONE_REGION = register(new CloneRegionTool());
    public static ServerTool RETEXTURE_REGION = register(new RetextureRegionTool());

    public static ServerTool OPEN_MENU = register(new OpenMenuTool());
    public static ServerTool OPEN_CONTAINER = register(new OpenContainerTool());

    public static ServerTool GET_SERVER_STATUS = register(new GetServerStatusTool());
    public static ServerTool READ_SERVER_LOG = register(new ReadServerLogTool());
    public static ServerTool GET_MOD_LIST = register(new GetModListTool());

    public static ServerTool LIST_SCRIPTS = register(new ListScriptsTool());
    public static ServerTool READ_SCRIPT = register(new ReadScriptTool());
    public static BasicTool SAVE_ONE_SHOT_SCRIPT = register(new SaveScriptTool(ScriptType.ONE_SHOT));
    public static BasicTool SAVE_PERIODIC_SCRIPT = register(new SaveScriptTool(ScriptType.PERIODIC));
    public static BasicTool SAVE_LISTENER_SCRIPT = register(new SaveScriptTool(ScriptType.LISTENER));
    public static BasicTool SAVE_COMMAND_SCRIPT = register(new SaveScriptTool(ScriptType.COMMAND));
    public static ServerTool SET_SCRIPT_ENABLED = register(new SetScriptEnabledTool());
    public static ServerTool RUN_SCRIPT = register(new RunScriptTool());
    public static ServerTool READ_SCRIPT_LOG = register(new ReadScriptLogTool());
    public static ServerTool DELETE_SCRIPT = register(new DeleteScriptTool());

    public static BasicTool WEB_SEARCH = register(new WebSearchTool());
    public static BasicTool WEB_FETCH = register(new WebFetchTool());

    private static <T extends Tool> T register(T tool) {
        if (tool.getArgumentSchema().containsKey("status")) {
            throw new IllegalStateException("Tool " + tool.getName() + " can't define the reserved 'status' argument");
        }

        if (TOOLS.putIfAbsent(tool.getName(), tool) != null) {
            throw new IllegalStateException("Duplicate tool name: " + tool.getName());
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
