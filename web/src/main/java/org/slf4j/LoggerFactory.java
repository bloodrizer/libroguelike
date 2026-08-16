package org.slf4j;

/** Web shim: hands out console-backed loggers, no provider binding involved. */
public final class LoggerFactory {

    private LoggerFactory() {
    }

    public static Logger getLogger(String name) {
        return new ConsoleLogger(name);
    }

    public static Logger getLogger(Class<?> type) {
        return new ConsoleLogger(type.getSimpleName());
    }

    private static final class ConsoleLogger implements Logger {

        private final String name;

        ConsoleLogger(String name) {
            this.name = name;
        }

        /** slf4j's "{}" placeholders, filled positionally. */
        private String fmt(String format, Object... args) {
            if (args == null || args.length == 0) {
                return format;
            }
            StringBuilder out = new StringBuilder();
            int arg = 0;
            int i = 0;
            while (i < format.length()) {
                if (arg < args.length && i + 1 < format.length()
                        && format.charAt(i) == '{' && format.charAt(i + 1) == '}') {
                    out.append(args[arg++]);
                    i += 2;
                } else {
                    out.append(format.charAt(i++));
                }
            }
            return out.toString();
        }

        private void out(String level, String msg) {
            System.out.println(level + " " + name + " - " + msg);
        }

        @Override
        public void info(String msg) {
            out("INFO", msg);
        }

        @Override
        public void info(String format, Object... args) {
            out("INFO", fmt(format, args));
        }

        @Override
        public void debug(String msg) {
            out("DEBUG", msg);
        }

        @Override
        public void debug(String format, Object... args) {
            out("DEBUG", fmt(format, args));
        }

        @Override
        public void error(String msg) {
            out("ERROR", msg);
        }

        @Override
        public void error(String format, Object... args) {
            out("ERROR", fmt(format, args));
        }

        @Override
        public void warn(String msg) {
            out("WARN", msg);
        }

        @Override
        public void warn(String format, Object... args) {
            out("WARN", fmt(format, args));
        }
    }
}
