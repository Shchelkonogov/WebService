package ru.tn.server.model;

import javax.json.bind.annotation.JsonbProperty;

/**
 * Модель данный для json
 */
public class DataModel {

    @JsonbProperty("Par_name")
    private String parMemo;

    @JsonbProperty("Stat_aggr")
    private String parDesc;

    @JsonbProperty("Time_stamp")
    private String date;

    @JsonbProperty("Par_value")
    private String value;

    @JsonbProperty("Condition")
    private String cond;

    public DataModel(String parMemo, String parDesc, String date, String value, String cond) {
        this.parMemo = parMemo;
        this.parDesc = parDesc;
        this.date = date;
        this.value = value;
        this.cond = cond;
    }

    public String getParMemo() {
        return parMemo;
    }

    public String getParDesc() {
        return parDesc;
    }

    public String getDate() {
        return date;
    }

    public String getValue() {
        return value;
    }

    public String getCond() {
        return cond;
    }

    @Override
    public String toString() {
        return "DataModel{" + "parMemo='" + parMemo + '\'' +
                ", parDesc='" + parDesc + '\'' +
                ", date='" + date + '\'' +
                ", value='" + value + '\'' +
                ", cond='" + cond + '\'' +
                '}';
    }
}
