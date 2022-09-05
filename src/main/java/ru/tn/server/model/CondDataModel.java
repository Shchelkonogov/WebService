package ru.tn.server.model;

import javax.json.bind.annotation.JsonbTransient;
import java.time.LocalDateTime;
import java.util.List;
import java.util.StringJoiner;

/**
 * Модель данных объекта с списком его состояний
 */
public class CondDataModel {

    private long muid;
    private List<Short> cond;

    @JsonbTransient
    private LocalDateTime maxTimeStamp;

    /**
     * Конструктор
     * @param muid идентификатор объекта
     * @param cond состояния объекта
     * @param maxTimeStamp максимальное время для состояния
     */
    public CondDataModel(long muid, List<Short> cond, LocalDateTime maxTimeStamp) {
        this.muid = muid;
        this.cond = cond;
        setMaxTimeStamp(maxTimeStamp);
    }

    /**
     * Возвращает идентификатор объекта
     * @return идентификатор объекта
     */
    public long getMuid() {
        return muid;
    }

    /**
     * Возвращает состояния объекта
     * @return состояния
     */
    public List<Short> getCond() {
        return cond;
    }

    /**
     * Возвращает максимальное время по состоянию
     * @return время
     */
    public LocalDateTime getMaxTimeStamp() {
        return maxTimeStamp;
    }

    private void setMaxTimeStamp(LocalDateTime maxTimeStamp) {
        this.maxTimeStamp = maxTimeStamp;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", CondDataModel.class.getSimpleName() + "[", "]")
                .add("muid=" + muid)
                .add("cond=" + cond)
                .add("maxTimeStamp='" + maxTimeStamp + "'")
                .toString();
    }
}
