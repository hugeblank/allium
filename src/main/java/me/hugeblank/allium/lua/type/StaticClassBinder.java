package me.hugeblank.allium.lua.type;

import me.basiqueevangelist.enhancedreflection.api.EClass;
import me.basiqueevangelist.enhancedreflection.api.EMethod;
import me.basiqueevangelist.enhancedreflection.api.typeuse.EClassUse;
import me.hugeblank.allium.lua.api.JavaLib;
import me.hugeblank.allium.lua.type.annotation.LuaIndex;
import me.hugeblank.allium.lua.type.property.PropertyData;
import me.hugeblank.allium.lua.type.property.PropertyResolver;
import org.squiddev.cobalt.*;
import org.squiddev.cobalt.function.ThreeArgFunction;
import org.squiddev.cobalt.function.TwoArgFunction;
import org.squiddev.cobalt.function.VarArgFunction;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class StaticClassBinder {
    private StaticClassBinder() {

    }

    public static LuaUserdata bindClass(EClass<?> clazz) {
        Map<String, PropertyData<?>> cachedProperties = new HashMap<>();
        LuaTable metatable = new LuaTable();
        EMethod indexImpl = clazz.methods().stream().filter(x -> x.isStatic() && x.hasAnnotation(LuaIndex.class)).findAny().orElse(null);

        metatable.rawset("__index", new TwoArgFunction() {
            @Override
            public LuaValue call(LuaState state, LuaValue arg1, LuaValue arg2) throws LuaError {
                if (indexImpl != null) {
                    var parameters = indexImpl.parameters();
                    try {
                        var jargs = UserdataFactory.toJavaArguments(state, arg2, 1, parameters);

                        if (jargs.length == parameters.size()) {
                            try {
                                var instance = UserdataFactory.toJava(state, arg1, clazz);
                                EClassUse<?> ret = indexImpl.returnTypeUse().upperBound();
                                Object out = indexImpl.invoke(instance, jargs);
                                // If out is null, we can assume the index is nil
                                if (out == null) throw new UserdataFactory.InvalidArgumentException();
                                return UserdataFactory.toLuaValue(out, ret);
                            } catch (IllegalAccessException e) {
                                throw new LuaError(e);
                            } catch (InvocationTargetException e) {
                                var target = e.getTargetException();

                                if (target instanceof LuaError err) {
                                    throw err;
                                } else if (target instanceof IndexOutOfBoundsException) {
                                    // Continue.
                                } else {
                                    throw new LuaError(target);
                                }
                            } catch (UserdataFactory.InvalidArgumentException ignore) {}
                        }
                    } catch (UserdataFactory.InvalidArgumentException | IllegalArgumentException e) {
                        // Continue.
                    }
                }

                if (arg2.isTable()) {
                    LuaTable table = arg2.checkTable();
                    EClass<?>[] typeArgs = new EClass[table.getArrayLength()];

                    for (int i = 0; i < typeArgs.length; i++) {
                        typeArgs[i] = JavaLib.asClass(table.rawget(i + 1));
                    }

                    try {
                        return bindClass(clazz.instantiateWith(List.of(typeArgs)));
                    } catch (IllegalArgumentException e) {
                        throw new LuaError(e);
                    }
                }

                String name = arg2.checkString(); // mapped name

                if (name.equals("allium_java_class")) {
                    return UserdataFactory.of(EClass.fromJava(EClass.class)).create(clazz);
                }

                PropertyData<?> cachedProperty = cachedProperties.get(name);

                if (cachedProperty == null) {
                    cachedProperty = PropertyResolver.resolveProperty(clazz, name, true);

                    cachedProperties.put(name, cachedProperty);
                }

                return cachedProperty.get(name, state, null, false);
            }
        });

        metatable.rawset("__newindex", new ThreeArgFunction() {
            @Override
            public LuaValue call(LuaState state, LuaValue arg1, LuaValue arg2, LuaValue arg3) throws LuaError {
                String name = arg2.checkString(); // mapped name

                PropertyData<?> cachedProperty = cachedProperties.get(name);

                if (cachedProperty == null) {
                    cachedProperty = PropertyResolver.resolveProperty(clazz, name, false);

                    cachedProperties.put(name, cachedProperty);
                }

                cachedProperty.set(name, state, null, arg3);

                return Constants.NIL;
            }
        });

        metatable.rawset("__call", new VarArgFunction() {
            @Override
            public Varargs invoke(LuaState state, Varargs args) throws LuaError {
                return createInstance(
                    clazz,
                    state,
                    args.subargs(2)
                );
            }
        });

        return new LuaUserdata(clazz, metatable);
    }

    private static Varargs createInstance(EClass<?> clazz, LuaState state, Varargs args) throws LuaError {
        List<String> paramList = new ArrayList<>();
        for (var constructor : clazz.constructors()) {
            if (AnnotationUtils.isHiddenFromLua(constructor)) continue;

            var parameters = constructor.parameters();
            try {
                var jargs = UserdataFactory.toJavaArguments(state, args, 1, parameters);

                try { // Get the return type, invoke method, cast returned value, cry.
                    EClassUse<?> ret = (EClassUse<?>) constructor.receiverTypeUse();

                    if (ret == null) ret = clazz.asEmptyUse();

                    Object out = constructor.invoke(jargs);
                    return UserdataFactory.toLuaValue(out, ret);
                } catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
                    throw new LuaError(e);
                }
            } catch (UserdataFactory.InvalidArgumentException e) {
                paramList.add(UserdataFactory.paramsToPrettyString(parameters));
            }
        }

        StringBuilder error = new StringBuilder("Could not find parameter match for called constructor " +
            clazz.name() +
            "\nThe following are correct argument types:\n"
        );

        for (String headers : paramList) {
            error.append(headers).append("\n");
        }

        throw new LuaError(error.toString());
    }
}
