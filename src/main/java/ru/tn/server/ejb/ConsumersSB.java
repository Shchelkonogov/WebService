package ru.tn.server.ejb;

import oracle.jdbc.OracleConnection;
import ru.tn.server.model.*;
import ru.tn.server.util.ConsumersException;

import javax.annotation.Resource;
import javax.ejb.*;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.sql.DataSource;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Maksim Shchelkonogov
 */
@Stateless
@LocalBean
public class ConsumersSB {

    private static final String ALTER_SESSION = "alter session set NLS_NUMERIC_CHARACTERS='.,'";

    private static final String SEL_PASSPORT_DATA = "select prop_name, prop_value, prop_type from table(iasdtu.get_obj_props(?, ?))";

    private static final String SEL_MUID = "select muid, LAST_SEND_CONDITION from iasdtu_subscr " +
            "where client_id = (select client_id from iasdtu_clients where client_name = ?)";
    private static final String SEL_MAX_TIME_STAMP = "select max(time_stamp) from iasdtu_data where muid = ?";
    private static final String SEL_COND = "select cond, decode(val, 'ДА' , 1, 0) " +
            "from iasdtu_data a, obj_type_prop_val_vie b " +
            "where muid = ? " +
            "and time_stamp <= ? " +
            "and TIME_STAMP > ?" +
            "and obj_prop_id = 60 " +
            "and a.obj_id = b.obj_id " +
            "order by time_stamp, id";

    private static final String SQL_STATISTIC = "select get_obj_filial(obj_id), count(*) from obj_object where obj_id in ( " +
            "select id_aspid from gis_object where id_gis in (select * from table(?))) " +
            "group by get_obj_filial(obj_id) " +
            "order by to_number(replace(get_obj_filial(obj_id),'Филиал ',''))";

    private static final String SQL_STRING = "select n1, n2, n3, n4, n5 from table(iasdtu.get_hist_data(?))";
    private static final String SQL_STRING_INST = "select n1, n2, n3, n4, n5 from table(iasdtu.get_async_data(?))";

    private static final String DEL_SUBS = "delete from iasdtu_subscr " +
            "where client_id = (select client_id from iasdtu_clients where client_name = ?)";

    private static final String SEL_SUBSCRIPTIONS = "select muid from iasdtu_subscr " +
            "where client_id = (select client_id from iasdtu_clients where client_name = ?)";

    private static final String SEL_CLIENT = "select client_id from iasdtu_clients where client_name = ?";
    private static final String DEL_CLIENT_SUBS = "delete from iasdtu_subscr where client_id = ?";
    private static final String INS_CLIENT_SUBS = "insert into iasdtu_subscr (client_id, muid)  select * from table(?)";

    private static final String UPD_LAST_SEND_CONDITION_DATE = "update IASDTU_SUBSCR set LAST_SEND_CONDITION = ? " +
            "where CLIENT_ID = (select CLIENT_ID from IASDTU_CLIENTS where CLIENT_NAME = ?) and MUID = ?";

    private static final String HIST_DATA = "h";
    private static final String INSTANT_DATA = "i";

    @Inject
    private Logger logger;

    @Inject
    private Jsonb jsonb;

    @Resource(name = "jdbc/DataSource")
    private DataSource ds;

    @Resource
    private EJBContext context;

    @EJB
    private ConsumersSB consumersBean;

    @EJB
    private StatisticSB statisticBean;

    @Asynchronous
    public void sendStates(String clientName, String clientPath) {
        logger.log(Level.INFO, "start sending states to {0} {1}", new Object[] {clientName, clientPath});
//        // TODO Сделано для теста, убрать в продуктиве.
//        if (!clientName.equals("MAXTEST")) {
//            return;
//        }

        List<CondDataModel> model = consumersBean.getData(clientName);
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
        try (OracleConnection connect = (OracleConnection) ds.getConnection();
             PreparedStatement stmGetClient = connect.prepareStatement(SEL_CLIENT);
             PreparedStatement stmDeleteClientSubs = connect.prepareStatement(DEL_CLIENT_SUBS);
             PreparedStatement stmInsertClientSubs = connect.prepareStatement(INS_CLIENT_SUBS)) {
            stmGetClient.setString(1, model.getClientName());

            ResultSet res = stmGetClient.executeQuery();
            if (res.next()) {
                String clientID = res.getString("client_id");

                stmDeleteClientSubs.setString(1, clientID);
                stmDeleteClientSubs.executeUpdate();

                List<Struct> muids = new ArrayList<>();
                for (Long muid: model.getMuid()) {
                    Object[] o = {clientID, muid};
                    muids.add(connect.createStruct("T_IASDTU_SUBS_ROW", o));
                }

                Array array = connect.createOracleArray("T_IASDTU_SUBS_TAB", muids.toArray());

                stmInsertClientSubs.setArray(1, array);
                stmInsertClientSubs.executeUpdate();
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
             PreparedStatement stmMaxTimeStamp = connect.prepareStatement(SEL_MAX_TIME_STAMP);
             PreparedStatement stmCond = connect.prepareStatement(SEL_COND)) {
            List<LoadDataModel> muid = new ArrayList<>();
            stm.setString(1, clientName);

            ResultSet res = stm.executeQuery();
            while(res.next()) {
                Timestamp startTimestamp = res.getTimestamp(2);
                muid.add(new LoadDataModel(res.getLong(1), (startTimestamp == null ? null : startTimestamp.toLocalDateTime())));
            }

            List<Integer> cond;
            for (LoadDataModel item: muid) {
                LocalDateTime endTimeStamp = null;
                stmMaxTimeStamp.setLong(1, item.getMuid());

                res = stmMaxTimeStamp.executeQuery();
                if(res.next()) {
                    Timestamp timestamp = res.getTimestamp(1);
                    endTimeStamp = (timestamp == null ? null : res.getTimestamp(1).toLocalDateTime());
                }

                if (endTimeStamp == null) {
                    return result;
                }

                List<Short> cond = new ArrayList<>();
                stmCond.setLong(1, item.getMuid());
                stmCond.setTimestamp(2, Timestamp.valueOf(endTimeStamp));
                if (item.getStartDate() == null) {
                    stmCond.setTimestamp(3, Timestamp.valueOf(endTimeStamp.minusDays(1)));
                } else {
                    stmCond.setTimestamp(3, Timestamp.valueOf(item.getStartDate()));
                }

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
     * Получение паспортных данных объекта
     * @param muid объекта
     * @param objType тип объекта
     * @return коллекция паспортной информации
     */
    public List<PassportData> getPassportData(long muid, int objType) throws ConsumersException {
        try (Connection connect = ds.getConnection();
             PreparedStatement alterSession = connect.prepareStatement(ALTER_SESSION);
             PreparedStatement stm = connect.prepareStatement(SEL_PASSPORT_DATA)) {
            List<PassportData> result = new ArrayList<>();

            alterSession.executeQuery();

            stm.setLong(1, muid);
            stm.setInt(2, objType);

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
        try (OracleConnection connect = (OracleConnection) ds.getConnection();
             PreparedStatement stm = connect.prepareStatement(SQL_STATISTIC)) {
            Map<String, String> result = new LinkedHashMap<>();

            stm.setArray(1, connect.createOracleArray("NUM_TABLE", statisticBean.getMuidStatistic().toArray()));

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
     * Получение мгновенных данных по заданному объекту
     * @param muid заданный объект
     * @return список мгновенных данных
     */
    public List<DataModel> getInst(String muid) throws ConsumersException {
        List<DataModel> result = new ArrayList<>();
        String id;
        try (Connection connect = ds.getConnection();
             CallableStatement stm = connect.prepareCall("{? = call iasdtu.set_async_request(?)}")) {
            stm.setString(2, muid);
            stm.registerOutParameter(1, Types.VARCHAR);
            stm.execute();

            id = stm.getString(1);
        } catch(SQLException e) {
            logger.log(Level.WARNING, "error async request instant data", e);
            throw new ConsumersException("error async request instant data", e);
        }

        for (int i = 0; i < 10; i++) {
            logger.log(Level.INFO, "waiting for instant data {0}мс", String.valueOf(i * 6000));

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
            logger.log(Level.INFO, "instant data for muid {0} is loaded", muid);
        }

        return result;
    }

    /**
     * Метод которы выгружает данные из базы
     * @param muid идентификационный номер объекта
     * @param dataType тип запрашиваемых данных
     */
    private List<DataModel> getData(String muid, String dataType) throws ConsumersException {
        try (Connection connect = ds.getConnection();
             PreparedStatement stmAlter = connect.prepareStatement(ALTER_SESSION)) {
            stmAlter.execute();

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
