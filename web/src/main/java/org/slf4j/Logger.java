package org.slf4j;

/**
 * Web shim for the slf4j facade.
 *
 * The real slf4j-api binds its provider through ServiceLoader, ClassLoader
 * resource enumeration and a SecurityManager stack walk — none of which exist
 * under TeaVM. The engine only ever calls info/debug/error, so this maps them
 * onto the browser console instead.
 */
public interface Logger {

    void info(String msg);

    void info(String format, Object... args);

    void debug(String msg);

    void debug(String format, Object... args);

    void error(String msg);

    void error(String format, Object... args);

    void warn(String msg);

    void warn(String format, Object... args);
}
