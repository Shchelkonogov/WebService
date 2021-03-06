package ru.tn.server.rest;

import ru.tn.server.bean.Bean;
import ru.tn.server.bean.BeanSchedule;
import ru.tn.server.model.CondDataModel;
import ru.tn.server.model.SubscriptModel;

import javax.ejb.EJB;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.ws.rs.*;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

/**
 * RestFull сервис
 */
@Path("/")
public class RestService {

    @EJB
    private Bean bean;

    @EJB
    private BeanSchedule schedule;

    /**
     * Метод подписки объектов пользователя
     * @param model модель данных подписки
     * @return статус подписки <br>
     *     202 - успешная подписка <br>
     *     500 - ошибка на сервере
     */
    @POST
    @Path("/subscription")
    @Consumes("application/json")
    public Response subscription(SubscriptModel model) {
        if(bean.subscript(model) && Objects.nonNull(schedule.getUrl())) {
            Client client = ClientBuilder.newClient();
            WebTarget target = client.target(schedule.getUrl() + "send");

            Response resp = target.queryParam("clientName", model.getControlId()).request().method("POST");

            System.out.println("RestService.subscription status: " + resp.getStatus());

            client.close();
            return Response.accepted().build();
        }
        return Response.serverError().build();
    }


    /**
     * Сервис проверки подписки
     * @param name пользователь
     * @return статус подписки
     */
    @POST
    @Path("/checkSub")
    public Response checkSub(@QueryParam("clientName") String name) {
        System.out.println("RestService.checkSub clientName: " + name);
        if(bean.checkSub(name)) {
            return Response.ok().build();
        } else {
            return Response.serverError().build();
        }
    }

    /**
     * Метод отсылает сосятояния подписаных объектов пользователя
     * @param name пользователь
     * @return статус отправки
     */
    @POST
    @Path("/send")
    public Response send(@QueryParam("clientName") String name) {
        System.out.println("RestService.send clientName: " + name);

        List<CondDataModel> model = bean.getData(name);

        if (!model.isEmpty()) {
            Jsonb jsonb = JsonbBuilder.create();

            System.out.println("RestService.send send MOEK: " + jsonb.toJson(model));
            System.out.println("RestService.send send URL: " + bean.getSendUrl(name));

            try {
                Client client = ClientBuilder.newClient();
                WebTarget target = client.target(bean.getSendUrl(name));
//                WebTarget target = client.target(schedule.getUrl() + "test");

                GenericEntity<List<CondDataModel>> modelWrapper = new GenericEntity<List<CondDataModel>>(model){};

                Response resp = target.request().post(Entity.entity(modelWrapper, MediaType.APPLICATION_JSON));

                System.out.println("RestService.send status: " + resp.getStatus());

                if(resp.getStatus() != 200) {
                    schedule.checkUser(name);
                } else {
                    bean.removeData(model);
                }
                client.close();
            } catch(Exception e) {
                System.out.println("RestService.send ERROR WHILE SEND");
                schedule.checkUser(name);
            }
        }

        return Response.ok().build();
    }

    @GET
    @Path("/statistic")
    @Produces("text/html")
    public String getStatistic() {
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

        bean.getStatistic().forEach((k, v) -> sb.append(k).append(": \t\t").append(v).append("<br>"));

        sb.append("</div>").append("</body>").append("</html>");
        return sb.toString();
    }

    /*@POST
    @Path("/send1")
    @Produces("application/json")
    public Response send1(@QueryParam("clientName") String name) {
        System.out.println("RestService.send clientName: " + name);

        List<CondDataModel> model = bean.getData(name);

        if (!model.isEmpty()) {

            try {


                GenericEntity<List<CondDataModel>> modelWrapper = new GenericEntity<List<CondDataModel>>(model){};

                //Response resp = target.request().post(Entity.entity(modelWrapper, MediaType.APPLICATION_JSON));
                return Response.ok().entity(modelWrapper).build();
            } catch(Exception e) {


            }
        }

        return Response.ok().build();
    }
*/
    /*@POST
    @Path("/test")
    @Consumes("application/json")
    public Response testSend(String model) {
        System.out.println("Catch result data!");
        System.out.println(model);
//        return Response.ok().build();
        return Response.serverError().build();
    }

    @GET
    public String test() {
        SubscriptModel model = new SubscriptModel();
        model.setControlId("IASDTU_6");
        model.setMuid(Arrays.asList(1746996630856007626L, 1746996789719662577L));

        Jsonb jsonb = JsonbBuilder.create();

        System.out.println("RestService.test send: " + jsonb.toJson(model));

        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(schedule.getUrl() + "subscription");

        Response resp = target.request().post(Entity.entity(model, MediaType.APPLICATION_JSON));

        System.out.println("RestService.test status: " + resp.getStatus());

        client.close();

//        Client client = ClientBuilder.newClient();
//
//        WebTarget target = client.target(schedule.getUrl() + "checkSub");
//        Response resp = target.queryParam("clientName", "IASDTU_6").request().method("POST");
//        System.out.println("RestService.test checkSub status: " + resp.getStatus());
//
//        client.close();

        return null;
    }*/

    /**
     * Метод инициализирует сервис
     * @param info метаданные
     * @return статус
     */
    @GET
    @Path("/run")
    public Response start(@Context UriInfo info) {
        schedule.setUrl(info.getBaseUri());
        return Response.ok().build();
    }
}
