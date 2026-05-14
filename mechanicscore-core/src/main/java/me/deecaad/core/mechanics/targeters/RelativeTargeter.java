package me.deecaad.core.mechanics.targeters;

import me.deecaad.core.file.SerializeData;
import me.deecaad.core.file.SerializerException;

public abstract class RelativeTargeter extends Targeter {

    protected boolean isUseTarget;

    /**
     * Default constructor for serializer
     */
    public RelativeTargeter() {
    }

    public boolean isUseTarget() {
        return isUseTarget;
    }

    /**
     * Usually used to force a targeter to use target, when nothing else makes
     * sense.
     *
     * @param isUseTarget Whether to use the target.
     */
    public void setUseTarget(boolean isUseTarget) {
        this.isUseTarget = isUseTarget;
    }

    @Override
    protected Targeter applyParentArgs(SerializeData data, Targeter targeter) throws SerializerException {
        RelativeTargeter relativeTargeter = (RelativeTargeter) super.applyParentArgs(data, targeter);
        relativeTargeter.isUseTarget = data.of("Use_Target").getBool().orElse(false);
        return relativeTargeter;
    }
}
