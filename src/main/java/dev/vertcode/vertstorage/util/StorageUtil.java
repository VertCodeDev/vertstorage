package dev.vertcode.vertstorage.util;

import com.fatboyindustrial.gsonjavatime.Converters;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.experimental.UtilityClass;

@UtilityClass
public class StorageUtil {

    private static GsonBuilder GSON_BUILDER = Converters.registerAll(new GsonBuilder()
            .excludeFieldsWithModifiers(128)
            .serializeNulls()
            .enableComplexMapKeySerialization()
    );
    private static Gson GSON = GSON_BUILDER.create();

    /**
     * Get the Gson instance.
     *
     * @return The Gson instance
     */
    public static Gson getGson() {
        return GSON;
    }

    /**
     * Set the Gson instance.
     *
     * @param gson The Gson instance
     */
    public static void setGson(Gson gson) {
        GSON = gson;
    }

    /**
     * Get the GsonBuilder instance.
     *
     * @return The GsonBuilder instance
     */
    public static GsonBuilder getGsonBuilder() {
        return GSON_BUILDER;
    }

    /**
     * Set the GsonBuilder instance.
     *
     * @param gsonBuilder The GsonBuilder instance
     */
    public static void setGsonBuilder(GsonBuilder gsonBuilder) {
        GSON_BUILDER = gsonBuilder;
    }

}
