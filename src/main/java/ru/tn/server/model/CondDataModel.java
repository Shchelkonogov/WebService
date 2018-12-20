package ru.tn.server.model;

import javax.json.bind.annotation.JsonbTransient;
import javax.xml.bind.annotation.XmlTransient;
import java.io.Serializable;
import java.util.List;

/**
 * Модель данных объекта с списком его состояний
 */
public class CondDataModel implements Serializable {

    private long muid;
    private List<Integer> cond;

    @XmlTransient
    @JsonbTransient
    private String maxTimeStamp;

    /**
     * Конструктор по умолчанию
     */
    public CondDataModel() {
    }

    /**
     * Конструктор
     * @param muid идентификатор объекта
     * @param cond состояния объекта
     * @param maxTimeStamp максимальное время для состояния
     */
    public CondDataModel(long muid, List<Integer> cond, String maxTimeStamp) {
        this.muid = muid;
        this.cond = cond;
        this.maxTimeStamp = maxTimeStamp;
    }

    /**
     * Возвращает идентификатор объекта
     * @return идентификатор объекта
     */
    public long getMuid() {
        return muid;
    }

    public void setMuid(long muid) {
        this.muid = muid;
    }

    /**
     * Возвращает состояния объекта
     * @return состояния
     */
    public List<Integer> getCond() {
        return cond;
    }

    public void setCond(List<Integer> cond) {
        this.cond = cond;
    }

    /**
     * Возвращает максимальное время по состоянию
     * @return время
     */
    @XmlTransient
    public String getMaxTimeStamp() {
        return maxTimeStamp;
    }

    @XmlTransient
    public void setMaxTimeStamp(String maxTimeStamp) {
        this.maxTimeStamp = maxTimeStamp;
    }
}
