package com.plummy.vio.core.script;

import com.plummy.vio.Vio;
import com.plummy.vio.core.script.dsl.CommandCompiledScript;
import com.plummy.vio.core.script.dsl.ListenerCompiledScript;
import com.plummy.vio.core.script.dsl.CompiledScript;
import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyCodeSource;
import groovy.transform.CompileStatic;
import groovy.transform.ConditionalInterrupt;
import org.codehaus.groovy.ast.VariableScope;
import org.codehaus.groovy.ast.expr.ClosureExpression;
import org.codehaus.groovy.ast.tools.GeneralUtils;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.MultipleCompilationErrorsException;
import org.codehaus.groovy.control.customizers.ASTTransformationCustomizer;
import org.codehaus.groovy.control.messages.ExceptionMessage;
import org.codehaus.groovy.control.messages.Message;
import org.codehaus.groovy.control.messages.SyntaxErrorMessage;
import org.codehaus.groovy.runtime.InvokerHelper;
import org.codehaus.groovy.syntax.SyntaxException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ScriptCompiler {

    private ScriptCompiler() {
    }

    public static GroovyClassLoader createLoader(ScriptType type) {
        ClosureExpression condition = GeneralUtils.closureX(GeneralUtils.stmt(GeneralUtils.callX(GeneralUtils.classX(ScriptManager.class), "isBudgetExceeded")));
        condition.setVariableScope(new VariableScope());

        Map<String, Object> interrupt = new HashMap<>();
        interrupt.put("value", condition);
        interrupt.put("thrown", ScriptTimeoutError.class);

        Class<? extends CompiledScript> baseClass = switch (type) {
            case ONE_SHOT, PERIODIC -> CompiledScript.class;
            case LISTENER -> ListenerCompiledScript.class;
            case COMMAND -> CommandCompiledScript.class;
        };
        CompilerConfiguration configuration = new CompilerConfiguration();

        configuration.setScriptBaseClass(baseClass.getName());
        configuration.addCompilationCustomizers(
                new ASTTransformationCustomizer(interrupt, ConditionalInterrupt.class),
                new ASTTransformationCustomizer(CompileStatic.class)
        );

        return new GroovyClassLoader(ScriptCompiler.class.getClassLoader(), configuration);
    }

    public static Class<? extends CompiledScript> compile(GroovyClassLoader loader, String name, String source) {
        Class<?> compiled;

        try {
            compiled = loader.parseClass(new GroovyCodeSource(source, getFileName(name), "/groovy/script"));
        } catch (MultipleCompilationErrorsException e) {
            List<String> errors = new ArrayList<>();

            for (Message message : e.getErrorCollector().getErrors()) {
                if (message instanceof SyntaxErrorMessage syntaxError) {
                    SyntaxException cause = syntaxError.getCause();
                    errors.add("line %s, column %s: %s".formatted(cause.getLine(), cause.getStartColumn(), cause.getOriginalMessage().strip()));
                } else if (message instanceof ExceptionMessage exceptionMessage) {
                    errors.add(String.valueOf(exceptionMessage.getCause()));
                } else {
                    errors.add(message.toString());
                }
            }

            throw new ScriptException("Couldn't compile the script:\n%s".formatted(String.join("\n", errors)), e);
        } catch (RuntimeException | LinkageError e) {
            throw new ScriptException("Couldn't compile the script: %s".formatted(e), e);
        }

        if (!CompiledScript.class.isAssignableFrom(compiled)) {
            throw new ScriptException("The source must be a script with statements to run, not only class declarations");
        }

        return compiled.asSubclass(CompiledScript.class);
    }

    public static void dispose(GroovyClassLoader loader) {
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

    public static String getFileName(String name) {
        return "script_%s.groovy".formatted(name);
    }
}
