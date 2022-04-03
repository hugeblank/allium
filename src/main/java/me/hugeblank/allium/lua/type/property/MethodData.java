package me.hugeblank.allium.lua.type.property;

import me.basiqueevangelist.enhancedreflection.api.EClass;
import me.basiqueevangelist.enhancedreflection.api.EMethod;
import me.hugeblank.allium.lua.type.UDFFunctions;
import me.hugeblank.allium.lua.type.UserdataFactory;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.LuaValue;

import java.util.List;

public class MethodData<I> implements PropertyData<I> {
    private final EClass<I> sourceClass;
    public final List<EMethod> methods;
    public final UDFFunctions<I> unboundFunction;

    public MethodData(EClass<I> sourceClass, List<EMethod> methods, String name, boolean isStatic) {
        this.sourceClass = sourceClass;
        this.methods = methods;
        this.unboundFunction = new UDFFunctions<>(sourceClass, methods, name, null, isStatic);
    }

    @Override
    public LuaValue get(String name, LuaState state, I instance, boolean isBound) {
        if (isBound)
            return new UDFFunctions<>(sourceClass, methods, name, instance, false);
        else
            return unboundFunction;
    }
}
