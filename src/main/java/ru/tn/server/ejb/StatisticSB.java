package ru.tn.server.ejb;

import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Maksim Shchelkonogov
 */
@Singleton
@Startup
@LocalBean
public class StatisticSB {

    private Map<String, AtomicInteger> clients = new HashMap<>();
    private Set<Long> muidStatistic = new HashSet<>();

    @Inject
    private Logger logger;

    @EJB
    private ConsumersSB consumersBean;

    public void checkClient(String clientName, boolean sendStatus) {
        if (clients.containsKey(clientName)) {
            if (sendStatus) {
                if (clients.get(clientName).get() > 0) {
                    clients.get(clientName).set(0);
                }
            } else {
                if (clients.get(clientName).incrementAndGet() == 15) {
                    clients.get(clientName).set(0);
                    consumersBean.removeSubs(clientName);
                }
            }
        } else {
            if (sendStatus) {
                clients.put(clientName, new AtomicInteger(0));
            } else {
                clients.put(clientName, new AtomicInteger(1));
            }
        }

        for(Map.Entry<String, AtomicInteger> item: clients.entrySet()) {
            logger.log(Level.INFO, "errors for user: {0} count: {1}", new Object[] {item.getKey(), item.getValue()});
        }
    }

    /**
     * Добавляем muid в статистику
     * @param muid asot muid
     */
    public void addMuidToStatistic(long muid) {
        muidStatistic.add(muid);
    }

    /**
     * Получение статистики по обращениям к архивным и мгновенным данным
     * @return статистика
     */
    public Set<Long> getMuidStatistic() {
        return muidStatistic;
    }

    /**
     * Метод очищает статистику
     */
    public void clearStatistic() {
        muidStatistic.clear();
    }
}
