package gr.codelearn.springbootai.coredemo.catalog;

import org.jspecify.annotations.Nullable;

public record Category(long id, String name, @Nullable String description, @Nullable Long parentId) {
}
