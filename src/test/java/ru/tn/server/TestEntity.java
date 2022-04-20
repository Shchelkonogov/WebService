package ru.tn.server;

import ru.tn.server.entity.FittingsEntity;
import ru.tn.server.entity.TubesEntity;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * @author Maksim Shchelkonogov
 */
public class TestEntity {

    public static void main(String[] args) {
        EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory("PostgreDBLocal");
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        EntityTransaction transaction = entityManager.getTransaction();

        try {
            transaction.begin();

            TubesEntity tubes = new TubesEntity();
            tubes.setBrand("1");
            tubes.setClientId("2");
            tubes.setStatus(5L);
            tubes.setTimeStamp(LocalDateTime.now());

            entityManager.persist(tubes);

            TypedQuery<TubesEntity> tubeByBrandQuery = entityManager.createNamedQuery("TubesEntity.byBrand", TubesEntity.class);
            tubeByBrandQuery.setParameter(1, "1");

            System.out.println(tubeByBrandQuery.getResultList());
            System.out.println(entityManager.find(TubesEntity.class, "1dlWlqP2UvngVbeL9p1YoQ=="));
            System.out.println(entityManager.find(FittingsEntity.class, "1dlWlpGAUvngVbeL9p1YoQ=="));

            transaction.commit();
        } finally {
            if (transaction.isActive()) {
                transaction.rollback();
            }

            entityManager.close();
            entityManagerFactory.close();
        }
    }
}
