package ru.tn.server.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.xml.bind.annotation.*;

/**
 * Модель данный для json
 */
public class DataModel {

    @XmlElement(name = "Par_name")
    private String parMemo;

    @XmlElement(name = "Stat_aggr")
    private String parDesc;

    @XmlElement(name = "Time_stamp")
    private String date;

    @XmlElement(name = "Par_value")
    private String value;

    public DataModel(String parMemo, String parDesc, String date, String value) {
        this.parMemo = parMemo;
        this.parDesc = parDesc;
        this.date = date;
        this.value = value;
    }

    @JsonProperty("Par_name")
    public String getParMemo() {
        return parMemo;
    }

    @JsonProperty("Par_name")
    public void setParMemo(String parMemo) {
        this.parMemo = parMemo;
    }

    @JsonProperty("Stat_aggr")
    public String getParDesc() {
        return parDesc;
    }

    @JsonProperty("Stat_aggr")
    public void setParDesc(String parDesc) {
        this.parDesc = parDesc;
    }

    @JsonProperty("Time_stamp")
    public String getDate() {
        return date;
    }

    @JsonProperty("Time_stamp")
    public void setDate(String date) {
        this.date = date;
    }

    @JsonProperty("Par_value")
    public String getValue() {
        return value;
    }

    @JsonProperty("Par_value")
    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("DataModel{");
        sb.append("parMemo='").append(parMemo).append('\'');
        sb.append(", parDesc='").append(parDesc).append('\'');
        sb.append(", date='").append(date).append('\'');
        sb.append(", value='").append(value).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
