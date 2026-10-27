/**
 * Shipments and their scan history ({@code shipment} and {@code tracking_event} tables). {@code TrackingEvent} lives here
 * because it is only ever reached through its {@code Shipment}.
 */
@NullMarked
package gr.codelearn.springbootai.persistencedemo.shipment;

import org.jspecify.annotations.NullMarked;