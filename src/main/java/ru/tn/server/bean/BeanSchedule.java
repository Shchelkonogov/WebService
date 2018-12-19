package ru.tn.server.bean;

import javax.ejb.EJB;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import java.net.URI;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Singleton bean который занимается хранением данных url
 * ежеминутной отправкой данных и проверкой ошибочной доставки
 */
@Startup
@Singleton
public class BeanSchedule {

    private URI url;
    private HashMap<String, Integer> clients = new HashMap<>();

    @EJB
    private Bean bean;

    /**
     * Schedule метод который каждую минуту пытается отправить
     * состояния подписанных объектов
     */
    @Schedule(minute="*/1", hour="*", persistent = false)
    private void timer() {
        System.out.println("Timer tic! " + LocalTime.now());
        if(Objects.nonNull(url)) {
            bean.send();
        }
    }

    /**
     * Метод проверки как часто происходила ошибка доставки данных
     * Если 15 раз не доставили данные то подпись стирается
     * @param client клиент
     */
    public void checkUser(String client) {
        if(clients.containsKey(client)) {
            int count = clients.get(client);
            if(count == 15) {
                clients.remove(client);
                bean.removeSubs(client);
            } else {
                count++;
                clients.put(client, count);
            }
        } else {
            clients.put(client, 0);
        }

        for(Map.Entry item: clients.entrySet()) {
            System.out.println("BeanSchedule.checkUser errors for user: " + item.getKey() + " count: " + item.getValue());
        }
    }

    /**
     * Метод возвращает url
     * @return url
     */
    public URI getUrl() {
        return url;
    }

    /**
     * Устанавливаем url этого сервиса
     * @param url url сервиса
     */
    public void setUrl(URI url) {
        this.url = url;
    }
}
