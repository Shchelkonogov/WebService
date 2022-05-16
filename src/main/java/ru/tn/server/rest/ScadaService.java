package ru.tn.server.rest;

import ru.tn.server.bean.ScadaSB;
import ru.tn.server.entity.FittingsEntity;
import ru.tn.server.entity.TubesEntity;
import ru.tn.server.entity.util.FittingsSerializer;
import ru.tn.server.entity.util.TubesSerializer;

import javax.ejb.EJB;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbConfig;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Maksim Shchelkonogov
 */
@Path("/scada")
public class ScadaService {

    private static final Logger LOGGER = Logger.getLogger(ScadaService.class.getName());

    @EJB
    private ScadaSB scadaBean;

    @GET
    @Path("/tubeByBrand")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTubeByBrand(@QueryParam("brand") String brand) {
        LOGGER.log(Level.INFO, "get tube by brand {0}", brand);

        List<TubesEntity> tubesByBrand = scadaBean.getTubesByBrand(brand);
        if (!tubesByBrand.isEmpty()) {
            LOGGER.log(Level.INFO, "find tubes {0}", tubesByBrand);

            JsonbConfig config = new JsonbConfig()
                    .withFormatting(true)
                    .withSerializers(new TubesSerializer());
            Jsonb jsonb = JsonbBuilder.create(config);

            return Response.ok(jsonb.toJson(tubesByBrand, new ArrayList<TubesEntity>(){}.getClass().getGenericSuperclass())).build();
        }

        LOGGER.log(Level.INFO, "no tubes find for brand {0}", brand);
        return Response.status(Response.Status.NO_CONTENT).build();
    }

    @GET
    @Path("/fittingByBrand")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getFittingByBrand(@QueryParam("brand") String brand) {
        LOGGER.log(Level.INFO, "get fitting by brand {0}", brand);

        List<FittingsEntity> fittingsByBrand = scadaBean.getFittingsByBrand(brand);
        if (!fittingsByBrand.isEmpty()) {
            LOGGER.log(Level.INFO, "find fitting {0}", fittingsByBrand);

            JsonbConfig config = new JsonbConfig()
                    .withFormatting(true)
                    .withSerializers(new FittingsSerializer());
            Jsonb jsonb = JsonbBuilder.create(config);

            return Response.ok(jsonb.toJson(fittingsByBrand, new ArrayList<FittingsEntity>(){}.getClass().getGenericSuperclass())).build();
        }

        LOGGER.log(Level.INFO, "no fitting find for brand {0}", brand);
        return Response.status(Response.Status.NO_CONTENT).build();
    }

    @GET
    @Path("/entity")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getEntity(@QueryParam("muid") String muid) {
        LOGGER.log(Level.INFO, "get entity {0}", muid);

        Jsonb jsonb = JsonbBuilder.create();

        TubesEntity tube = scadaBean.getTubeByMuid(muid);
        if (tube != null) {
            LOGGER.log(Level.INFO, "find tube {0}", tube);
            return Response.ok(jsonb.toJson(tube)).build();
        }

        FittingsEntity fitting = scadaBean.getFittingByMuid(muid);
        if (fitting != null) {
            LOGGER.log(Level.INFO, "find fitting {0}", fitting);
            return Response.ok(jsonb.toJson(fitting)).build();
        }

        LOGGER.log(Level.INFO, "no entity find for muid: {0}", muid);
        return Response.status(Response.Status.NO_CONTENT).build();
    }

    @POST
    @Path("/tube")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response setTube(@QueryParam("muid") String muid, TubesEntity tube, @Context SecurityContext securityContext) {
        if (!securityContext.isUserInRole("USER")) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        boolean result;

        if (muid != null) {
            LOGGER.log(Level.INFO, "update tube {0} new data {1}", new Object[] {muid, tube});
            result = scadaBean.updateTube(muid, tube);
        } else {
            LOGGER.log(Level.INFO, "create tube {0}", tube);
            result = scadaBean.addTube(tube);
        }

        return result ?
                Response.status(Response.Status.CREATED).entity(tube.getMuid()).build() :
                Response.status(Response.Status.NO_CONTENT).build();
    }

    @POST
    @Path("/fitting")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response setFitting(@QueryParam("muid") String muid, FittingsEntity fitting, @Context SecurityContext securityContext) {
        if (!securityContext.isUserInRole("USER")) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        boolean result;

        if (muid != null) {
            LOGGER.log(Level.INFO, "update fitting {0} new data {1}", new Object[] {muid, fitting});
            result = scadaBean.updateFitting(muid, fitting);
        } else {
            LOGGER.log(Level.INFO, "create fitting {0}", fitting);
            result = scadaBean.addFitting(fitting);
        }
        return result ?
                Response.status(Response.Status.CREATED).entity(fitting.getMuid()).build() :
                Response.status(Response.Status.NO_CONTENT).build();
    }

    @DELETE
    @Path("/entity")
    public Response removeEntity(@QueryParam("muid") String muid, @Context SecurityContext securityContext) {
        if (!securityContext.isUserInRole("USER")) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        LOGGER.log(Level.INFO, "remove entity {0}", muid);
        return (scadaBean.deleteTube(muid) || scadaBean.deleteFitting(muid)) ?
                Response.status(Response.Status.OK).build() : Response.status(Response.Status.NO_CONTENT).build();
    }
}
