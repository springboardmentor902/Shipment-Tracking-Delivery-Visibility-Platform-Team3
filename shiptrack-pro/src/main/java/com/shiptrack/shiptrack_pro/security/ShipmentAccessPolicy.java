package com.shiptrack.shiptrack_pro.security;

import com.shiptrack.shiptrack_pro.entity.Shipment;
import com.shiptrack.shiptrack_pro.entity.User;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * One place that answers "may this user see this shipment?".
 *
 * REST calls and live WebSocket subscriptions must agree on the answer, so both
 * go through this component instead of repeating the rules.
 */
@Component
public class ShipmentAccessPolicy {

    public boolean canView(Shipment shipment, User user) {
        if (shipment == null || user == null) {
            return false;
        }

        return switch (Role.valueOf(user.getRole())) {
            case ADMINISTRATOR, SUPPORT_AGENT -> true;
            // an operator only ever sees the work assigned to them
            case LOGISTICS_OPERATOR -> isAssignedOperator(shipment, user);
            case BUSINESS_CLIENT -> isCreator(shipment, user)
                    || isAssignedOperator(shipment, user)
                    || belongsToBusiness(shipment, user);
            case CUSTOMER -> isCreator(shipment, user) || isReceiver(shipment, user);
        };
    }

    /** Roles allowed to watch the fleet-wide active delivery feed. */
    public boolean canMonitorFleet(User user) {
        if (user == null) {
            return false;
        }
        Role role = Role.valueOf(user.getRole());
        return role == Role.LOGISTICS_OPERATOR || role == Role.SUPPORT_AGENT || role == Role.ADMINISTRATOR;
    }

    public boolean isCreator(Shipment shipment, User user) {
        return shipment.getCreatedBy() != null
                && Objects.equals(shipment.getCreatedBy().getId(), user.getId());
    }

    public boolean isAssignedOperator(Shipment shipment, User user) {
        return shipment.getAssignedOperator() != null
                && Objects.equals(shipment.getAssignedOperator().getId(), user.getId());
    }

    /** Business clients own every shipment booked under their business id. */
    public boolean belongsToBusiness(Shipment shipment, User user) {
        return shipment.getBusinessId() != null
                && Objects.equals(shipment.getBusinessId(), user.getId());
    }

    /** A customer receiving a parcel is linked by email only. */
    public boolean isReceiver(Shipment shipment, User user) {
        return shipment.getReceiverEmail() != null
                && shipment.getReceiverEmail().equalsIgnoreCase(user.getEmail());
    }
}
