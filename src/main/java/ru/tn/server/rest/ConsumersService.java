package ru.tn.server.rest;

import com.google.gson.Gson;
import ru.tn.server.ejb.ConsumersSB;
import ru.tn.server.ejb.StatisticSB;
import ru.tn.server.model.DataModel;
import ru.tn.server.model.PassportData;
import ru.tn.server.model.SubscriptModel;
import ru.tn.server.util.ConsumersException;
import ru.tn.server.util.Json;

import javax.ejb.EJB;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Maksim Shchelkonogov
 */
@Path("/")
public class ConsumersService {

    private static final List<Integer> OBJECT_TYPE_LIST = Arrays.asList(1, 303, 343, 283);

    @Inject
    private Logger logger;

    @Inject
    @Json
    private Jsonb jsonb;

    @Inject
    @Json(withNull = true)
    private Gson jsonWithNUll;

    @EJB
    private ConsumersSB consumersBean;

    @EJB
    private StatisticSB statisticBean;

    @GET
    @Path("/echo")
    public String echo(@QueryParam("q") String original) {
        return original;
    }

    /**
     * Метод подписки объектов пользователя
     * @param model модель данных подписки
     * @return статус подписки <br>
     *     202 - успешная подписка <br>
     *     400 - не известный клиент <br>
     *     500 - ошибка на сервере
     */
    @POST
    @Path("/subscription")
    @Consumes("application/json")
    public Response subscription(SubscriptModel model) {
        try {
            logger.log(Level.INFO, "request subscript model {0}", model);

            consumersBean.subscript(model);
            return Response.ok().build();
        } catch (ConsumersException e) {
            if (e.getMessage().contains("unknown client")) {
                return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
            }
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
    }

    /**
     * Get метод для получения паспортной информации об объекте
     * @param muid объекта
     * @param objType тип объекта
     * @return http ответ ввиде json
     */
    @GET
    @Path("/getPassportData")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPassportData(@QueryParam("muid") long muid, @QueryParam("objType") int objType) {
        logger.log(Level.INFO, "request passport data muid {0} objType {1}", new Object[]{String.valueOf(muid), objType});

        if (!OBJECT_TYPE_LIST.contains(objType)) {
            return Response.status(Response.Status.BAD_REQUEST).entity("unknown object type").build();
        }

        try {
            List<PassportData> passportData = consumersBean.getPassportData(muid, objType);
            if (passportData.isEmpty()) {
                return Response.status(Response.Status.NO_CONTENT).build();
            } else {
                return Response.ok(jsonWithNUll.toJson(passportData)).build();
            }
        } catch (ConsumersException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
    }

    @GET
    @Path("/getHistData")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getHistData(@QueryParam("muid") long muid) {
        logger.log(Level.INFO, "request hist data muid {0}", String.valueOf(muid));

        statisticBean.addMuidToStatistic(muid);

        try {
            List<DataModel> histData = consumersBean.getHistData(String.valueOf(muid));
            if (histData.isEmpty()) {
                return Response.status(Response.Status.NO_CONTENT).build();
            } else {
                return Response.ok(jsonb.toJson(histData)).build();
            }
        } catch (ConsumersException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
    }

    @GET
    @Path("/getAsyncRefreshData")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getInstantData(@QueryParam("muid") long muid) {
        logger.log(Level.INFO, "request instant data muid {0}", String.valueOf(muid));

        statisticBean.addMuidToStatistic(muid);

        try {
            List<DataModel> instData = consumersBean.getInst(String.valueOf(muid));
            if (instData.isEmpty()) {
                return Response.status(Response.Status.NO_CONTENT).build();
            } else {
                return Response.ok(jsonb.toJson(instData)).build();
            }
        } catch (ConsumersException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
    }

    /**
     * Сервис проверки подписки
     * @param clientName пользователь
     * @return статус подписки
     */
    @GET
    @Path("/getSubs")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSubs(@QueryParam("clientName") String clientName) {
        logger.log(Level.INFO, "request get subscription muids for client name {0}", clientName);

        try {
            List<String> subs = consumersBean.getSubs(clientName);
            if (subs.isEmpty()) {
                return Response.status(Response.Status.NO_CONTENT).build();
            } else {
                return Response.ok(jsonb.toJson(subs)).build();
            }
        } catch (ConsumersException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
    }

    @GET
    @Path("/statistic")
    @Produces(MediaType.TEXT_HTML)
    public Response getStatistic() {
        LocalDateTime time = LocalDateTime.now();
        StringBuilder sb = new StringBuilder("<html>")
                .append("<head>")
                .append("<meta charset=\"utf-8\">")
                .append("</head>")
                .append("<body>")
                .append("<div style=\"text-align: center; font-weight: bold;\">")
                .append("Статистика работы сервиса экранов коллективного доступа")
                .append("</div>")
                .append("<div style=\"text-align: center;\">")
                .append(" c ")
                .append(time.withMinute(0).withSecond(0).format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")))
                .append(" по ")
                .append(time.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")))
                .append("</div>")
                .append("<br>")
                .append("<div style=\"text-align: center;\">");

        try {
            consumersBean.getStatistic().forEach((k, v) -> sb.append(k).append(": \t\t").append(v).append("<br>"));
        } catch (ConsumersException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }

        sb.append("</div>").append("</body>").append("</html>");
        return Response.ok().entity(sb.toString()).build();
    }
}
