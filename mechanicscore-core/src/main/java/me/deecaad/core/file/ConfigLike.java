package me.deecaad.core.file;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface ConfigLike {

    boolean contains(String key);

    /**
     * Enumerates the child keys under the given path. Used to diff present config keys against a
     * serializer's declared schema for hallucinated-key detection.
     *
     * @param path The dotted path of the section, or null/empty for the root.
     * @param deep Whether to include nested keys (dotted) or only immediate children.
     * @return The child keys, or an empty collection if the path is not a section.
     */
    default Collection<String> getKeys(String path, boolean deep) {
        return List.of();
    }

    default Object get(String key) {
        return get(key, null);
    }

    Object get(String key, Object def);

    boolean isString(String key);

    default String getString(String key) {
        Object temp = get(key);
        if (temp == null || temp instanceof Collection<?> || temp instanceof Map<?, ?>)
            return null;

        return temp.toString();
    }

    List<?> getList(String key);

    String getLocation(File localFile, String localPath);
}
