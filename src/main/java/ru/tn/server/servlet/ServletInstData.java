package ru.tn.server.servlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import ru.tn.server.bean.Bean;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Сервлет который возвращает мгновенные данные для объекта
 */
@WebServlet(name = "ServletInstData", urlPatterns = "/Get_AsyncRefresh_Data")
public class ServletInstData extends HttpServlet {

    @Inject
    private Bean bean;

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String value = request.getParameter("muid");

        bean.addMuidToStatistic(value);

        ObjectMapper mapper = new ObjectMapper();

        response.setContentType("text/html; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(mapper.writeValueAsString(bean.getInst(value)));
    }
}
