package com.plummy.vio.script;

import com.plummy.vio.script.dsl.CommandVioScript;
import com.plummy.vio.script.dsl.ListenerVioScript;
import com.plummy.vio.script.dsl.VioScript;
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
import org.codehaus.groovy.syntax.SyntaxException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Compiles statically, so wrong names and types fail here instead of at run time, and with a budget check in every
// loop, method and closure. Line numbers in errors are those of the source, without the header.
public final class ScriptCompiler {

    private ScriptCompiler() {
    }

    public static CompiledScript compile(String name, ScriptType type, String source) {
        GroovyClassLoader loader = new GroovyClassLoader(ScriptCompiler.class.getClassLoader(), createConfiguration(type));
        Class<?> compiled;

        try {
            compiled = loader.parseClass(new GroovyCodeSource(source, getFileName(name), "/groovy/script"));
        } catch (MultipleCompilationErrorsException e) {
            CompiledScript.close(loader);
            throw new ScriptException("Compilation failed:\n" + String.join("\n", describe(e)), e);
        } catch (RuntimeException | LinkageError e) {
            CompiledScript.close(loader);
            throw new ScriptException("Compilation failed: " + e, e);
        }

        // A source with only class declarations compiles to those classes instead of a script.
        if (!VioScript.class.isAssignableFrom(compiled)) {
            CompiledScript.close(loader);
            throw new ScriptException("The source must be a script with statements to run, not only class declarations.");
        }

        return new CompiledScript(loader, compiled.asSubclass(VioScript.class));
    }

    // What stack traces of the script's code name as their file. The prefix keeps the class name, derived from it,
    // valid even for script names like "int".
    public static String getFileName(String name) {
        return "script_" + name + ".groovy";
    }

    private static CompilerConfiguration createConfiguration(ScriptType type) {
        ClosureExpression condition = GeneralUtils.closureX(GeneralUtils.stmt(GeneralUtils.callX(GeneralUtils.classX(ScriptBudget.class), "isExceeded")));
        condition.setVariableScope(new VariableScope());

        Map<String, Object> interrupt = new HashMap<>();
        interrupt.put("value", condition);
        interrupt.put("thrown", ScriptTimeoutError.class);

        CompilerConfiguration configuration = new CompilerConfiguration();

        configuration.setScriptBaseClass(getBaseClass(type).getName());
        configuration.addCompilationCustomizers(
                new ASTTransformationCustomizer(interrupt, ConditionalInterrupt.class),
                new ASTTransformationCustomizer(CompileStatic.class)
        );

        return configuration;
    }

    private static Class<? extends VioScript> getBaseClass(ScriptType type) {
        return switch (type) {
            case ONE_SHOT, PERIODIC -> VioScript.class;
            case LISTENER -> ListenerVioScript.class;
            case COMMAND -> CommandVioScript.class;
        };
    }

    // Like "line 2, column 1: [Static type checking] - Cannot find matching method ServerPlayer#flyy()".
    private static List<String> describe(MultipleCompilationErrorsException exception) {
        List<String> errors = new ArrayList<>();

        for (Message message : exception.getErrorCollector().getErrors()) {
            if (message instanceof SyntaxErrorMessage syntaxError) {
                SyntaxException cause = syntaxError.getCause();
                errors.add("line %d, column %d: %s".formatted(cause.getLine(), cause.getStartColumn(), cause.getOriginalMessage().strip()));
            } else if (message instanceof ExceptionMessage exceptionMessage) {
                errors.add(String.valueOf(exceptionMessage.getCause()));
            } else {
                errors.add(message.toString());
            }
        }

        return errors;
    }
}
