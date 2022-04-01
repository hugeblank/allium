package me.hugeblank.allium.lua.api;

import me.basiqueevangelist.enhancedreflection.api.EClass;
import me.hugeblank.allium.loader.Script;
import me.hugeblank.allium.lua.api.commands.CommandRegisterEntry;
import me.hugeblank.allium.lua.type.CoerceToNative;
import me.hugeblank.allium.lua.type.LuaWrapped;
import me.hugeblank.allium.lua.type.UserdataFactory;
import org.jetbrains.annotations.Nullable;
import org.squiddev.cobalt.LuaTable;

import java.util.ArrayList;
import java.util.List;

@LuaWrapped(name = "allium")
public class AlliumLib implements WrappedLuaLibrary {
    public static final List<CommandRegisterEntry>
            COMMANDS = new ArrayList<>();


    public AlliumLib() {
    }

    @LuaWrapped
    public boolean isScriptLoaded(String id) {
        return Script.getFromID(id) != null;
    }

    @LuaWrapped
    public @CoerceToNative List<Script> getAllScripts() {
        return Script.getAllScripts().stream().toList();
    }

    @LuaWrapped
    public @Nullable Script getScript(String id) {
        return Script.getFromID(id);
    }
}
