package com.tollsystem.repository;

import com.tollsystem.entity.VehiclePassEntity;
import com.tollsystem.model.Transaction;

import javax.persistence.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository providing both raw JDBC operations (for summary reporting) and
 * JPA/Hibernate CRUD operations (for transactional record management).
 */
public class DatabaseManager {

    public static final String JDBC_DRIVER = "org.h2.Driver";
    public static final String JDBC_URL    = "jdbc:h2:mem:tolldb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE";
    public static final String JDBC_USER   = "sa";
    public static final String JDBC_PASS   = "";

    public static final String PERSISTENCE_UNIT = "TollSystemPU";

    private EntityManagerFactory emf;

    public DatabaseManager() {
        try {
            emf = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT);
            System.out.println("[DatabaseManager] JPA EntityManagerFactory initialised successfully.");
            createSchemaViaJdbc();
        } catch (Exception e) {
            System.err.println("[DatabaseManager] WARNING: Could not initialise JPA EntityManagerFactory. "
                    + "JPA operations will be unavailable. Cause: " + e.getMessage());
            emf = null;
        }
    }

    public boolean testJdbcConnection() {
        System.out.println("[JDBC] Testing connection to: " + JDBC_URL);
        try {
            Class.forName(JDBC_DRIVER);
        } catch (ClassNotFoundException e) {
            System.err.println("[JDBC] H2 JDBC driver not found on classpath: " + e.getMessage());
            return false;
        }

        try (Connection conn = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASS)) {
            if (conn != null && !conn.isClosed()) {
                System.out.println("[JDBC] Connection successful! Product: "
                        + conn.getMetaData().getDatabaseProductName()
                        + " v" + conn.getMetaData().getDatabaseProductVersion());
                return true;
            }
        } catch (SQLException e) {
            System.err.println("[JDBC] Connection failed: " + e.getMessage());
        }
        return false;
    }

    private void createSchemaViaJdbc() {
        String ddl = "CREATE TABLE IF NOT EXISTS VEHICLE_PASS (" +
                "PASS_ID            BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                "REGISTRATION_NUM   VARCHAR(20)  NOT NULL, " +
                "VEHICLE_TYPE       VARCHAR(10)  NOT NULL, " +
                "TOLL_AMOUNT        DOUBLE       NOT NULL, " +
                "REMAINING_BALANCE  DOUBLE       NOT NULL, " +
                "LANE_NUMBER        INT          NOT NULL, " +
                "PASS_TIMESTAMP     TIMESTAMP    NOT NULL, " +
                "STATUS             VARCHAR(10)  NOT NULL, " +
                "STATUS_MESSAGE     VARCHAR(500), " +
                "FASTAG_ENABLED     BOOLEAN      NOT NULL)";

        try {
            Class.forName(JDBC_DRIVER);
        } catch (ClassNotFoundException e) {
            System.err.println("[JDBC] H2 driver not on classpath, skipping DDL.");
            return;
        }

        try (Connection conn = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASS);
             Statement stmt = conn.createStatement()) {
            stmt.execute(ddl);
            System.out.println("[JDBC] Schema verified / created: VEHICLE_PASS table ready.");
        } catch (SQLException e) {
            System.err.println("[JDBC] DDL execution failed: " + e.getMessage());
        }
    }

    public void printRevenueSummaryViaJdbc() {
        String sql = "SELECT VEHICLE_TYPE, COUNT(*) AS TOTAL_PASSES, " +
                     "SUM(TOLL_AMOUNT) AS TOTAL_REVENUE, " +
                     "AVG(TOLL_AMOUNT) AS AVG_TOLL " +
                     "FROM VEHICLE_PASS WHERE STATUS = 'SUCCESS' " +
                     "GROUP BY VEHICLE_TYPE ORDER BY TOTAL_REVENUE DESC";

        System.out.println("\n--- JDBC Revenue Summary Report ---");
        try {
            Class.forName(JDBC_DRIVER);
        } catch (ClassNotFoundException e) {
            System.err.println("[JDBC] Driver not found: " + e.getMessage());
            return;
        }

        try (Connection conn = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASS);
             Statement stmt = conn.createStatement();
             ResultSet rs   = stmt.executeQuery(sql)) {

            System.out.printf("%-12s %-14s %-18s %-12s%n",
                    "VehicleType", "Total Passes", "Total Revenue", "Avg Toll");
            System.out.println("----------------------------------------------------------");

            boolean hasRows = false;
            while (rs.next()) {
                hasRows = true;
                System.out.printf("%-12s %-14d INR %-14.2f INR %-10.2f%n",
                        rs.getString("VEHICLE_TYPE"),
                        rs.getInt("TOTAL_PASSES"),
                        rs.getDouble("TOTAL_REVENUE"),
                        rs.getDouble("AVG_TOLL"));
            }
            if (!hasRows) {
                System.out.println("  [No records found -- run toll processing first]");
            }
        } catch (SQLException e) {
            System.err.println("[JDBC] Query failed: " + e.getMessage());
        }
    }

    public VehiclePassEntity insertPassRecord(Transaction transaction) {
        if (emf == null) {
            System.err.println("[JPA] EntityManagerFactory not available.");
            return null;
        }

        EntityManager em = emf.createEntityManager();
        EntityTransaction et = em.getTransaction();
        VehiclePassEntity entity = null;

        try {
            et.begin();

            entity = new VehiclePassEntity(
                    transaction.getRegistrationNum(),
                    transaction.getVehicleType().name(),
                    transaction.getTollAmount(),
                    transaction.getRemainingBalance(),
                    transaction.getLaneNumber(),
                    transaction.getTimestamp(),
                    transaction.isSuccessful() ? "SUCCESS" : "FAILED",
                    transaction.getStatusMessage(),
                    transaction.isFastagEnabled());

            em.persist(entity);
            et.commit();

            System.out.printf("[JPA] Persisted VehiclePassEntity -- PassID: %d | Vehicle: %s%n",
                    entity.getPassId(), entity.getRegistrationNum());

        } catch (Exception e) {
            if (et.isActive()) {
                et.rollback();
            }
            System.err.println("[JPA] Failed to persist pass record: " + e.getMessage());
        } finally {
            em.close();
        }

        return entity;
    }

    public int bulkInsertTransactions(List<Transaction> transactions) {
        if (emf == null) {
            System.err.println("[JPA] EntityManagerFactory not available.");
            return 0;
        }

        int successCount = 0;
        EntityManager em = emf.createEntityManager();
        EntityTransaction et = em.getTransaction();

        try {
            et.begin();
            for (Transaction txn : transactions) {
                VehiclePassEntity entity = new VehiclePassEntity(
                        txn.getRegistrationNum(),
                        txn.getVehicleType().name(),
                        txn.getTollAmount(),
                        txn.getRemainingBalance(),
                        txn.getLaneNumber(),
                        txn.getTimestamp(),
                        txn.isSuccessful() ? "SUCCESS" : "FAILED",
                        txn.getStatusMessage(),
                        txn.isFastagEnabled());
                em.persist(entity);
                successCount++;
            }
            et.commit();
            System.out.printf("[JPA] Bulk insert complete: %d record(s) persisted.%n", successCount);
        } catch (Exception e) {
            if (et.isActive()) {
                et.rollback();
            }
            System.err.println("[JPA] Bulk insert failed: " + e.getMessage());
        } finally {
            em.close();
        }
        return successCount;
    }

    public List<VehiclePassEntity> fetchAllPassRecords() {
        List<VehiclePassEntity> results = new ArrayList<>();
        if (emf == null) {
            System.err.println("[JPA] EntityManagerFactory not available.");
            return results;
        }

        EntityManager em = emf.createEntityManager();
        try {
            TypedQuery<VehiclePassEntity> query = em.createQuery(
                    "SELECT vp FROM VehiclePassEntity vp ORDER BY vp.passTimestamp DESC",
                    VehiclePassEntity.class);
            results = query.getResultList();
        } catch (Exception e) {
            System.err.println("[JPA] JPQL fetch failed: " + e.getMessage());
        } finally {
            em.close();
        }
        return results;
    }

    public List<VehiclePassEntity> fetchPassesByVehicle(String registrationNum) {
        List<VehiclePassEntity> results = new ArrayList<>();
        if (emf == null) {
            return results;
        }

        EntityManager em = emf.createEntityManager();
        try {
            TypedQuery<VehiclePassEntity> query = em.createQuery(
                    "SELECT vp FROM VehiclePassEntity vp " +
                    "WHERE vp.registrationNum = :regNum " +
                    "ORDER BY vp.passTimestamp DESC",
                    VehiclePassEntity.class);
            query.setParameter("regNum", registrationNum);
            results = query.getResultList();
        } catch (Exception e) {
            System.err.println("[JPA] JPQL fetch by vehicle failed: " + e.getMessage());
        } finally {
            em.close();
        }
        return results;
    }

    public boolean updateRemainingBalance(Long passId, double newRemainingBalance) {
        if (emf == null) {
            return false;
        }

        EntityManager em = emf.createEntityManager();
        EntityTransaction et = em.getTransaction();
        boolean success = false;

        try {
            et.begin();
            VehiclePassEntity entity = em.find(VehiclePassEntity.class, passId);
            if (entity != null) {
                entity.setRemainingBalance(newRemainingBalance);
                em.merge(entity);
                et.commit();
                success = true;
                System.out.printf("[JPA] Updated PassID %d -- new balance: INR %.2f%n",
                        passId, newRemainingBalance);
            } else {
                System.err.printf("[JPA] PassID %d not found for update.%n", passId);
                et.rollback();
            }
        } catch (Exception e) {
            if (et.isActive()) {
                et.rollback();
            }
            System.err.println("[JPA] Update failed: " + e.getMessage());
        } finally {
            em.close();
        }
        return success;
    }

    public boolean deletePassRecord(Long passId) {
        if (emf == null) {
            return false;
        }

        EntityManager em = emf.createEntityManager();
        EntityTransaction et = em.getTransaction();
        boolean success = false;

        try {
            et.begin();
            VehiclePassEntity entity = em.find(VehiclePassEntity.class, passId);
            if (entity != null) {
                em.remove(entity);
                et.commit();
                success = true;
                System.out.printf("[JPA] Deleted PassID %d from database.%n", passId);
            } else {
                System.err.printf("[JPA] PassID %d not found for deletion.%n", passId);
                et.rollback();
            }
        } catch (Exception e) {
            if (et.isActive()) {
                et.rollback();
            }
            System.err.println("[JPA] Delete failed: " + e.getMessage());
        } finally {
            em.close();
        }
        return success;
    }

    public void displayAllPassRecords() {
        List<VehiclePassEntity> records = fetchAllPassRecords();
        System.out.println("\n--- JPA: All Vehicle Pass Records ---");
        System.out.printf("%-8s %-14s %-8s %-12s %-14s %-7s %-10s%n",
                "PassID", "RegNum", "Type", "Toll (INR)", "Balance (INR)", "Lane", "Status");
        System.out.println("---------------------------------------------------------------------------");

        if (records.isEmpty()) {
            System.out.println("  [No records found]");
        } else {
            for (VehiclePassEntity e : records) {
                System.out.printf("%-8d %-14s %-8s %-12.2f %-14.2f %-7d %-10s%n",
                        e.getPassId(), e.getRegistrationNum(), e.getVehicleType(),
                        e.getTollAmount(), e.getRemainingBalance(),
                        e.getLaneNumber(), e.getStatus());
            }
        }
        System.out.println("  Total: " + records.size() + " record(s)");
    }

    public void close() {
        if (emf != null && emf.isOpen()) {
            emf.close();
            System.out.println("[DatabaseManager] EntityManagerFactory closed.");
        }
    }
}