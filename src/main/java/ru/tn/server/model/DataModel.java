package ru.tn.server.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.xml.bind.annotation.*;
import java.io.Serializable;

/**
 * Модель данный для json
 */
public class DataModel implements Serializable {

    @XmlElement(name = "Par_name")
    private String parMemo;

    @XmlElement(name = "Stat_aggr")
    private String parDesc;

    @XmlElement(name = "Time_stamp")
    private String date;

    @XmlElement(name = "Par_value")
    private String value;

    @XmlElement(name = "Condition")
    private String cond;

    public DataModel() {
    }

    public DataModel(String parMemo, String parDesc, String date, String value, String cond) {
        this.parMemo = parMemo;
        this.parDesc = parDesc;
        this.date = date;
        this.value = value;
        this.cond = cond;
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

    @JsonProperty("Condition")
    public String getCond() {
        return cond;
    }

    @JsonProperty("Condition")
    public void setCond(String cond) {
        this.cond = cond;
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
