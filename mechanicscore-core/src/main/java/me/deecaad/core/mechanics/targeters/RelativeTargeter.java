package me.deecaad.core.mechanics.targeters;

/**
 * Base for targeters whose targets are positioned relative to an origin context
 * (see {@link Targeter#getFrom()}). The old {@code Use_Target} flag is replaced
 * by the general {@code From=} context reference.
 */
public abstract class RelativeTargeter extends Targeter {

    public RelativeTargeter() {
    }
}
