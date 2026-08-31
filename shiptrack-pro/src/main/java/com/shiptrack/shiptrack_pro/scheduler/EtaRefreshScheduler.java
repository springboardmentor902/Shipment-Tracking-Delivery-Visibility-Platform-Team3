package com.shiptrack.shiptrack_pro.scheduler;

import com.shiptrack.shiptrack_pro.service.EtaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Keeps forecasts honest while nothing is happening.
 *
 * Location pings already refresh the ETA, but a shipment that goes quiet is
 * exactly the one whose risk should be climbing, so time alone has to trigger a
 * recalculation too. Disable with eta.refresh.enabled=false.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EtaRefreshScheduler {

    private final EtaService etaService;

    @Value("${eta.refresh.enabled:true}")
    private boolean enabled;

    @Scheduled(fixedDelayString = "${eta.refresh.interval-ms:900000}",
            initialDelayString = "${eta.refresh.initial-delay-ms:60000}")
    public void refreshActiveShipments() {
        if (!enabled) {
            return;
        }
        try {
            int updated = etaService.recalculateActive();
            if (updated > 0) {
                log.debug("Refreshed ETA for {} active shipment(s)", updated);
            }
        } catch (RuntimeException ex) {
            // a failed sweep must not stop the next one
            log.warn("Scheduled ETA sweep failed: {}", ex.getMessage());
        }
    }
}
