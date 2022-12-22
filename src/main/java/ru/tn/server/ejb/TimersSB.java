package ru.tn.server.ejb;

import javax.annotation.Resource;
import javax.ejb.*;
import javax.inject.Inject;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Maksim Shchelkonogov
 */
@Singleton
@Startup
@LocalBean
public class TimersSB {

    private static final String SELECT_CLIENTS = "select client_name, client_path from iasdtu_clients";
    private static final String DEL_COND_DATA = "truncate table iasdtu_data";

    @Inject
    private Logger logger;

    @Resource(name = "jdbc/DataSource")
    private DataSource ds;

    @EJB
    private StatisticSB statisticBean;

    @EJB
    private ConsumersSB consumersBean;

    /**
     * Schedule метод который каждую минуту пытается отправить
     * состояния подписанных объектов
     */
    @Schedule(minute="*/1", hour="*", persistent = false)
    private void timer() {
        try (Connection connect = ds.getConnection();
             PreparedStatement stm = connect.prepareStatement(SELECT_CLIENTS)) {
            ResultSet res = stm.executeQuery();
            while (res.next()) {
                consumersBean.sendStates(res.getString("client_name"), res.getString("client_path"));
            }
        } catch (SQLException ex) {
            logger.log(Level.WARNING, "error get clients", ex);
        }
    }

    /**
     * Schedule метод который каждый час очищает статистику по обращениям
     */
    @Schedule(hour = "*", persistent = false)
    private void clearStatistic() {
        statisticBean.clearStatistic();
    }

    /**
     * Schedule метод, который каждый день в 00:05 чистит данные состояний
     */
    @Schedule(minute = "5", persistent = false)
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    private void removeConditionData() {
        try (Connection connect = ds.getConnection();
             PreparedStatement stm = connect.prepareStatement(DEL_COND_DATA)) {
            stm.executeUpdate();
        } catch (SQLException ex) {
            logger.log(Level.WARNING, "error delete condition data", ex);
        }
    }
}
