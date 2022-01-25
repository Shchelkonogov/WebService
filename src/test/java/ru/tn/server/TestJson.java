package ru.tn.server;

import ru.tn.server.entity.TubesEntity;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import java.lang.reflect.Array;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

        System.out.printf("json to object: %s%n", tubesEntity);

        List<TubesEntity> tubesEntityList = Arrays.asList(tubes, tubesEntity);

        System.out.println(jsonb.toJson(tubesEntityList));
    }
}
