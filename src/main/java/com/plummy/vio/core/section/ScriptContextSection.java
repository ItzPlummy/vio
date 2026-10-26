package com.plummy.vio.core.section;

import com.plummy.vio.core.script.Script;
import com.plummy.vio.core.script.ScriptManager;
import com.plummy.vio.core.script.ScriptState;
import com.plummy.vio.core.script.ScriptType;

import java.util.ArrayList;
import java.util.List;

public class ScriptContextSection implements Section {

    private final List<Entry> scripts;

    public ScriptContextSection(List<Entry> scripts) {
        this.scripts = List.copyOf(scripts);
    }

    public static ScriptContextSection fromManager(ScriptManager manager) {
        List<Entry> scripts = new ArrayList<>();

        for (Script script : manager.getAll()) {
            scripts.add(new Entry(script.getName(), script.getType(), script.getState(), script.getDescription()));
        }

        return new ScriptContextSection(scripts);
    }

    @Override
    public SectionType getType() {
        return SectionType.SCRIPT_CONTEXT;
    }

    @Override
    public <T> T accept(SectionVisitor<T> visitor) {
        return visitor.visitScriptContextSection(this);
    }

    public List<Entry> getScripts() {
        return scripts;
    }

    public record Entry(String name, ScriptType type, ScriptState state, String description) {
    }
}
