package ru.tn.server.bean;

import ru.tn.server.model.CondDataModel;
import ru.tn.server.model.DataModel;
import ru.tn.server.model.SubscriptModel;

import javax.annotation.Resource;
import javax.ejb.*;
import javax.sql.DataSource;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

/**
 * Bean для получения данных из базы
 */
@Stateless
public class Bean {

    private static Set<String> muidStatistic = ConcurrentHashMap.newKeySet();

    @EJB
    private BeanSchedule schBean;

    @Resource(name = "OracleDataSource", mappedName = "jdbc/OracleDataSource")
    private DataSource ds;

    private static final String SQL_STRING = "select n1, n2, n3, n4, n5 from table (iasdtu.get_hist_data(?))";
    private static final String SQL_STRING_INST = "select n1, n2, n3, n4, n5 from table (iasdtu.get_async_data(?))";

    private static final String SQL_CHECK_CLIENT = "select client_id from iasdtu_clients where client_name = ?";
    private static final String SQL_INSERT_OBJ = "insert into iasdtu_subscr(client_id, muid) values (?, ?)";
    private static final String SQL_DELETE_OBJ = "delete from iasdtu_subscr where client_id = ?";
    private static final String SQL_DELETE_DATA = "delete from iasdtu_data where muid = ?";
    private static final String SQL_PROC_CALL_DATA = "{call iasdtu.send_obj_state(?)}";

    private static final String SQL_SELECT_MUID = "select muid from iasdtu_subscr where client_id = (select client_id " +
            "from iasdtu_clients where client_name = ?)";
    private static final String SQL_SELECT_MAX_TIME_STAMP = "select to_char(max(time_stamp), 'dd.mm.yyyy hh24:mi:ss') " +
            "from iasdtu_data where muid = ?";
    private static final String SQL_SELECT_COND = "select cond from iasdtu_data where muid = ? " +
            "and time_stamp <= to_date(?, 'dd.mm.yyyy hh24:mi:ss') order by time_stamp, id";

    private static final String SQL_DELETE_COND = "delete from iasdtu_data where time_stamp <= to_date(?, 'dd.mm.yyyy hh24:mi:ss') " +
            "and muid = ?";
    private static final String SQL_GET_URL = "select client_path from iasdtu_clients where client_name = ?";
    private static final String SQL_GET_USER = "select client_name from iasdtu_clients";
    private static final String SQL_DELETE_SUBS = "delete from iasdtu_subscr " +
            "where client_id = (select client_id from iasdtu_clients where client_name = ?)";
    private static final String SQL_CHECK_SUB = "select count(*) from IASDTU_SUBSCR " +
            "where client_id = (select client_id from IASDTU_CLIENTS where client_name = ?)";

    private static final String SQL_STATISTIC = "select get_obj_filial(obj_id), count(*) from obj_object where obj_id in ( " +
            "select id_aspid from gis_object where id_gis in (?)) " +
            "group by get_obj_filial(obj_id) " +
            "order by to_number(replace(get_obj_filial(obj_id),'Филиал ',''))";

    /**
     * Получение исторических данных по заданному объекту
     * @param muid заданный объект
     * @return коллекция архивных данных
     */
    public ArrayList<DataModel> getHist(String muid) {
        ArrayList<DataModel> result = new ArrayList<>();
        getData(muid, result, SQL_STRING);
        return result;
    }

    /**
     * Метод которы выгружает данные из базы
     * @param muid идентификационный номер объекта
     * @param result коллекция результатов
     * @param sqlString sql строка запроса
     */
    private void getData(String muid, ArrayList<DataModel> result, String sqlString) {
        try(Connection connect = ds.getConnection();
            PreparedStatement pst = connect.prepareStatement(sqlString)) {
            pst.setString(1, muid);
            ResultSet res = pst.executeQuery();
            while(res.next()) {
                result.add(new DataModel(res.getString(1),
                                res.getString(2), res.getString(3),
                                res.getString(4), res.getString(5)));
            }
            System.out.println("Bean.getData data ready");
        } catch(SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Получение мгновенных данных по заданному объекту
     * @param muid заданный объект
     * @return список мгновенных данных
     */
    public ArrayList<DataModel> getInst(String muid) {
        ArrayList<DataModel> result = new ArrayList<>();
        String id = null;
        try(Connection connect = ds.getConnection();
                CallableStatement stm = connect.prepareCall("{? = call iasdtu.set_async_request(?)}")) {
            stm.setString(2, muid);
            stm.registerOutParameter(1, Types.VARCHAR);
            stm.execute();

            id = stm.getString(1);
        } catch(SQLException e) {
            e.printStackTrace();
        }

        for (int i = 0; i < 10; i++) {
            System.out.println("Bean.getInst wait data: " + (i * 6000) + "мс");

            try {
                Thread.sleep(6000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            getData(id, result, SQL_STRING_INST);

            if (!result.isEmpty()) {
                break;
            }
        }
        return result;
    }

    /**
     * Метод производящий подписку объектов пользователя
     * Если пользователь существует, то удаляются все объекты
     * пользователя и вставляются новые. Затем стирается вся
     * информация о состоянии этого объекта и запускается
     * функция определяющаяя состояние объекта на данный момент
     * @param client клиент с его объектами
     * @return true - подписка объектов удалась <br>
     *     false - подписка не удачна
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean subscript(SubscriptModel client) {
        try(Connection connect = ds.getConnection();
                PreparedStatement stm = connect.prepareStatement(SQL_CHECK_CLIENT);
                PreparedStatement stmDeleteObj = connect.prepareStatement(SQL_DELETE_OBJ);
                PreparedStatement stmInsert = connect.prepareStatement(SQL_INSERT_OBJ);
                PreparedStatement stmDeleteData = connect.prepareStatement(SQL_DELETE_DATA);
                PreparedStatement stmProcCall = connect.prepareCall(SQL_PROC_CALL_DATA)) {
            stm.setString(1, client.getControlId());

            ResultSet res = stm.executeQuery();
            if(res.next()) {
                stmDeleteObj.setString(1, res.getString(1));
                stmDeleteObj.executeUpdate();

                for (Long item: client.getMuid()) {
                    stmInsert.setString(1, res.getString(1));
                    stmInsert.setLong(2, item);
                    stmInsert.addBatch();

                    stmDeleteData.setLong(1, item);
                    stmDeleteData.addBatch();

                    stmProcCall.setLong(1, item);
                    stmProcCall.addBatch();
                }

                stmInsert.executeBatch();
                System.out.println("Bean.subscript inserted");
                stmDeleteData.executeBatch();
                System.out.println("Bean.subscript delete");
                stmProcCall.executeBatch();
                System.out.println("Bean.subscript procedure call");
            } else {
                return false;
            }
        } catch(SQLException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * Метод возвращает коллекция с состояния объектов пользователя
     * @param clientName пользователь
     * @return коллекция данных
     */
    public List<CondDataModel> getData(String clientName) {
        List<CondDataModel> result = new ArrayList<>();
        try(Connection connect = ds.getConnection();
                PreparedStatement stm = connect.prepareStatement(SQL_SELECT_MUID);
                PreparedStatement stmMaxTimeStamp = connect.prepareStatement(SQL_SELECT_MAX_TIME_STAMP);
                PreparedStatement stmCond = connect.prepareStatement(SQL_SELECT_COND)) {
            List<Long> muid = new ArrayList<>();
            stm.setString(1, clientName);

            ResultSet res = stm.executeQuery();
            while(res.next()) {
                muid.add(res.getLong(1));
            }

            List<Integer> cond;
            for (Long item: muid) {
                String timeStamp = null;
                stmMaxTimeStamp.setLong(1, item);

                res = stmMaxTimeStamp.executeQuery();
                if(res.next()) {
                    timeStamp = res.getString(1);
                }

                cond = new ArrayList<>();
                stmCond.setLong(1, item);
                stmCond.setString(2, timeStamp);

                res = stmCond.executeQuery();
                while(res.next()) {
                    cond.add(res.getInt(1));
                }

                if (!cond.isEmpty()) {
                    result.add(new CondDataModel(item, cond, timeStamp));
                }
            }
        } catch(SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * Метод стирает состояния объектов из базы
     * Надо вызывать в случае успешной отправки данных сервисом
     * @param list данные
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void removeData(List<CondDataModel> list) {
        try(Connection connect = ds.getConnection();
                PreparedStatement stm = connect.prepareStatement(SQL_DELETE_COND)) {
            for (CondDataModel item: list) {
                stm.setString(1, item.getMaxTimeStamp());
                stm.setLong(2, item.getMuid());

                stm.addBatch();
            }

            stm.executeBatch();
            System.out.println("Bean.removeData delete");
        } catch(SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Метод возвращет строку с url по которому надо отсылать данные
     * @param name клиент
     * @return url строка
     */
    public String getSendUrl(String name) {
        try(Connection connect = ds.getConnection();
                PreparedStatement stm = connect.prepareStatement(SQL_GET_URL)) {
            stm.setString(1, name);

            ResultSet res = stm.executeQuery();
            if(res.next()) {
                return res.getString(1);
            }
        } catch(SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Метод удаляет подписанные объекты пользователя по имени клиента
     * @param client клиент
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void removeSubs(String client) {
        try(Connection connect = ds.getConnection();
            PreparedStatement stm = connect.prepareStatement(SQL_DELETE_SUBS)) {
            stm.setString(1, client);
            stm.executeQuery();
            System.out.println("Bean.removeSubs remove client subs: " + client);
        } catch(SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Метод проверяет существуют ли подписанные объекту у пользователя
     * @param clientName пользователь
     * @return статус
     */
    public boolean checkSub(String clientName) {
        System.out.println("Bean.checkSub client: " + clientName);
        try(Connection connect = ds.getConnection();
            PreparedStatement stm = connect.prepareStatement(SQL_CHECK_SUB)) {
            stm.setString(1, clientName);

            ResultSet res = stm.executeQuery();
            if(res.next()) {
                if(res.getInt(1) != 0) {
                    System.out.println("Bean.checkSub ok sub");
                    return true;
                }
            }
        } catch(SQLException e) {
            e.printStackTrace();
        }
        System.out.println("Bean.checkSub no sub");
        return false;
    }

    /**
     * Метод для ужеминутной отправки данных
     * Асинхронный что бы не бокировать Schedule bean
     */
    @Asynchronous
    public Future<Void> send() {
        List<String> clients = new ArrayList<>();
        try(Connection connect = ds.getConnection();
            PreparedStatement stm = connect.prepareStatement(SQL_GET_USER)) {
            ResultSet res = stm.executeQuery();
            while(res.next()) {
                clients.add(res.getString(1));
            }
        } catch(SQLException e) {
            e.printStackTrace();
        }
        for (String item: clients) {
            System.out.println("Bean.send client: " + item);
            Client client = ClientBuilder.newClient();
            WebTarget target = client.target(schBean.getUrl() + "send");

            Response resp = target.queryParam("clientName", item).request().method("POST");

            System.out.println("Bean.send status: " + resp.getStatus());

            client.close();
        }
        return null;
    }

    /**
     * Добавляем muid в статистику
     * @param muid asot muid
     */
    public void addMuidToStatistic(String muid) {
        muidStatistic.add(muid);
    }

    /**
     * Метод очищает статистику
     */
    public void clearStatistic() {
        muidStatistic.clear();
    }

    public Map<String, String> getStatistic() {
        Map<String, String> result = new LinkedHashMap<>();

        String muids = muidStatistic.stream().map(s -> "'" + s + "'").collect(Collectors.joining(", "));

        try (Connection connect = ds.getConnection();
             PreparedStatement stm = connect.prepareStatement(SQL_STATISTIC.replace("?", muids))) {
            ResultSet res = stm.executeQuery();

            while (res.next()) {
                result.put(res.getString(1), res.getString(2));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return result;
    }
}
