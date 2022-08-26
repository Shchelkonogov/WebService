package ru.tn.server.util;

import java.sql.SQLException;

/**
 * @author Maksim Shchelkonogov
 */
public class ConsumersException extends SQLException {

    public ConsumersException(String reason) {
        super(reason);
    }

    public ConsumersException(String reason, Throwable cause) {
        super(reason, cause);
    }
}
