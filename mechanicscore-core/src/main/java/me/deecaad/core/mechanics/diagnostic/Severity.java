package me.deecaad.core.mechanics.diagnostic;

/**
 * A diagnostic severity. {@link #ERROR} blocks compilation; {@link #WARNING}
 * is logged but the program still compiles.
 */
public enum Severity {
    ERROR,
    WARNING
}
