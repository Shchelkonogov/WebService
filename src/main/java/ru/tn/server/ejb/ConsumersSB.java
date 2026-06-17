package ru.tn.server.ejb;

import jakarta.annotation.Resource;
import jakarta.ejb.*;
import jakarta.inject.Inject;
import jakarta.json.bind.Jsonb;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import ru.tn.server.model.*;
import ru.tn.server.util.ConsumersException;
import ru.tn.server.util.Json;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Maksim Shchelkonogov
 */
@Stateless
@LocalBean
public class ConsumersSB {

    private static final String SEL_PASSPORT_DATA = "select prop_name, prop_value, prop_type from iasdtu.get_obj_props(?, ?)";

    private static final String SEL_MUID = "select muid, last_send_condition from admin.iasdtu_subscr " +
            "where client_id = (select client_id from admin.iasdtu_clients where client_name = ?)";
    private static final String SEL_COND = "select cond, case val when 'ДА' then 1 else 0 end, time_stamp " +
            "from admin.iasdtu_data a, admin.obj_type_prop_val_vie b " +
            "where muid = ? " +
            "and time_stamp > ? " +
            "and obj_prop_id = 60 " +
            "and a.obj_id = b.obj_id " +
            "order by time_stamp, id";

    private static final String SQL_STATISTIC = "select admin.get_obj_filial(obj_id), count(*) " +
            "from admin.obj_object " +
            "where obj_id in (select id_aspid from admin.gis_object where id_gis = any(?)) " +
            "group by admin.get_obj_filial(obj_id) " +
            "order by replace(admin.get_obj_filial(obj_id), 'Филиал ', '')::smallint";

    private static final String SQL_STRING = "select n1, n2, n3, n4, n5 from iasdtu.get_hist_data(?)";
    private static final String SQL_STRING_INST = "select n1, n2, n3, n4, n5 from iasdtu.get_async_data(?)";

    private static final String DEL_SUBS = "delete from admin.iasdtu_subscr " +
            "where client_id = (select client_id from admin.iasdtu_clients where client_name = ?)";

    private static final String SEL_SUBSCRIPTIONS = "select muid from admin.iasdtu_subscr " +
            "where client_id = (select client_id from admin.iasdtu_clients where client_name = ?)";

    private static final String SEL_CLIENT = "select client_id from admin.iasdtu_clients where client_name = ?";
    private static final String DEL_CLIENT_SUBS = "delete from admin.iasdtu_subscr where client_id = ?";
    private static final String INS_CLIENT_SUBS = "insert into admin.iasdtu_subscr (client_id, muid) values (?, ?)";

    private static final String UPD_LAST_SEND_CONDITION_DATE = "update admin.iasdtu_subscr set last_send_condition = ? " +
            "where client_id = (select client_id from admin.iasdtu_clients where client_name = ?) and muid = ?";

    private static final String SEL_LAST_STATES = "select a.muid, a.cond, case val when 'ДА' then 1 else 0 end, a.time_stamp from admin.iasdtu_last_state a, admin.obj_type_prop_val_vie b " +
            "    where a.muid in " +
            "          (select muid::varchar from admin.iasdtu_subscr " +
            "            where client_id = " +
            "                  (select client_id from admin.iasdtu_clients where client_name = ?)) " +
            "        and obj_prop_id = 60 " +
            "        and a.obj_id = b.obj_id";

    private static final String SEL_CLIENT_PATH = "select client_path from admin.iasdtu_clients where client_name = ?";

    private static final String HIST_DATA = "h";
    private static final String INSTANT_DATA = "i";

    @Inject
    private Logger logger;

    @Inject
    @Json
    private Jsonb jsonb;

    @Resource(name = "jdbc/DataSource")
    private DataSource ds;

    @Resource
    private EJBContext context;

    @EJB
    private ConsumersSB consumersBean;

    @EJB
    private StatisticSB statisticBean;

    /**
     * Отправка последних известных состояний объектов клиенту
     * @param clientName идентификатор клиента
     */
    public void sendLastStates(String clientName) {
        try (Connection connect = ds.getConnection();
             PreparedStatement stm = connect.prepareStatement(SEL_CLIENT_PATH)) {
            stm.setString(1, clientName);
            ResultSet res = stm.executeQuery();
            if (res.next()) {
                consumersBean.sendStates(clientName, res.getString("client_path"), true);
            }
        } catch (SQLException ex) {
            logger.log(Level.WARNING, "error get client path", ex);
        }
    }

    /**
     * Отправка состояний объектов клиенту
     * @param clientName идентификатор клиента
     * @param clientPath адрес куда отправлять
     */
    public void sendStates(String clientName, String clientPath) {
        consumersBean.sendStates(clientName, clientPath, false);
    }

    /**
     * Отправка состояний объектов клиенту
     * @param clientName идентификатор клиента
     * @param clientPath адрес куда отправлять
     * @param lastStates true - отправить последние известные состояния,
     *                   false - отправить состояния, которые не были отправлены клиенту
     */
    @Asynchronous
    public void sendStates(String clientName, String clientPath, boolean lastStates) {
        logger.log(Level.INFO, "start sending states to {0} {1}", new Object[] {clientName, clientPath});

        List<CondDataModel> model;
        if (lastStates) {
            model = consumersBean.getLastStates(clientName);
        } else {
            model = consumersBean.getData(clientName);
        }

        if (!model.isEmpty()) {
            try {
                logger.log(Level.INFO, "send to {0} {1} states {2}", new Object[]{clientName, clientPath, model});

                Client client = ClientBuilder.newClient();
                WebTarget target = client.target(clientPath);

                Response resp = target
                        .request()
                        .post(Entity.entity(jsonb.toJson(model), MediaType.APPLICATION_JSON));

                logger.log(Level.INFO, "response status {0}", resp.getStatus());

                if (resp.getStatus() != 200) {
                    statisticBean.checkClient(clientName, false);
                } else {
                    statisticBean.checkClient(clientName, true);
                    consumersBean.updateLastSendDateCondition(model, clientName);
                }
                client.close();
            } catch (Exception e) {
                logger.log(Level.WARNING, "error while sending states", e);
                statisticBean.checkClient(clientName, false);
            }
        } else {
            logger.log(Level.INFO, "no states for sending to {0}", clientName);
        }
    }

    /**
     * Метод обновляет в таблице iasdtu_subscr поле с датой последней успешной отправки состояний
     * @param dataModels модель отправленных данных
     * @param clientName идентификатор клиента
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void updateLastSendDateCondition(List<CondDataModel> dataModels, String clientName) {
        try (Connection connect = ds.getConnection();
             PreparedStatement stm = connect.prepareStatement(UPD_LAST_SEND_CONDITION_DATE)) {
            for (CondDataModel condModel: dataModels) {
                stm.setTimestamp(1, Timestamp.valueOf(condModel.getMaxTimeStamp()));
                stm.setString(2, clientName);
                stm.setLong(3, condModel.getMuid());

                stm.addBatch();
            }

            stm.executeBatch();
        } catch (SQLException e) {
            logger.log(Level.WARNING, "error update last sent date", e);
        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void subscript(SubscriptModel model) throws ConsumersException {
        try (Connection connect = ds.getConnection();
             PreparedStatement stmGetClient = connect.prepareStatement(SEL_CLIENT);
             PreparedStatement stmDeleteClientSubs = connect.prepareStatement(DEL_CLIENT_SUBS);
             PreparedStatement stmInsertClientSubs = connect.prepareStatement(INS_CLIENT_SUBS)) {
            stmGetClient.setString(1, model.getClientName());

            ResultSet res = stmGetClient.executeQuery();
            if (res.next()) {
                String clientID = res.getString("client_id");

                stmDeleteClientSubs.setString(1, clientID);
                stmDeleteClientSubs.executeUpdate();

                for (Long muid: model.getMuid()) {
                    stmInsertClientSubs.setString(1, clientID);
                    stmInsertClientSubs.setBigDecimal(2, new BigDecimal(muid));

                    stmInsertClientSubs.addBatch();
                }

                stmInsertClientSubs.executeBatch();

                // В случае успешной подписи отправляю клиента последние известные состояния его объектов
                consumersBean.sendLastStates(model.getClientName());
            } else {
                logger.log(Level.WARNING, "unknown client {0}", model.getClientName());
                throw new ConsumersException("unknown client " + model.getClientName());
            }
        } catch (ConsumersException ex) {
            throw ex;
        } catch (SQLException ex) {
            context.setRollbackOnly();
            logger.log(Level.WARNING, "error subscript client " + model.getClientName(), ex);
            throw new ConsumersException("error subscript client " + model.getClientName(), ex);
        }
    }

    /**
     * Метод удаляет подписку клиента
     * @param clientName имя клиента
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void removeSubs(String clientName) {
        try (Connection connect = ds.getConnection();
             PreparedStatement stm = connect.prepareStatement(DEL_SUBS)) {
            stm.setString(1, clientName);
            stm.executeQuery();

            logger.log(Level.INFO, "remove subs for client {0}", clientName);
        } catch(SQLException e) {
            logger.log(Level.WARNING, "error remove subs", e);
        }
    }

    /**
     * Получение списка подписанных объектов клиента
     * @param clientName имя клиента
     * @return список объектов
     * @throws ConsumersException в случае ошибки запроса в базу
     */
    public List<String> getSubs(String clientName) throws ConsumersException {
        try (Connection connect = ds.getConnection();
             PreparedStatement stm = connect.prepareStatement(SEL_SUBSCRIPTIONS)) {
            ArrayList<String> result = new ArrayList<>();

            stm.setString(1, clientName);

            ResultSet res = stm.executeQuery();
            while (res.next()) {
                result.add(res.getString(1));
            }

            return result;
        } catch (SQLException e) {
            logger.log(Level.WARNING, "error load object subscription for client name {0}", clientName);
            throw new ConsumersException("error load object subscription for client name " + clientName, e);
        }
    }

    /**
     * Метод возвращает коллекция с состояния объектов пользователя
     * @param clientName пользователь
     * @return коллекция данных
     */
    public List<CondDataModel> getData(String clientName) {
        List<CondDataModel> result = new ArrayList<>();
        try (Connection connect = ds.getConnection();
             PreparedStatement stm = connect.prepareStatement(SEL_MUID);
             PreparedStatement stmCond = connect.prepareStatement(SEL_COND)) {
            List<LoadDataModel> muid = new ArrayList<>();
            stm.setString(1, clientName);

            ResultSet res = stm.executeQuery();
            while(res.next()) {
                Timestamp startTimestamp = res.getTimestamp(2);
                muid.add(new LoadDataModel(res.getLong(1), (startTimestamp == null ? null : startTimestamp.toLocalDateTime())));
            }

            for (LoadDataModel item: muid) {
                stmCond.setLong(1, item.getMuid());
                if (item.getStartDate() == null) {
                    stmCond.setTimestamp(2, Timestamp.valueOf(LocalDate.now().atStartOfDay()));
                } else {
                    stmCond.setTimestamp(2, Timestamp.valueOf(item.getStartDate()));
                }

                List<Short> cond = new ArrayList<>();
                LocalDateTime endTimeStamp = null;

                res = stmCond.executeQuery();
                while(res.next()) {
                    String condValue = res.getString(2);
                    switch (res.getInt(1)) {
                        case 0 : {
                            condValue = condValue + "0000001";
                            break;
                        }
                        case 1 : {
                            condValue = condValue + "0000010";
                            break;
                        }
                        case 2 : {
                            condValue = condValue + "0000100";
                            break;
                        }
                        case 3 : {
                            condValue = condValue + "0001000";
                            break;
                        }
                    }

                    cond.add(Short.parseShort(condValue, 2));
                    endTimeStamp = res.getTimestamp(3).toLocalDateTime();
                }

                if (!cond.isEmpty()) {
                    result.add(new CondDataModel(item.getMuid(), cond, endTimeStamp));
                }
            }
        } catch(SQLException e) {
            logger.log(Level.WARNING, "error load states", e);
        }
        return result;
    }

    /**
     * Получение списка последних известных состояний клиента
     * @param clientName идентификатор клиента
     * @return список состояний
     */
    public List<CondDataModel> getLastStates(String clientName) {
        List<CondDataModel> result = new ArrayList<>();
        try (Connection connect = ds.getConnection();
             PreparedStatement stm = connect.prepareStatement(SEL_LAST_STATES)) {
            stm.setString(1, clientName);
            ResultSet res = stm.executeQuery();
            while (res.next()) {
                String condValue = res.getString(3);
                switch (res.getInt(2)) {
                    case 0 : {
                        condValue = condValue + "0000001";
                        break;
                    }
                    case 1 : {
                        condValue = condValue + "0000010";
                        break;
                    }
                    case 2 : {
                        condValue = condValue + "0000100";
                        break;
                    }
                    case 3 : {
                        condValue = condValue + "0001000";
                        break;
                    }
                }

                result.add(
                        new CondDataModel(res.getLong(1),
                                Collections.singletonList(Short.parseShort(condValue, 2)),
                                res.getTimestamp(4).toLocalDateTime())
                );
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, "error load last states", e);
        }
        return result;
    }

    /**
     * Получение паспортных данных объекта
     * @param muid объекта
     * @param objType тип объекта
     * @return коллекция паспортной информации
     */
    public List<PassportData> getPassportData(long muid, int objType) throws ConsumersException {
        try (Connection connect = ds.getConnection();
             PreparedStatement stm = connect.prepareStatement(SEL_PASSPORT_DATA)) {
            List<PassportData> result = new ArrayList<>();

            stm.setLong(1, muid);
            stm.setLong(2, objType);

            ResultSet res = stm.executeQuery();
            while (res.next()) {
                result.add(
                        new PassportData(
                                res.getString("prop_name"),
                                res.getString("prop_value"),
                                res.getString("prop_type")
                        )
                );
            }

            return result;
        } catch (SQLException e) {
            logger.log(Level.WARNING, "error load passport data", e);
            throw new ConsumersException("Error load passport data", e);
        }
    }

    /**
     * Статистика обращений за архивными и мгновенными данными
     * @return статистика
     */
    public Map<String, String> getStatistic() throws ConsumersException {
        try (Connection connect = ds.getConnection();
             PreparedStatement stm = connect.prepareStatement(SQL_STATISTIC)) {
            Map<String, String> result = new LinkedHashMap<>();
            stm.setArray(1, connect.createArrayOf("bigint", statisticBean.getMuidStatistic().toArray()));

            ResultSet res = stm.executeQuery();
            while (res.next()) {
                result.put(res.getString(1), res.getString(2));
            }

            return result;
        } catch (SQLException ex) {
            logger.log(Level.WARNING, "error load statistic", ex);
            throw new ConsumersException("error load statistic", ex);
        }
    }

    /**
     * Получение исторических данных по заданному объекту
     * @param muid заданный объект
     * @return коллекция архивных данных
     */
    public List<DataModel> getHistData(String muid) throws ConsumersException {
        return getData(muid, HIST_DATA);
    }

    /**
     * Формирование запроса для мгновенных данных по заданному объекту
     * @param muid заданный объект
     * @return идентификатор запроса
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public String requestInstData(long muid) throws ConsumersException {
        try (Connection connect = ds.getConnection();
             CallableStatement stm = connect.prepareCall("{? = call iasdtu.set_async_request(?)}")) {
            stm.setLong(2, muid);
            stm.registerOutParameter(1, Types.VARCHAR);
            stm.execute();

            return stm.getString(1);
        } catch(SQLException e) {
            logger.log(Level.WARNING, "error async request instant data", e);
            throw new ConsumersException("error async request instant data", e);
        }
    }

    /**
     * Получение мгновенных данных по заданному объекту
     * @param id идентификатор запроса
     * @return список мгновенных данных
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public List<DataModel> getInst(String id) throws ConsumersException {
        List<DataModel> result = new ArrayList<>();

        for (int i = 0; i < 20; i++) {
            logger.log(Level.INFO, "waiting for instant data {0}мс", String.valueOf(i * 6000));
            logger.log(Level.INFO, "ID {0}", id);

            try {
                Thread.sleep(6000);
            } catch (InterruptedException e) {
                logger.log(Level.WARNING, "error waiting instant data", e);
                throw new ConsumersException("error waiting instant data");
            }

            result = getData(id, INSTANT_DATA);

            if (!result.isEmpty()) {
                break;
            }
        }

        if (result.isEmpty()) {
            logger.log(Level.WARNING, "timeout waiting instant data");
        } else {
            logger.log(Level.INFO, "instant data for muid {0} is loaded", id);
        }

        return result;
    }

    /**
     * Метод которы выгружает данные из базы
     * @param muid идентификационный номер объекта
     * @param dataType тип запрашиваемых данных
     */
    private List<DataModel> getData(String muid, String dataType) throws ConsumersException {
        try (Connection connect = ds.getConnection()) {
            PreparedStatement stmData;
            switch (dataType) {
                case HIST_DATA:
                    stmData = connect.prepareStatement(SQL_STRING);
                    break;
                case INSTANT_DATA:
                    stmData = connect.prepareStatement(SQL_STRING_INST);
                    break;
                default:
                    logger.log(Level.WARNING, "unknown select");
                    throw new ConsumersException("unknown select");
            }

            List<DataModel> result = new ArrayList<>();

            stmData.setString(1, muid);
            ResultSet res = stmData.executeQuery();
            while(res.next()) {
                result.add(new DataModel(res.getString(1),
                        res.getString(2), res.getString(3),
                        res.getString(4), res.getString(5)));
            }

            return result;
        } catch (ConsumersException ex) {
          throw ex;
        } catch(SQLException e) {
            switch (dataType) {
                case HIST_DATA:
                    logger.log(Level.WARNING, "error load hist data", e);
                    throw new ConsumersException("error load hist data" , e);
                case INSTANT_DATA:
                    logger.log(Level.WARNING, "error load instant data" , e);
                    throw new ConsumersException("error load instant data" , e);
                default:
                    logger.log(Level.WARNING, "unknown select");
                    throw new ConsumersException("unknown select");
            }
        }
    }
}
