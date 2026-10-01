package com.garageos.modules.notification.service;

import com.garageos.core.enums.notification.NotificationEventType;
import com.garageos.modules.notification.event.NotificationFacts;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Renders persona-appropriate title/message text. Text only ever contains
 * the job card number as a reference - no customer, vehicle, amount or
 * contact data - because it is shown on lock screens.
 */
@Component
public class NotificationTemplateFactory {

    public record Rendered(String title, String message) {
    }

    public Rendered render(NotificationEventType type, NotificationPersona persona, Map<String, Object> facts) {

        Object number = facts == null ? null : facts.get(NotificationFacts.JOB_CARD_NUMBER);
        String ref = number == null ? "" : " (" + number + ")";
        boolean customer = persona == NotificationPersona.CUSTOMER;
        boolean owner = persona == NotificationPersona.OWNER;
        boolean driver = persona == NotificationPersona.DRIVER;
        boolean technician = persona == NotificationPersona.TECHNICIAN;

        return switch (type) {

            case BOOKING_REQUESTED -> new Rendered(
                    owner ? "New booking received" : "New booking request",
                    "A customer has requested a service booking.");
            case BOOKING_CREATED_BY_STAFF -> new Rendered(
                    "Booking created",
                    "A service booking has been created for you.");
            case BOOKING_CONFIRMED -> new Rendered(
                    "Booking confirmed",
                    "Your service booking has been confirmed.");
            case BOOKING_REJECTED -> new Rendered(
                    "Booking not accepted",
                    "Your service booking could not be accepted. Open the app for details.");
            case BOOKING_CANCELLED -> new Rendered(
                    "Booking cancelled",
                    customer ? "Your service booking has been cancelled." : "A service booking has been cancelled.");

            case PICKUP_DRIVER_ASSIGNED -> driver
                    ? new Rendered("Pickup assigned", "You have been assigned a new vehicle trip.")
                    : new Rendered("Driver assigned", "A driver has been assigned to your vehicle trip.");
            case TRIP_ACCEPTED -> new Rendered(
                    "Trip accepted",
                    customer ? "Your driver has accepted the trip." : "The driver has accepted the trip.");
            case TRIP_STARTED -> new Rendered(
                    customer ? "Your driver is on the way" : "Trip started",
                    customer ? "Track your vehicle trip live in the app." : "A vehicle trip is now in progress.");
            case DRIVER_ARRIVED -> new Rendered(
                    "Your driver has arrived",
                    "Open the app to complete the vehicle handover.");
            case HANDOVER_CODE_GENERATED -> new Rendered(
                    "Customer handover required",
                    "The customer is ready to hand over. Verify the handover code in the app.");
            case HANDOVER_VERIFIED -> new Rendered(
                    "Handover verified",
                    "The vehicle handover has been verified.");
            case TRIP_COMPLETED -> new Rendered(
                    "Trip completed",
                    customer ? "Your vehicle trip has been completed." : "A vehicle trip has been completed.");

            case JOB_CARD_CREATED -> new Rendered(
                    customer ? "Service started" : "New job card",
                    customer
                            ? "A job card has been opened for your vehicle" + ref + "."
                            : "A job card has been created" + ref + ".");

            case INSPECTION_STARTED -> new Rendered(
                    "Inspection started",
                    "Inspection of your vehicle has started" + ref + ".");
            case INSPECTION_COMPLETED -> new Rendered(
                    "Inspection completed",
                    customer
                            ? "Inspection of your vehicle is complete" + ref + "."
                            : "Inspection completed" + ref + ".");

            case ESTIMATE_READY_FOR_APPROVAL -> new Rendered(
                    "Your estimate is ready",
                    "Please review and approve your estimate" + ref + ".");
            case ESTIMATE_APPROVED -> new Rendered(
                    customer ? "Estimate approved" : "Estimate approved",
                    customer
                            ? "Your estimate has been approved" + ref + "."
                            : "The estimate has been approved" + ref + ".");
            case ESTIMATE_REJECTED -> new Rendered(
                    "Estimate rejected",
                    "The estimate was rejected" + ref + ".");

            case REPAIR_TASK_ASSIGNED -> new Rendered(
                    "New repair task assigned",
                    "A repair task has been assigned to you" + ref + ".");
            case REPAIR_TASK_STARTED -> new Rendered(
                    "Repair task started",
                    "A repair task has been started" + ref + ".");
            case REPAIR_TASK_COMPLETED -> new Rendered(
                    "Repair task completed",
                    "A repair task has been completed" + ref + ".");

            case JOB_REPAIR_STARTED -> new Rendered(
                    "Repair started",
                    "Repair of your vehicle has started" + ref + ".");
            case JOB_REPAIR_COMPLETED -> new Rendered(
                    "Repair completed",
                    customer
                            ? "Repair of your vehicle is complete" + ref + "."
                            : "Repair completed" + ref + ".");

            case QUALITY_CHECK_STARTED -> new Rendered(
                    "Quality check started",
                    "Quality check has started" + ref + ".");
            case QUALITY_CHECK_PASSED -> new Rendered(
                    "Quality check passed",
                    "Quality check passed" + ref + ".");
            case QUALITY_CHECK_FAILED -> new Rendered(
                    technician ? "Quality check failed — rework required" : "Quality check failed",
                    technician
                            ? "Rework is required" + ref + "."
                            : "Quality check failed and rework is required" + ref + ".");

            case INVOICE_GENERATED -> new Rendered(
                    customer ? "Your invoice is ready" : "Invoice generated",
                    customer
                            ? "Your invoice is ready for review" + ref + "."
                            : "An invoice has been generated" + ref + ".");
            case INVOICE_ACCEPTED -> new Rendered(
                    "Invoice accepted",
                    "The customer accepted the invoice" + ref + ".");
            case PAYMENT_RECEIVED -> new Rendered(
                    "Payment received",
                    customer
                            ? "Your payment has been received" + ref + "."
                            : "Payment has been received" + ref + ".");

            case VEHICLE_READY_FOR_DELIVERY -> new Rendered(
                    customer ? "Your vehicle is ready for delivery" : "Vehicle ready for delivery",
                    customer
                            ? "Your vehicle is ready" + ref + "."
                            : "A vehicle is ready for delivery" + ref + ".");
            case VEHICLE_DELIVERED -> new Rendered(
                    customer ? "Your vehicle has been delivered" : "Vehicle delivered",
                    customer
                            ? "Your vehicle has been delivered" + ref + "."
                            : "A vehicle has been delivered" + ref + ".");
            case JOB_CLOSED -> new Rendered(
                    "Job closed",
                    "A job card has been closed" + ref + ".");
        };
    }
}
