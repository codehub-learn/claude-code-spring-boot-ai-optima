package gr.codelearn.springbootai.persistencedemo.shipment.dto;

import gr.codelearn.springbootai.persistencedemo.shipment.ServiceLevel;

import java.math.BigDecimal;

import org.jspecify.annotations.Nullable;

/**
 * Delivery performance of one {@link ServiceLevel} over a period: how many shipments were delivered, how many of those after their promised
 * time, and the average hours from pickup to delivery ({@code null} when no delivered shipment has a pickup scan).
 */
public record ServiceLevelPerformance(ServiceLevel serviceLevel, long delivered, long late, @Nullable BigDecimal avgTransitHours) {
}