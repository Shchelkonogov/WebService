package ru.tn.server.entity.util;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;

import java.io.Serializable;

/**
 * Генератор для создания id в базе, для entity
 * @author Maksim Shchelkonogov
 */
public class MyGenerator implements IdentifierGenerator {

    @Override
    public Serializable generate(SharedSessionContractImplementor session, Object obj) throws HibernateException {
        return session.createNativeQuery("select GET_GUID_BASE64() from DUAL")
                .getSingleResult()
                .toString();
    }
}
