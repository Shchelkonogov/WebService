package ru.tn.server.model;

import javax.json.bind.annotation.JsonbProperty;
import java.util.StringJoiner;

/**
 * Класс для описания паспортной информации объекта
 * @author Maksim Shchelkonogov
 */
public class PassportData {

    @JsonbProperty("Name")
    private String name;

    @JsonbProperty("Value")
    private String value;

    @JsonbProperty("Type")
    private String type;

    public PassportData(String name, String value, String type) {
        this.name = name;
        this.value = value;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public String getType() {
        return type;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", PassportData.class.getSimpleName() + "[", "]")
                .add("name='" + name + "'")
                .add("value='" + value + "'")
                .add("type='" + type + "'")
                .toString();
    }
}
