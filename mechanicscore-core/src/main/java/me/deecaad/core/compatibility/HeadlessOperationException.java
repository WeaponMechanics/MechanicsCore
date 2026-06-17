package me.deecaad.core.compatibility;

/**
 * Signals that an operation cannot be performed in a headless context - i.e. without a live server
 * (for example, config verification under MockBukkit, where there is no {@code net.minecraft.server}).
 *
 * <p>It is a precise, catchable marker: config verification catches this type to report a feature as
 * "in-game only", while every other exception still propagates as a genuine error (never bucketed by a
 * broad {@code catch (Throwable)}). Downstream plugins can throw this - or a subclass - from their own
 * server-dependent serializer paths so those paths are reported, not mistaken for config mistakes or
 * bugs. {@link NoCompatibilityException} is the built-in subclass for a missing NMS layer.
 */
public class HeadlessOperationException extends RuntimeException {

    public HeadlessOperationException(String message) {
        super(message);
    }

    public HeadlessOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
