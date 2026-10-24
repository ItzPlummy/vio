package com.plummy.vio.section;

import com.plummy.vio.script.Script;
import com.plummy.vio.script.ScriptManager;
import com.plummy.vio.script.ScriptState;
import com.plummy.vio.script.ScriptType;

import java.util.ArrayList;
import java.util.List;

// A summary of every saved script; the source, log and history stay behind the script tools.
public class ScriptContextSection implements Section {

    private final List<Entry> scripts;

    public ScriptContextSection(List<Entry> scripts) {
        this.scripts = List.copyOf(scripts);
    }

    public static ScriptContextSection fromManager(ScriptManager manager) {
        List<Entry> scripts = new ArrayList<>();

        for (Script script : manager.getAll()) {
            scripts.add(new Entry(script.getName(), script.getType(), script.getState(), script.getHeader().getDescription()));
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
