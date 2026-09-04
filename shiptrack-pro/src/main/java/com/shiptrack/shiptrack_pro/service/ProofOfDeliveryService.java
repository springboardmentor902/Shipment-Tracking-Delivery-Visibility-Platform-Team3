package com.shiptrack.shiptrack_pro.service;

import com.shiptrack.shiptrack_pro.entity.ProofOfDelivery;
import org.springframework.web.multipart.MultipartFile;

public interface ProofOfDeliveryService {

    ProofOfDelivery createProofOfDelivery(
            Long shipmentId,
            Long verifiedById,
            MultipartFile signature,
            MultipartFile photo,
            String deliveredToName,
            String deliveryNotes
    );

    ProofOfDelivery getByShipmentId(Long shipmentId);

    ProofOfDelivery verify(
            Long shipmentId,
            Long verifiedById
    );
}