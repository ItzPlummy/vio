package com.plummy.vio.script;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;

import java.util.Collection;

public interface ScriptManager {

    void load();

    void shutdown();

    void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher);

    void tick();

    Script getScript(ScriptKey key);

    ScriptSource getScriptSource(ScriptKey key);

    ScriptLog getScriptLog(ScriptKey key);

    ScriptState getScriptState(ScriptKey key);

    Collection<Script> getScripts();

    void createScript(Script script, ScriptSource source);

    void updateScriptSource(ScriptSource source);

    void deleteScript(ScriptKey key);

    ScriptOutcome execute(ScriptKey key);

    void enable(ScriptKey key);

    void disable(ScriptKey key);
}
