package com.garageos.modules.notification.event;

import com.garageos.core.enums.notification.NotificationEventType;
import com.garageos.modules.jobcard.entity.JobCard;

/**
 * Null-safe publish helpers for business services, so a service constructed
 * without a publisher (plain unit tests, notifications unwired) simply
 * emits nothing instead of failing the business operation.
 */
public final class NotificationEvents {

    private NotificationEvents() {
    }

    public static void publish(NotificationEventPublisher publisher, NotificationEvent event) {
        if (publisher != null) {
            publisher.publish(event);
        }
    }

    /**
     * Job-card-scoped event: garage, customer and job card number are taken
     * from the job card. The discriminator separates repeated passes through
     * the same state (see {@link NotificationEvent}).
     */
    public static void publishJobCardEvent(
            NotificationEventPublisher publisher,
            NotificationEventType type,
            JobCard jobCard,
            Object discriminator) {

        publishJobCardEvent(publisher, type, jobCard, jobCard == null ? null : jobCard.getId(), discriminator);
    }

    /** Same as above for an event whose deep-link entity is not the job card itself (estimate, invoice, repair task). */
    public static void publishJobCardEvent(
            NotificationEventPublisher publisher,
            NotificationEventType type,
            JobCard jobCard,
            Long entityId,
            Object discriminator) {

        if (publisher == null || jobCard == null) {
            return;
        }

        publisher.publish(jobCardEvent(type, jobCard, entityId, discriminator));
    }

    /** Builds (does not publish) a job-card-scoped event so callers can add extra facts. */
    public static NotificationEvent jobCardEvent(
            NotificationEventType type,
            JobCard jobCard,
            Long entityId,
            Object discriminator) {

        return NotificationEvent.of(
                        type,
                        jobCard.getGarage() == null ? null : jobCard.getGarage().getId(),
                        entityId,
                        jobCard.getId(),
                        discriminator)
                .fact(NotificationFacts.JOB_CARD_NUMBER, jobCard.getJobCardNumber())
                .fact(NotificationFacts.CUSTOMER_ID,
                        jobCard.getCustomer() == null ? null : jobCard.getCustomer().getId());
    }
}
