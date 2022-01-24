package ru.tn.server;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

/**
 * @author Maksim Shchelkonogov
 */
public class SendHttpRequest {

    public static void main(String[] args) throws NoSuchAlgorithmException, KeyManagementException {
        // Настройка cookie

        // Thread безопастный вариант 1
//        ThreadLocalCookieStore store = new ThreadLocalCookieStore();
//        CookieHandler.setDefault(new CookieManager(store, CookiePolicy.ACCEPT_ALL));

        // Thread безопастный вариант 2
//        CookieHandler.setDefault(SessionCookieManager.getInstance());

        // Обычный
        CookieManager cookieManager = new CookieManager();
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
        CookieHandler.setDefault(cookieManager);


        // Игнорирование проверки SSL
        TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager(){
            public X509Certificate[] getAcceptedIssuers(){return null;}
            public void checkClientTrusted(X509Certificate[] certs, String authType){}
            public void checkServerTrusted(X509Certificate[] certs, String authType){}
        }};

        HostnameVerifier hostnameVerifier = (arg0, arg1) -> true;

        SSLContext sslContext = SSLContext.getInstance("TLS");

        sslContext.init(null, trustAllCerts, new SecureRandom());


        Client client = ClientBuilder.newBuilder().hostnameVerifier(hostnameVerifier).sslContext(sslContext).build();
        WebTarget target = client.target("https://localhost:7002/IASDTU/view/j_security_check");

        Form form = new Form();
        form.param("j_username", "Tecon");
        form.param("j_password", "123456789");

        Response response = target.request(MediaType.APPLICATION_FORM_URLENCODED).accept(MediaType.TEXT_HTML)
                .post(Entity.form(form));

        System.out.println(response.getStatus());

        // Вывод cookie
        // Thread безопастный вариант 1
//        System.out.println(store.getStore().getCookies());

        // Thread безопастный вариант 2
//        System.out.println(SessionCookieManager.getInstance().getCookieStore().getCookies());

        // Обычный
        System.out.println(cookieManager.getCookieStore().getCookies());

    }


}
