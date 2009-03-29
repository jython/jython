package org.python.core;

import org.python.antlr.ParseException;

public interface Pragma {
    void addTo(PragmaReceiver receiver);

    public abstract class PragmaModule {
        public final String name;

        protected PragmaModule(String name) {
            this.name = name;
        }

        public abstract Pragma getPragma(String name);

        public abstract Pragma getStarPragma();
    }

    public final class ForbiddenPragmaModule extends PragmaModule {
        private final String message;

        public ForbiddenPragmaModule(String name) {
            this(name, "pragma " + name + " is not allowed in this context.");
        }

        public ForbiddenPragmaModule(String name, String message) {
            super(name);
            this.message = message;
        }

        @Override
        public Pragma getPragma(String name) {
            throw new ParseException(message);
        }

        @Override
        public Pragma getStarPragma() {
            throw new ParseException(message);
        }

        public void addTo(PragmaReceiver receiver) {
            throw new ParseException(message);
        }
    }
}
