package com.garageos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * {@code @EnableScheduling} added for MediaUploadRetryScheduler (see
 * com.garageos.modules.media.service.impl) — the backoff-retry executor for
 * queued Google Drive media uploads. No {@code @Scheduled} job existed
 * anywhere else in this application prior to that addition; this enables
 * Spring's own built-in scheduling support (already on the classpath via
 * spring-context, no new dependency) rather than introducing a separate
 * job runner/Quartz for a single periodic task.
 */
@SpringBootApplication
@EnableScheduling
public class GarageOsApplication {

	public static void main(String[] args) {
		SpringApplication.run(GarageOsApplication.class, args);
	}

}
