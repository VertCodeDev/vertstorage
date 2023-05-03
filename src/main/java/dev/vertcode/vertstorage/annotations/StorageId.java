package dev.vertcode.vertstorage.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface StorageId {

    /**
     * If the ID is automatically generated. (MUST be a number)
     *
     * @return If the ID is automatically generated
     */
    boolean automaticallyGenerated() default false;

}
