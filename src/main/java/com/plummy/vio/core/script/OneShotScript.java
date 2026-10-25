package com.plummy.vio.core.script;

public class OneShotScript extends Script implements Executable {

    public OneShotScript(ScriptFile file, CompiledScript compiled, String error) {
        super(file, compiled, error);
    }
}
