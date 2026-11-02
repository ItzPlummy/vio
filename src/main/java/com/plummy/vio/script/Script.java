package com.plummy.vio.script;

import com.plummy.vio.script.runtime.RuntimeScript;
import com.plummy.vio.storage.StorageItem;

public sealed interface Script extends StorageItem<ScriptKey> permits Executable, Toggleable, OneShotScript, PeriodicScript, CommandScript, ListenerScript {

    ScriptType getType();

    String getDescription();

    Class<? extends RuntimeScript> getRuntimeBaseClass();

    <T> T accept(ScriptVisitor<T> visitor);
}
