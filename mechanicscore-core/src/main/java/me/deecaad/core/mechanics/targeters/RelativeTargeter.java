package me.deecaad.core.mechanics.targeters;

/**
 * Base for targeters whose targets are positioned relative to an origin context
 * (see {@link Targeter#getFrom()}), selected via the {@code From=} context reference.
 */
public abstract class RelativeTargeter extends Targeter {

    public RelativeTargeter() {
    }
}
