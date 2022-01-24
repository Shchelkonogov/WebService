package ru.tn.server;

import ru.tn.server.entity.TubesEntity;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import java.time.LocalDateTime;

/**
 * @author Maksim Shchelkonogov
 */
public class TestJson {

    public static void main(String[] args) {
        TubesEntity tubes = new TubesEntity();
        tubes.setBrand("1");
        tubes.setClientId("2");
        tubes.setStatus(5L);
        tubes.setTimeStamp(LocalDateTime.now());

        Jsonb jsonb = JsonbBuilder.create();

        String jsonTube = jsonb.toJson(tubes);

        System.out.printf("object TubesEntity to json: %s%n", jsonTube);

        jsonTube = "{\"brand\":\"1\",\"clientId\":\"2\",\"status\":5,\"timeStamp\":\"11.01.2025 11:26:44\"}";

        TubesEntity tubesEntity = jsonb.fromJson(jsonTube, TubesEntity.class);

        System.out.printf("json to object: %s", tubesEntity);
    }
}
