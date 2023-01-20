package ru.tn.server.util;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import javax.enterprise.inject.Produces;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

/**
 * @author Maksim Shchelkonogov
 */
public class JsonbProducer {

    @Produces
    @Json()
    public Jsonb produceWithNotNullValueJsonb() {
        return JsonbBuilder.create();
    }

    @Produces
    @Json(withNull = true)
    public Gson produceWithNullValueJsonb() {
        return new GsonBuilder().serializeNulls().setFieldNamingStrategy(FieldNamingPolicy.UPPER_CAMEL_CASE).create();
    }
}

