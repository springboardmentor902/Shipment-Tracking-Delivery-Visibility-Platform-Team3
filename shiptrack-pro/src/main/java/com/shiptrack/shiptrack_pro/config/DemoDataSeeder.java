package com.shiptrack.shiptrack_pro.config;

import com.shiptrack.shiptrack_pro.entity.Shipment;
import com.shiptrack.shiptrack_pro.entity.ShipmentPackage;
import com.shiptrack.shiptrack_pro.entity.ShipmentPriority;
import com.shiptrack.shiptrack_pro.entity.ShipmentStatus;
import com.shiptrack.shiptrack_pro.entity.User;
import com.shiptrack.shiptrack_pro.repository.ShipmentRepository;
import com.shiptrack.shiptrack_pro.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Seeds demo users (one per role) and a spread of sample shipments so the UI has
 * something to show on a fresh database.
 *
 * Idempotent: users are created only when the email is free, and shipments are
 * skipped entirely once the SHIPMENTS table is non-empty. Disable with
 * app.seed.demo-data=false in application.properties.
 */
@Component
@RequiredArgsConstructor
public class DemoDataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ShipmentRepository shipmentRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.demo-data:true}")
    private boolean enabled;

    @Override
    public void run(String... args) {
        if (!enabled) {
            return;
        }

        User business = upsertUser("Acme Traders Pvt Ltd", "biz@acme.com", "Biz@12345",
                "9810011111", "BUSINESS_CLIENT");
        User operator = upsertUser("Ops Rider One", "ops@shiptrack.com", "Ops@12345",
                "9810022222", "LOGISTICS_OPERATOR");
        User customer = upsertUser("Ravi Kumar", "ravi@gmail.com", "Ravi@12345",
                "9810033333", "CUSTOMER");
        upsertUser("Priya Support", "support@shiptrack.com", "Support@123",
                "9810044444", "SUPPORT_AGENT");

        if (shipmentRepository.count() > 0) {
            return; // shipments already present, leave real data alone
        }

        LocalDate today = LocalDate.now();

        // 1 — freshly created, awaiting pickup
        Shipment s1 = shipment("STPDEMO00001", business, null,
                "Acme Traders Pvt Ltd", "9810011111", "Plot 21, Gachibowli, Hyderabad 500032",
                "Ravi Kumar", "9810033333", "ravi@gmail.com", "12-4-9 Banjara Hills, Hyderabad 500034",
                "Plot 21, Gachibowli, Hyderabad 500032", "12-4-9 Banjara Hills, Hyderabad 500034",
                ShipmentStatus.CREATED, ShipmentPriority.STANDARD, today.plusDays(5), null);
        s1.addPackage(pkg(1, "Cotton kurta set - 3 pcs", "2.400", "40", "30", "12", 1, "3200.00", false));
        shipmentRepository.save(s1);

        // 2 — picked up by the operator
        Shipment s2 = shipment("STPDEMO00002", business, operator,
                "Acme Traders Pvt Ltd", "9810011111", "Plot 21, Gachibowli, Hyderabad 500032",
                "Sneha Iyer", "9820055555", "sneha.iyer@example.com", "7B Koregaon Park, Pune 411001",
                "Plot 21, Gachibowli, Hyderabad 500032", "7B Koregaon Park, Pune 411001",
                ShipmentStatus.PICKED_UP, ShipmentPriority.EXPRESS, today.plusDays(2), null);
        s2.addPackage(pkg(1, "Bluetooth speaker", "1.100", "22", "18", "18", 2, "5400.00", true));
        shipmentRepository.save(s2);

        // 3 — moving between hubs
        Shipment s3 = shipment("STPDEMO00003", business, operator,
                "Acme Traders Pvt Ltd", "9810011111", "Warehouse 4, Medchal, Hyderabad 501401",
                "Arjun Menon", "9830066666", "arjun.menon@example.com", "45 Anna Nagar, Chennai 600040",
                "Warehouse 4, Medchal, Hyderabad 501401", "45 Anna Nagar, Chennai 600040",
                ShipmentStatus.IN_TRANSIT, ShipmentPriority.STANDARD, today.plusDays(3), null);
        s3.addPackage(pkg(1, "Steel cookware set", "6.800", "50", "40", "25", 1, "8900.00", false));
        s3.addPackage(pkg(2, "Glass storage jars", "3.200", "35", "35", "30", 4, "1600.00", true));
        shipmentRepository.save(s3);

        // 4 — with the rider today
        Shipment s4 = shipment("STPDEMO00004", operator, operator,
                "Acme Traders Pvt Ltd", "9810011111", "Hub 2, Kondapur, Hyderabad 500084",
                "Meera Nair", "9840077777", "meera.nair@example.com", "9 MG Road, Bengaluru 560001",
                "Hub 2, Kondapur, Hyderabad 500084", "9 MG Road, Bengaluru 560001",
                ShipmentStatus.OUT_FOR_DELIVERY, ShipmentPriority.EXPRESS, today, null);
        s4.addPackage(pkg(1, "Laptop sleeve + charger", "1.600", "42", "30", "8", 1, "4200.00", false));
        shipmentRepository.save(s4);

        // 5 — delivered on time
        Shipment s5 = shipment("STPDEMO00005", business, operator,
                "Acme Traders Pvt Ltd", "9810011111", "Plot 21, Gachibowli, Hyderabad 500032",
                "Rohit Sharma", "9850088888", "rohit.sharma@example.com", "22 Sector 18, Noida 201301",
                "Plot 21, Gachibowli, Hyderabad 500032", "22 Sector 18, Noida 201301",
                ShipmentStatus.DELIVERED, ShipmentPriority.STANDARD, today.minusDays(2), today.minusDays(2));
        s5.addPackage(pkg(1, "Office stationery bundle", "4.500", "45", "35", "20", 3, "2750.00", false));
        shipmentRepository.save(s5);

        // 6 — delivered a day early
        Shipment s6 = shipment("STPDEMO00006", business, operator,
                "Acme Traders Pvt Ltd", "9810011111", "Warehouse 4, Medchal, Hyderabad 501401",
                "Kavya Reddy", "9860099999", "kavya.reddy@example.com", "3 Jubilee Hills, Hyderabad 500033",
                "Warehouse 4, Medchal, Hyderabad 501401", "3 Jubilee Hills, Hyderabad 500033",
                ShipmentStatus.DELIVERED, ShipmentPriority.EXPRESS, today.minusDays(4), today.minusDays(5));
        s6.addPackage(pkg(1, "Ceramic dinner plates", "5.900", "38", "38", "22", 6, "6100.00", true));
        shipmentRepository.save(s6);

        // 7 — failed delivery attempt, waiting for a retry
        Shipment s7 = shipment("STPDEMO00007", business, operator,
                "Acme Traders Pvt Ltd", "9810011111", "Hub 2, Kondapur, Hyderabad 500084",
                "Imran Qureshi", "9870011111", "imran.q@example.com", "14 Park Street, Kolkata 700016",
                "Hub 2, Kondapur, Hyderabad 500084", "14 Park Street, Kolkata 700016",
                ShipmentStatus.FAILED_DELIVERY, ShipmentPriority.STANDARD, today.minusDays(1), null);
        s7.addPackage(pkg(1, "Running shoes size 9", "1.300", "34", "24", "14", 1, "3600.00", false));
        shipmentRepository.save(s7);

        // 8 — cancelled by the customer before pickup
        Shipment s8 = shipment("STPDEMO00008", business, null,
                "Acme Traders Pvt Ltd", "9810011111", "Plot 21, Gachibowli, Hyderabad 500032",
                "Ravi Kumar", "9810033333", "ravi@gmail.com", "12-4-9 Banjara Hills, Hyderabad 500034",
                "Plot 21, Gachibowli, Hyderabad 500032", "12-4-9 Banjara Hills, Hyderabad 500034",
                ShipmentStatus.CANCELLED, ShipmentPriority.STANDARD, today.plusDays(4), null);
        s8.setCancelledAt(LocalDateTime.now().minusDays(1));
        s8.setCancellationReason("Customer ordered the wrong size and asked to cancel");
        s8.addPackage(pkg(1, "Denim jacket", "1.800", "40", "32", "10", 1, "2900.00", false));
        shipmentRepository.save(s8);

        // 9 — customer's own shipment, so a CUSTOMER login also sees data
        Shipment s9 = shipment("STPDEMO00009", operator, operator,
                "Ravi Kumar", "9810033333", "12-4-9 Banjara Hills, Hyderabad 500034",
                "Anita Desai", "9880022222", "anita.desai@example.com", "5 Vastrapur, Ahmedabad 380015",
                "12-4-9 Banjara Hills, Hyderabad 500034", "5 Vastrapur, Ahmedabad 380015",
                ShipmentStatus.IN_TRANSIT, ShipmentPriority.EXPRESS, today.plusDays(1), null);
        s9.addPackage(pkg(1, "Books - 4 titles", "3.700", "30", "24", "20", 1, "1850.00", false));
        shipmentRepository.save(s9);

        System.out.println("Seeded demo users and " + shipmentRepository.count() + " sample shipments");
    }

    /* ---------- helpers ---------- */

    private User upsertUser(String fullName, String email, String rawPassword, String phone, String role) {
        return userRepository.findByEmail(email).orElseGet(() -> userRepository.save(
                User.builder()
                        .fullName(fullName)
                        .email(email)
                        .password(passwordEncoder.encode(rawPassword))
                        .phone(phone)
                        .role(role)
                        .status("ACTIVE")
                        .build()));
    }

    private Shipment shipment(String trackingNumber, User createdBy, User operator,
                              String senderName, String senderPhone, String senderAddress,
                              String receiverName, String receiverPhone, String receiverEmail,
                              String receiverAddress, String pickupAddress, String deliveryAddress,
                              ShipmentStatus status, ShipmentPriority priority,
                              LocalDate estimatedDeliveryDate, LocalDate actualDeliveryDate) {
        return Shipment.builder()
                .trackingNumber(trackingNumber)
                .createdBy(createdBy)
                .assignedOperator(operator)
                .senderName(senderName)
                .senderPhone(senderPhone)
                .senderAddress(senderAddress)
                .receiverName(receiverName)
                .receiverPhone(receiverPhone)
                .receiverEmail(receiverEmail)
                .receiverAddress(receiverAddress)
                .pickupAddress(pickupAddress)
                .deliveryAddress(deliveryAddress)
                .status(status)
                .priority(priority)
                .estimatedDeliveryDate(estimatedDeliveryDate)
                .actualDeliveryDate(actualDeliveryDate)
                .build();
    }

    private ShipmentPackage pkg(int packageNo, String description, String weightKg,
                                String lengthCm, String widthCm, String heightCm,
                                int quantity, String declaredValue, boolean fragile) {
        return ShipmentPackage.builder()
                .packageNo(packageNo)
                .description(description)
                .weightKg(new BigDecimal(weightKg))
                .lengthCm(new BigDecimal(lengthCm))
                .widthCm(new BigDecimal(widthCm))
                .heightCm(new BigDecimal(heightCm))
                .quantity(quantity)
                .declaredValue(new BigDecimal(declaredValue))
                .fragile(fragile)
                .build();
    }
}
