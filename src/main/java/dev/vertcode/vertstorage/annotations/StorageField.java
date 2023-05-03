package dev.vertcode.vertstorage.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface StorageField {

    /**
     * The name of the column.
     *
     * @return The name of the column
     */
    String columnName();
}
