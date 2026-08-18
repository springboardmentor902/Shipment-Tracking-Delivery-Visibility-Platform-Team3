package com.shiptrack.shiptrack_pro.config;

import com.shiptrack.shiptrack_pro.entity.Shipment;
import com.shiptrack.shiptrack_pro.entity.ShipmentPackage;
import com.shiptrack.shiptrack_pro.entity.ShipmentPriority;
import com.shiptrack.shiptrack_pro.entity.ShipmentStatus;
import com.shiptrack.shiptrack_pro.entity.User;
import com.shiptrack.shiptrack_pro.repository.ShipmentRepository;
import com.shiptrack.shiptrack_pro.repository.TrackingEventRepository;
import com.shiptrack.shiptrack_pro.repository.UserRepository;
import com.shiptrack.shiptrack_pro.entity.TrackingEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

// puts some demo users and shipments in the db so the ui is not empty on a fresh setup.
// runs only once - existing emails are skipped and shipments are added only if the table is empty.
// set app.seed.demo-data=false in application.properties to turn it off.
@Component
@RequiredArgsConstructor
public class DemoDataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ShipmentRepository shipmentRepository;
    private final TrackingEventRepository trackingEventRepository;
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
        upsertUser("Ravi Kumar", "ravi@gmail.com", "Ravi@12345",
                "9810033333", "CUSTOMER");
        upsertUser("Priya Support", "support@shiptrack.com", "Support@123",
                "9810044444", "SUPPORT_AGENT");

        if (shipmentRepository.count() > 0) {
            return; // already has data, do not touch it
        }

        LocalDate today = LocalDate.now();

        // waiting for pickup
        Shipment s1 = shipment("STPDEMO00001", business, null,
                "Acme Traders Pvt Ltd", "9810011111", "Plot 21, Gachibowli, Hyderabad 500032",
                "Ravi Kumar", "9810033333", "ravi@gmail.com", "12-4-9 Banjara Hills, Hyderabad 500034",
                "Plot 21, Gachibowli, Hyderabad 500032", "12-4-9 Banjara Hills, Hyderabad 500034",
                ShipmentStatus.CREATED, ShipmentPriority.STANDARD, today.plusDays(5), null);
        s1.addPackage(pkg(1, "Cotton kurta set - 3 pcs", "2.400", "40", "30", "12", 1, "3200.00", false));
        shipmentRepository.save(s1);

        // picked up
        Shipment s2 = shipment("STPDEMO00002", business, operator,
                "Acme Traders Pvt Ltd", "9810011111", "Plot 21, Gachibowli, Hyderabad 500032",
                "Sneha Iyer", "9820055555", "sneha.iyer@example.com", "7B Koregaon Park, Pune 411001",
                "Plot 21, Gachibowli, Hyderabad 500032", "7B Koregaon Park, Pune 411001",
                ShipmentStatus.PICKED_UP, ShipmentPriority.EXPRESS, today.plusDays(2), null);
        s2.addPackage(pkg(1, "Bluetooth speaker", "1.100", "22", "18", "18", 2, "5400.00", true));
        shipmentRepository.save(s2);

        // on the way, 2 packages
        Shipment s3 = shipment("STPDEMO00003", business, operator,
                "Acme Traders Pvt Ltd", "9810011111", "Warehouse 4, Medchal, Hyderabad 501401",
                "Arjun Menon", "9830066666", "arjun.menon@example.com", "45 Anna Nagar, Chennai 600040",
                "Warehouse 4, Medchal, Hyderabad 501401", "45 Anna Nagar, Chennai 600040",
                ShipmentStatus.IN_TRANSIT, ShipmentPriority.STANDARD, today.plusDays(3), null);
        s3.addPackage(pkg(1, "Steel cookware set", "6.800", "50", "40", "25", 1, "8900.00", false));
        s3.addPackage(pkg(2, "Glass storage jars", "3.200", "35", "35", "30", 4, "1600.00", true));
        shipmentRepository.save(s3);

        // rider is out with it today
        Shipment s4 = shipment("STPDEMO00004", operator, operator,
                "Acme Traders Pvt Ltd", "9810011111", "Hub 2, Kondapur, Hyderabad 500084",
                "Meera Nair", "9840077777", "meera.nair@example.com", "9 MG Road, Bengaluru 560001",
                "Hub 2, Kondapur, Hyderabad 500084", "9 MG Road, Bengaluru 560001",
                ShipmentStatus.OUT_FOR_DELIVERY, ShipmentPriority.EXPRESS, today, null);
        s4.addPackage(pkg(1, "Laptop sleeve + charger", "1.600", "42", "30", "8", 1, "4200.00", false));
        shipmentRepository.save(s4);

        // delivered on time
        Shipment s5 = shipment("STPDEMO00005", business, operator,
                "Acme Traders Pvt Ltd", "9810011111", "Plot 21, Gachibowli, Hyderabad 500032",
                "Rohit Sharma", "9850088888", "rohit.sharma@example.com", "22 Sector 18, Noida 201301",
                "Plot 21, Gachibowli, Hyderabad 500032", "22 Sector 18, Noida 201301",
                ShipmentStatus.DELIVERED, ShipmentPriority.STANDARD, today.minusDays(2), today.minusDays(2));
        s5.addPackage(pkg(1, "Office stationery bundle", "4.500", "45", "35", "20", 3, "2750.00", false));
        shipmentRepository.save(s5);

        // delivered 1 day early
        Shipment s6 = shipment("STPDEMO00006", business, operator,
                "Acme Traders Pvt Ltd", "9810011111", "Warehouse 4, Medchal, Hyderabad 501401",
                "Kavya Reddy", "9860099999", "kavya.reddy@example.com", "3 Jubilee Hills, Hyderabad 500033",
                "Warehouse 4, Medchal, Hyderabad 501401", "3 Jubilee Hills, Hyderabad 500033",
                ShipmentStatus.DELIVERED, ShipmentPriority.EXPRESS, today.minusDays(4), today.minusDays(5));
        s6.addPackage(pkg(1, "Ceramic dinner plates", "5.900", "38", "38", "22", 6, "6100.00", true));
        shipmentRepository.save(s6);

        // delivery attempt failed, needs retry
        Shipment s7 = shipment("STPDEMO00007", business, operator,
                "Acme Traders Pvt Ltd", "9810011111", "Hub 2, Kondapur, Hyderabad 500084",
                "Imran Qureshi", "9870011111", "imran.q@example.com", "14 Park Street, Kolkata 700016",
                "Hub 2, Kondapur, Hyderabad 500084", "14 Park Street, Kolkata 700016",
                ShipmentStatus.FAILED_DELIVERY, ShipmentPriority.STANDARD, today.minusDays(1), null);
        s7.addPackage(pkg(1, "Running shoes size 9", "1.300", "34", "24", "14", 1, "3600.00", false));
        shipmentRepository.save(s7);

        // cancelled before pickup
        Shipment s8 = shipment("STPDEMO00008", business, null,
                "Acme Traders Pvt Ltd", "9810011111", "Plot 21, Gachibowli, Hyderabad 500032",
                "Ravi Kumar", "9810033333", "ravi@gmail.com", "12-4-9 Banjara Hills, Hyderabad 500034",
                "Plot 21, Gachibowli, Hyderabad 500032", "12-4-9 Banjara Hills, Hyderabad 500034",
                ShipmentStatus.CANCELLED, ShipmentPriority.STANDARD, today.plusDays(4), null);
        s8.setCancelledAt(LocalDateTime.now().minusDays(1));
        s8.setCancellationReason("Customer ordered the wrong size and asked to cancel");
        s8.addPackage(pkg(1, "Denim jacket", "1.800", "40", "32", "10", 1, "2900.00", false));
        shipmentRepository.save(s8);

        // ravi is the receiver here so the customer login has an in-transit one
        Shipment s9 = shipment("STPDEMO00009", operator, operator,
                "Anita Desai", "9880022222", "5 Vastrapur, Ahmedabad 380015",
                "Ravi Kumar", "9810033333", "ravi@gmail.com", "12-4-9 Banjara Hills, Hyderabad 500034",
                "5 Vastrapur, Ahmedabad 380015", "12-4-9 Banjara Hills, Hyderabad 500034",
                ShipmentStatus.IN_TRANSIT, ShipmentPriority.EXPRESS, today.plusDays(1), null);
        s9.addPackage(pkg(1, "Books - 4 titles", "3.700", "30", "24", "20", 1, "1850.00", false));
        shipmentRepository.save(s9);

        if (trackingEventRepository.count() == 0) {
            seedEvent(s1, ShipmentStatus.CREATED, "Hyderabad hub", "17.440081", "78.348915", "order received", business);
            seedEvent(s1, ShipmentStatus.CREATED, "Hyderabad hub", "17.440081", "78.348915", "waiting for pickup", business);

            seedEvent(s2, ShipmentStatus.CREATED, "Hyderabad hub", "17.440081", "78.348915", "order received", business);
            seedEvent(s2, ShipmentStatus.PICKED_UP, "Gachibowli, Hyderabad", "17.440497", "78.348917", "parcel picked up", operator);

            seedEvent(s3, ShipmentStatus.CREATED, "Medchal hub, Hyderabad", "17.628647", "78.482785", "order received", business);
            seedEvent(s3, ShipmentStatus.PICKED_UP, "Medchal hub, Hyderabad", "17.628647", "78.482785", "parcel picked up", operator);
            seedEvent(s3, ShipmentStatus.IN_TRANSIT, "Pune transit hub", "18.520430", "73.856744", "moving to Chennai", operator);

            seedEvent(s4, ShipmentStatus.CREATED, "Kondapur hub, Hyderabad", "17.462232", "78.363489", "order received", operator);
            seedEvent(s4, ShipmentStatus.PICKED_UP, "Kondapur hub, Hyderabad", "17.462232", "78.363489", "parcel picked up", operator);
            seedEvent(s4, ShipmentStatus.IN_TRANSIT, "Bengaluru hub", "12.971599", "77.594566", "reached delivery city", operator);
            seedEvent(s4, ShipmentStatus.OUT_FOR_DELIVERY, "MG Road, Bengaluru", "12.975230", "77.606865", "out for delivery", operator);

            seedEvent(s5, ShipmentStatus.CREATED, "Hyderabad hub", "17.440081", "78.348915", "order received", business);
            seedEvent(s5, ShipmentStatus.PICKED_UP, "Hyderabad hub", "17.440081", "78.348915", "parcel picked up", operator);
            seedEvent(s5, ShipmentStatus.IN_TRANSIT, "Pune transit hub", "18.520430", "73.856744", "moving to Noida", operator);
            seedEvent(s5, ShipmentStatus.DELIVERED, "Sector 18, Noida", "28.570633", "77.321901", "delivered to receiver", operator);

            seedEvent(s6, ShipmentStatus.CREATED, "Medchal hub, Hyderabad", "17.628647", "78.482785", "order received", business);
            seedEvent(s6, ShipmentStatus.PICKED_UP, "Medchal hub, Hyderabad", "17.628647", "78.482785", "parcel picked up", operator);
            seedEvent(s6, ShipmentStatus.IN_TRANSIT, "Hyderabad hub", "17.440081", "78.348915", "reached delivery city", operator);
            seedEvent(s6, ShipmentStatus.DELIVERED, "Jubilee Hills, Hyderabad", "17.432584", "78.407062", "delivered to receiver", operator);

            seedEvent(s7, ShipmentStatus.CREATED, "Kondapur hub, Hyderabad", "17.462232", "78.363489", "order received", business);
            seedEvent(s7, ShipmentStatus.PICKED_UP, "Kondapur hub, Hyderabad", "17.462232", "78.363489", "parcel picked up", operator);
            seedEvent(s7, ShipmentStatus.IN_TRANSIT, "Kolkata transit hub", "22.572646", "88.363895", "reached delivery city", operator);
            seedEvent(s7, ShipmentStatus.FAILED_DELIVERY, "Park Street, Kolkata", "22.554759", "88.352791", "receiver unavailable", operator);

            seedEvent(s8, ShipmentStatus.CREATED, "Hyderabad hub", "17.440081", "78.348915", "order received", business);
            seedEvent(s8, ShipmentStatus.CANCELLED, "Hyderabad hub", "17.440081", "78.348915", "customer asked to cancel", business);

            seedEvent(s9, ShipmentStatus.CREATED, "Ahmedabad hub", "23.022505", "72.571362", "order received", operator);
            seedEvent(s9, ShipmentStatus.PICKED_UP, "Ahmedabad hub", "23.022505", "72.571362", "parcel picked up", operator);
            seedEvent(s9, ShipmentStatus.IN_TRANSIT, "Hyderabad hub", "17.440081", "78.348915", "moving to receiver", operator);
        }

        System.out.println("Demo data added: " + shipmentRepository.count() + " shipments");
    }

    /* ---------- small helpers ---------- */

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

    private void seedEvent(Shipment shipment, ShipmentStatus status, String location,
                           String latitude, String longitude, String notes, User recordedBy) {
        trackingEventRepository.save(TrackingEvent.builder()
                .shipment(shipment)
                .status(status)
                .location(location)
                .latitude(new BigDecimal(latitude))
                .longitude(new BigDecimal(longitude))
                .notes(notes)
                .recordedBy(recordedBy)
                .build());
    }
}
