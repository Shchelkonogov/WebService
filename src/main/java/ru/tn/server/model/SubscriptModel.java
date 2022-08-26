package ru.tn.server.model;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;
import java.util.StringJoiner;

/**
 * Модель данных для подписи объектов пользователя
 */
@XmlRootElement()
public class SubscriptModel {

    @XmlElement(name = "controlId")
    private String clientName;
    private List<Long> muid;

    /**
     * Возвращает пользователя
     * @return пользователь
     */
    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
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

    @Override
    public String toString() {
        return new StringJoiner(", ", SubscriptModel.class.getSimpleName() + "[", "]")
                .add("clientName='" + clientName + "'")
                .add("muid=" + muid)
                .toString();
    }
}
