package me.deecaad.core.file.verify;

/**
 * The declared datatype of a {@link KeySpec}. Drives both coercion during validation and the
 * exported JSON Schema type.
 */
public enum KeyType {
    INT,
    DOUBLE,
    BOOL,
    STRING,
    COLOR,
    MATERIAL,
    ENTITY,
    ENUM,
    REGISTRY,
    REGISTRY_SERIALIZER,
    REGISTRY_SERIALIZER_LIST,
    NESTED,
    LIST
}
