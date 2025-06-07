package me.deecaad.core.file;

import org.jetbrains.annotations.NotNull;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation controls {@link JarSearcher} behavior on when to include the
 * annotated class in the search results.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface SearcherFilter {
    @NotNull SearchMode value();
}
