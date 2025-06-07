package me.deecaad.core.file;

import org.jetbrains.annotations.NotNull;

public enum SearchMode {
    /**
     * If the class should never be searched
     */
    DISABLED {
        @Override
        public boolean shouldInclude(@NotNull SearchMode currentMode) {
            throw new IllegalArgumentException("No point is searching for nothing!");
        }
    },
    /**
     * Requests all normal searched, but also looks a little deeper for the on-demand.
     */
    ON_DEMAND {
        @Override
        public boolean shouldInclude(@NotNull SearchMode currentMode) {
            return currentMode == ENABLED || currentMode == ON_DEMAND;
        }
    },
    /**
     * Requests all normal searches, but does not look for the on-demand.
     */
    ENABLED {
        @Override
        public boolean shouldInclude(@NotNull SearchMode currentMode) {
            return currentMode == ENABLED;
        }
    };

    public abstract boolean shouldInclude(@NotNull SearchMode currentMode);
}
