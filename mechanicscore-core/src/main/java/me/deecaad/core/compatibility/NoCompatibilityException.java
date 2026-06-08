package me.deecaad.core.compatibility;

/**
 * A {@link HeadlessOperationException} for the specific case of a missing NMS {@link ICompatibility}
 * layer - i.e. we are running without a real server (headless verification under MockBukkit, where
 * there is no {@code net.minecraft.server}). Catching {@link HeadlessOperationException} handles this
 * along with any other headless-unsupported operation; catch this subtype to react to the NMS case
 * specifically.
 */
public class NoCompatibilityException extends HeadlessOperationException {

    public NoCompatibilityException(String message) {
        super(message);
    }
}
