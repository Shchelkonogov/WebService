package ru.tn.server.model;

import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.List;

/**
 * Модель данных для подписи объектов пользователя
 */
@XmlRootElement()
public class SubscriptModel implements Serializable {

    private String controlId;
    private List<Long> muid;

    /**
     * Конструктор по умолчанию
     */
    public SubscriptModel() {
    }

    /**
     * Возвращает пользователя
     * @return пользователь
     */
    public String getControlId() {
        return controlId;
    }

    public void setControlId(String controlId) {
        this.controlId = controlId;
    }

    /**
     * Возвращает объекты пользователя
     * @return объекты
     */
    public List<Long> getMuid() {
        return muid;
    }

    public void setMuid(List<Long> muid) {
        this.muid = muid;
    }
}
