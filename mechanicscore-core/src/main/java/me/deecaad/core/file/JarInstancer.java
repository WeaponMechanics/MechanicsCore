package me.deecaad.core.file;

import com.cjcrafter.foliascheduler.util.ConstructorInvoker;
import me.deecaad.core.MechanicsCore;
import me.deecaad.core.utils.LogLevel;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;

public class JarInstancer extends JarSearcher {

    public JarInstancer(@NotNull JarFile jar) {
        super(jar);
    }

    public <T> @NotNull List<T> createAllInstances(@NotNull Class<T> clazz, @NotNull ClassLoader classLoader, @NotNull SearchMode filter) {
        List<Class<T>> validClasses = findAllSubclasses(clazz, classLoader, filter);

        List<T> instances = new ArrayList<>();
        for (Class<T> validClass : validClasses) {
            Constructor<T> emptyConstructor;
            try {
                emptyConstructor = validClass.getConstructor();
            } catch (NoSuchMethodException e) {
                MechanicsCore.getInstance().getDebugger().severe(validClass + " is missing a no-arg constructor", new IllegalArgumentException());
                continue;
            }

            T instance = new ConstructorInvoker<>(emptyConstructor).newInstance();
            instances.add(instance);
        }

        if (instances.isEmpty())
            MechanicsCore.getInstance().getDebugger().fine("Search came up empty for " + clazz + "... maybe the jar had none!");
        return instances;
    }
}