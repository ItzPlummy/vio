package com.plummy.vio.script;

import com.plummy.vio.Vio;
import com.plummy.vio.script.dsl.VioScript;
import groovy.lang.GroovyClassLoader;
import org.codehaus.groovy.runtime.InvokerHelper;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

// A compiled script class in a class loader of its own, so closing it lets the class be unloaded.
public class CompiledScript {

    private final GroovyClassLoader loader;
    private final Class<? extends VioScript> scriptClass;

    public CompiledScript(GroovyClassLoader loader, Class<? extends VioScript> scriptClass) {
        this.loader = loader;
        this.scriptClass = scriptClass;
    }

    public Class<? extends VioScript> getScriptClass() {
        return scriptClass;
    }

    public VioScript newInstance() {
        try {
            return scriptClass.getDeclaredConstructor().newInstance();
        } catch (InvocationTargetException e) {
            // A script's field initializers run here, so their failures count as the script's own.
            if (e.getCause() instanceof RuntimeException cause) {
                throw cause;
            }

            if (e.getCause() instanceof Error cause) {
                throw cause;
            }

            throw new IllegalStateException("Couldn't create an instance of " + scriptClass.getName(), e);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Couldn't create an instance of " + scriptClass.getName(), e);
        }
    }

    public void close() {
        close(loader);
    }

    // Also drops Groovy's cached metaclasses, which would otherwise keep the classes and their loader alive.
    static void close(GroovyClassLoader loader) {
        for (Class<?> loaded : loader.getLoadedClasses()) {
            InvokerHelper.removeClass(loaded);
        }

        loader.clearCache();

        try {
            loader.close();
        } catch (IOException e) {
            Vio.getLogger().warn("Failed to close a script class loader", e);
        }
    }
}
