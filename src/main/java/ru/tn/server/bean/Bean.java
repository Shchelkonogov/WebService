package ru.tn.server.bean;

import ru.tn.server.model.DataModel;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;

/**
 * Bean для получения данных из базы
 */
@Stateless
public class Bean {

    @Resource(name = "OracleDataSource", mappedName = "jdbc/OracleDataSource")
    private DataSource ds;

    private static final String SQL_STRING = "select n1,n2,n3,n4 from table (GET_data_by_muid(?))";
    private static final String SQL_STRING_INST = "select * from (" +
            "select (select par_memo from dz_param where id = a.par_id) as par_m, " +
            "'Мгновенные данные' as stat_agr, " +
            "to_char(a.time_stamp + 3/24,'dd-mm-yyyy hh24:mi') as time_stamp, " +
            "case when (is_num(a.par_value) = 0) and (a.par_id in (select id from dz_param where visible is not null)) then a.par_value " +
            "when a.par_id in (select id from dz_param where visible is not null) then " +
            "ltrim(rtrim(to_char(a.par_value, '999999999999999999990.99'))) " +
            "end as val " +
            "from arm_async_refresh_data a " +
            "where command_id = ? " +
            "and a.quality = 192 " +
            "order by (select visible from dz_param where id = a.par_id)) " +
            "where val is not null";

    /**
     * Получение исторических данных по заданному объекту
     * @param muid заданный объект
     * @return
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
                                res.getString(4)));
            }
        } catch(SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Получение мгновенных данных по заданному объекту
     * @param muid заданный объект
     * @return
     */
    public ArrayList<DataModel> getInst(String muid) {
        ArrayList<DataModel> result = new ArrayList<>();
        String id = null;
        try(Connection connect = ds.getConnection();
                CallableStatement stm = connect.prepareCall("{? = call get_async_data(?)}")) {
            stm.setString(2, muid);
            stm.registerOutParameter(1, Types.VARCHAR);
            stm.execute();

            id = stm.getString(1);
        } catch(SQLException e) {
            e.printStackTrace();
        }


        for (int i = 0; i < 10; i++) {
            System.out.println(i);

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
}
