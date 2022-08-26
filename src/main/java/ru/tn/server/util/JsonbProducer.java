package ru.tn.server.util;

import javax.enterprise.inject.Produces;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

/**
 * @author Maksim Shchelkonogov
 */
public class JsonbProducer {

    @Produces
    public Jsonb produceJsonb() {
        return JsonbBuilder.create();
    }
}

