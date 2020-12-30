package com.tng.invitation;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

@Configuration
public class FlywayConfiguration {
	@Value("${admin.flyway.username}")
	private String username;
	@Value("${admin.flyway.password}")
	private String password;
	@Value("${admin.flyway.baselineOnMigrate}")
	private boolean baselineOnMigrate;
	@Value("${admin.database.host}")
	private String host;
	@Value("${admin.database.port}")
	private int port;
	@Value("${admin.database.databaseName}")
	private String databaseName;

	@PostConstruct
	public void updateDatabase() {
		final Flyway flyway = new Flyway(
				new FluentConfiguration()
						.dataSource(
								String.format("jdbc:postgresql://%s:%d/%s", host, port, databaseName),
								username,
								password)
						.baselineOnMigrate(baselineOnMigrate)
		);
		flyway.repair();
		flyway.migrate();
	}

}
