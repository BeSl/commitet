package com.company.commitet_jm.config

import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationStartingEvent
import org.springframework.context.ApplicationListener
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import java.sql.DriverManager

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class DatabaseInitializer : ApplicationListener<ApplicationStartingEvent> {

    private val log = LoggerFactory.getLogger(DatabaseInitializer::class.java)

    override fun onApplicationEvent(event: ApplicationStartingEvent) {
        try {
            log.info("Early database initialization - creating missing tables...")

            // Connect directly to HSQLDB
            val connection = DriverManager.getConnection(
                "jdbc:hsqldb:file:.jmix/hsqldb/commitetjm",
                "sa",
                ""
            )

            connection.use { conn ->
                conn.createStatement().use { stmt ->
                    // Create CONFIG_METADATA_ITEM table
                    stmt.execute("""
                        CREATE TABLE IF NOT EXISTS CONFIG_METADATA_ITEM (
                            ID UUID NOT NULL PRIMARY KEY,
                            EXTERNAL_ID VARCHAR(255),
                            NAME VARCHAR(255) NOT NULL,
                            METADATA_TYPE VARCHAR(255),
                            IS_COLLECTION BOOLEAN DEFAULT FALSE,
                            SORT_ORDER INT DEFAULT 0,
                            FULL_PATH VARCHAR(1000),
                            PARENT_ID UUID,
                            PROJECT_ID UUID NOT NULL,
                            VERSION INT NOT NULL
                        )
                    """.trimIndent())

                    stmt.execute("CREATE INDEX IF NOT EXISTS IDX_CMI_PROJECT ON CONFIG_METADATA_ITEM(PROJECT_ID)")
                    stmt.execute("CREATE INDEX IF NOT EXISTS IDX_CMI_PARENT ON CONFIG_METADATA_ITEM(PARENT_ID)")
                    stmt.execute("CREATE INDEX IF NOT EXISTS IDX_CMI_EXTERNAL_ID ON CONFIG_METADATA_ITEM(EXTERNAL_ID)")
                    stmt.execute("CREATE INDEX IF NOT EXISTS IDX_CMI_FULL_PATH ON CONFIG_METADATA_ITEM(FULL_PATH)")

                    // USER_EXTERNAL_ID
                    stmt.execute("""
                        CREATE TABLE IF NOT EXISTS USER_EXTERNAL_ID (
                            ID UUID NOT NULL PRIMARY KEY,
                            USER_ID UUID NOT NULL,
                            EXTERNAL_ID VARCHAR(255) NOT NULL UNIQUE,
                            SOURCE VARCHAR(255),
                            DESCRIPTION VARCHAR(255),
                            DATE_CREATED DATETIME
                        )
                    """.trimIndent())

                    stmt.execute("CREATE INDEX IF NOT EXISTS IDX_USER_EXTERNAL_ID_VALUE ON USER_EXTERNAL_ID(EXTERNAL_ID)")
                    stmt.execute("CREATE INDEX IF NOT EXISTS IDX_USER_EXTERNAL_ID_USER ON USER_EXTERNAL_ID(USER_ID)")

                    // PROJECT_EXTERNAL_ID
                    stmt.execute("""
                        CREATE TABLE IF NOT EXISTS PROJECT_EXTERNAL_ID (
                            ID UUID NOT NULL PRIMARY KEY,
                            PROJECT_ID UUID NOT NULL,
                            EXTERNAL_ID VARCHAR(255) NOT NULL UNIQUE,
                            SOURCE VARCHAR(255),
                            DESCRIPTION VARCHAR(255),
                            DATE_CREATED DATETIME
                        )
                    """.trimIndent())

                    stmt.execute("CREATE INDEX IF NOT EXISTS IDX_PROJECT_EXTERNAL_ID_VALUE ON PROJECT_EXTERNAL_ID(EXTERNAL_ID)")
                    stmt.execute("CREATE INDEX IF NOT EXISTS IDX_PROJECT_EXTERNAL_ID_PROJECT ON PROJECT_EXTERNAL_ID(PROJECT_ID)")

                    log.info("✓ All tables created/verified successfully")
                }
            }
        } catch (e: Exception) {
            log.error("Error during early database initialization", e)
        }
    }
}
