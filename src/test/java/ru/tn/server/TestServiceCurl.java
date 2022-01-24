package ru.tn.server;

import java.io.IOException;

/**
 * @author Maksim Shchelkonogov
 */
public class TestServiceCurl {

    public static void main(String[] args) throws IOException {
        System.out.println("run remote command \"curl\"");
        Runtime.getRuntime().exec("curl -k -b cookies.txt -c cookies.txt -d \"j_username=AMaxTest&j_password=12345678\" https://localhost:7002/IASDTU/view/j_security_check");
    }
}
