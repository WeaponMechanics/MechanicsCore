package me.deecaad.core.diagnostic;

/**
 * A diagnostic severity. {@link #ERROR} blocks compilation/loading; {@link #WARNING}
 * is logged but the program still loads; {@link #INFO} is advisory (e.g. a key that
 * has no effect given its siblings).
 */
public enum Severity {
    ERROR("E"),
    WARNING("W"),
    INFO("I");

    private final String code;

    Severity(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }
}
