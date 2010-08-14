package org.python.jsr223;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import org.python.Version;
import org.python.core.Py;

public class PyScriptEngineFactory implements ScriptEngineFactory {

    public String getEngineName() {
        return "jython";
    }

    public String getEngineVersion() {
        return String.format("%s.%s.%s", Version.PY_MAJOR_VERSION, Version.PY_MINOR_VERSION,
            Version.PY_MICRO_VERSION);
    }

    public List<String> getExtensions() {
        return Collections.unmodifiableList(Arrays.asList("py"));
    }

    public String getLanguageName() {
        return "python";
    }

    public String getLanguageVersion() {
        return String.format("%s.%s", Version.PY_MAJOR_VERSION, Version.PY_MINOR_VERSION);
    }

    public Object getParameter(String key) {
        if (key.equals(ScriptEngine.ENGINE)) {
            return getEngineName();
        } else if (key.equals(ScriptEngine.ENGINE_VERSION)) {
            return getEngineVersion();
        } else if (key.equals(ScriptEngine.NAME)) {
            return getEngineName();
        } else if (key.equals(ScriptEngine.LANGUAGE)) {
            return getLanguageName();
        } else if (key.equals(ScriptEngine.LANGUAGE_VERSION)) {
            return getLanguageVersion();
        } else if (key.equals("THREADING")) {
            return "MULTITHREADED";
        } else {
            return null;
        }

    }

    public String getMethodCallSyntax(String obj, String m, String... args) {
        StringBuilder buffer = new StringBuilder();
        buffer.append(String.format("%s.%s(", obj, m));
        int i = args.length;
        for (String arg : args) {
            buffer.append(arg);
            if (i-- > 0) {
                buffer.append(", ");
            }
        }
        buffer.append(")");
        return buffer.toString();
    }

    // presumably a unicode string
    public String getOutputStatement(String toDisplay) {
        StringBuilder buffer = new StringBuilder(toDisplay.length() + 8);
        buffer.append("print ");
        buffer.append(Py.newUnicode(toDisplay).__repr__());
        return buffer.toString();
    }

    public String getProgram(String... statements) {
        StringBuilder buffer = new StringBuilder();
        for (String statement : statements) {
            buffer.append(statement);
            buffer.append("\n");
        }
        return buffer.toString();
    }

    public ScriptEngine getScriptEngine() {
        return new PyScriptEngine(this);
    }

    public List<String> getMimeTypes() {
        return Collections.unmodifiableList(Arrays.asList(
                "text/python", "application/python", "text/x-python", "application/x-python"));
    }

    public List<String> getNames() {
        return Collections.unmodifiableList(Arrays.asList("python", "jython"));
    }

}
