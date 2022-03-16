package me.hugeblank.allium.lua.api;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import me.hugeblank.allium.Allium;
import me.hugeblank.allium.loader.Plugin;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import org.squiddev.cobalt.*;
import org.squiddev.cobalt.function.VarArgFunction;
import org.squiddev.cobalt.lib.LuaLibrary;

// Functionally similar to ComputerCraft's commands API
// See: https://github.com/cc-tweaked/CC-Tweaked/blob/mc-1.16.x/src/main/java/dan200/computercraft/shared/computer/apis/CommandAPI.java
public class CommandLib{

    private static boolean isServerNull(Plugin plugin) {
        if (Allium.SERVER == null) {
            plugin.getLogger().error("Cannot execute command: server is not loaded!");
            return true;
        }
        return false;
    }

    public static LuaLibrary create(Plugin plugin) {
        LuaTable mt = new LuaTable();
        mt.rawset("__index", new VarArgFunction() {
            @Override
            public Varargs invoke(LuaState state, Varargs args) throws LuaError {
                if (isServerNull(plugin)) return Constants.NIL;
                String command = args.arg(2).checkString();
                CommandManager manager = Allium.SERVER.getCommandManager();
                CommandDispatcher<ServerCommandSource> dispatcher = manager.getDispatcher();
                for(CommandNode<?> child : dispatcher.getRoot().getChildren()) {
                    if (child.getName().equals(command)) {
                        return new ExecuteCommandFunction(plugin, command);
                    }
                }
                return Constants.NIL;
            }
        });

        return LibBuilder.create("commands")
                // Both methods get the command run, one is just a bit more generic.
                .set("exec", new ExecuteCommandFunction(plugin)::invoke) // commands.exec(String... command)
                .addMetatable(mt) // commands.command(String... arguments)
                .build();
    }

    private static final class ExecuteCommandFunction extends VarArgFunction {
        private String command = "";
        private final Plugin plugin;
        public ExecuteCommandFunction(Plugin plugin, String command) {
            this.command = command + " ";
            this.plugin = plugin;
        }

        public ExecuteCommandFunction(Plugin plugin) {
            this.plugin = plugin;
        }

        private static String mergeStrings(Varargs args) throws LuaError {
            StringBuilder str = new StringBuilder();
            for(int i = 1; i <= args.count(); i++) {
                str.append(args.arg(i).checkString());
                if (i < args.count()) {
                    str.append(" ");
                }
            }
            return str.toString();
        }

        @Override
        public Varargs invoke(LuaState state, Varargs args) throws LuaError {
            if (isServerNull(plugin)) return Constants.NIL;
            CommandManager manager = Allium.SERVER.getCommandManager();
            ServerCommandSource source = Allium.SERVER.getCommandSource();
            return (manager.execute(source, command + mergeStrings(args))) == 0 ?
                    Constants.FALSE :
                    Constants.TRUE;
        }
    }
}