package me.deecaad.core.file;

import me.deecaad.core.file.verify.ConfigSchema;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface Serializer<T> {

    /**
     * Returns the unique identifier to this serializer. This identifier is used to determine when to
     * apply the serializer to a config section. The identifier is case-sensitive, and it is ignored
     * when <code>null</code>.
     *
     * <p>
     * Generally speaking, you should always override this method. When you do not need automatic
     * serializer handling, you may return null.
     *
     * @return The nullable unique identifier.
     */
    default @Nullable String getKeyword() {
        return null;
    }

    /**
     * Allows using this serializer under other serializers if the current path ends with any given
     * string in this list.
     *
     * <p>
     * Example values here could be Arrays.asList("Spread.Spread_Image", "Recoil_Pattern"). Which would
     * allow this serializer to be used under Spread.Spread_Image and Recoil_Patten parent keywords.
     *
     * @return The nullable parent paths
     */
    default @Nullable List<String> getParentKeywords() {
        return null;
    }

    /**
     * After the {@link #getKeyword()} check and {@link #getParentKeywords()} check, this final check
     * can be customized by the serializer in order to "fine tune" when a serializer should be
     * automatically serialized.
     *
     * @param data The config information.
     * @return true if the serializer should serialize.
     */
    default boolean shouldSerialize(@NotNull SerializeData data) {
        return true;
    }

    /**
     * Returns a link to the page on the wiki that describes this serializer. This method is called from
     * {@link SerializeData}, and is used to help the user find potential solutions to their problem.
     *
     * @return The nullable link to the wiki.
     */
    @Nullable default String getWikiLink() {
        return null;
    }

    @NotNull default String getName() {
        // Sometimes a class will end with 'Serializer' in its name, like
        // 'ColorSerializer'. This information may be confusing to some people,
        // so we can strip it away here.
        String simple = getClass().getSimpleName();
        int index = simple.indexOf("Serializer");
        if (index > 0)
            simple = simple.substring(0, index);
        return simple;
    }

    /**
     * Instantiates a new Object to be added into the finalized configuration. The object should be
     * built off of {@link SerializeData#getConfig()}. If there is any misconfiguration (or any other
     * issue preventing the construction of an object), then this method should throw a
     * {@link SerializerException}. This method may not return null.
     *
     * @param data The non-null data containing config
     * @return The non-null serialized data.
     * @throws SerializerException If there is an error in config.
     */
    @NotNull T serialize(@NotNull SerializeData data) throws SerializerException;

    /**
     * Declares every config key this serializer accepts. When non-null, the framework validates raw
     * config against this schema (presence, type, range, unknown keys) for exact hallucinated-key
     * detection and JSON Schema export. The schema is validation + export only; construction always
     * goes through {@link #serialize(SerializeData)}. Defaults to null (no schema declared).
     *
     * @return The nullable declared schema.
     */
    default @Nullable ConfigSchema schema() {
        return null;
    }
}
