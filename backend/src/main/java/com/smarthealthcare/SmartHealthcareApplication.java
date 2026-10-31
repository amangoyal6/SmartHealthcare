package com.smarthealthcare;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.client.RestClient;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import javax.crypto.SecretKey;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@SpringBootApplication
public class SmartHealthcareApplication {
    public static void main(String[] args) {
        SpringApplication.run(SmartHealthcareApplication.class, args);
    }

    @Bean
    CommandLineRunner seedDemoAccounts(UserRepository users, PatientRepository patients, DoctorRepository doctors, LaboratoryRepository labs, PharmacyRepository pharmacies, PasswordEncoder encoder) {
        return args -> {
            User admin = ensureUser(users, encoder, "System Admin", "admin@smarthealthcare.local", "9000000000", Role.ADMIN, ApprovalStatus.APPROVED);
            User patientUser = ensureUser(users, encoder, "Aarav Sharma", "aarav@example.com", "9876543210", Role.PATIENT, ApprovalStatus.APPROVED);
            User doctorUser = ensureUser(users, encoder, "Dr Mira Patel", "mira.doctor@example.com", "9876543211", Role.DOCTOR, ApprovalStatus.APPROVED);
            User labUser = ensureUser(users, encoder, "CityCare Labs", "labs@example.com", "9876543212", Role.LABORATORY, ApprovalStatus.APPROVED);
            User pharmacyUser = ensureUser(users, encoder, "HealthPlus Pharmacy", "pharmacy@example.com", "9876543213", Role.PHARMACY, ApprovalStatus.APPROVED);

            patients.findByUser(patientUser).orElseGet(() -> {
                Patient p = new Patient(); p.user = patientUser; p.age = 32; p.gender = "Male"; p.allergyDetails = "No"; p.nearDearContactNumber = "9876500000"; p.diseaseDetails = "Asthma"; return patients.save(p);
            });
            doctors.findByUser(doctorUser).or(() -> doctors.findByNmcId("NMC-2026-101")).orElseGet(() -> {
                Doctor d = new Doctor(); d.user = doctorUser; d.age = 40; d.experience = 12; d.speciality = "Cardiology"; d.address = "Mumbai"; d.nmcId = "NMC-2026-101"; return doctors.save(d);
            });
            labs.findByUser(labUser).orElseGet(() -> {
                Laboratory l = new Laboratory(); l.user = labUser; l.labName = "CityCare Labs"; l.address = "Mumbai"; return labs.save(l);
            });
            pharmacies.findByUser(pharmacyUser).orElseGet(() -> {
                Pharmacy p = new Pharmacy(); p.user = pharmacyUser; p.licenseNumber = "PHARMA-LIC-500"; p.address = "Mumbai"; return pharmacies.save(p);
            });
        };
    }

    private static User ensureUser(UserRepository users, PasswordEncoder encoder, String name, String email, String mobile, Role role, ApprovalStatus status) {
        Optional<User> existing = users.findByMobileNumber(mobile);
        if (existing.isPresent()) {
            User user = existing.get();
            user.name = name;
            user.email = email;
            user.password = encoder.encode("admin123");
            user.role = role;
            user.status = status;
            user.emailVerified = true;
            user.mobileVerified = true;
            user.enabled = true;
            return users.save(user);
        }
        return users.save(newUser(encoder, name, email, mobile, role, status));
    }

    private static User newUser(PasswordEncoder encoder, String name, String email, String mobile, Role role, ApprovalStatus status) {
            User user = new User();
            user.name = name;
            user.email = email;
            user.mobileNumber = mobile;
            user.password = encoder.encode("admin123");
            user.role = role;
            user.status = status;
            user.emailVerified = true;
            user.mobileVerified = true;
            user.enabled = true;
            return user;
    }
}

@Component
class VerificationCodeStore {
    private record CodeEntry(String code, LocalDateTime expiresAt) {}
    private final Map<String, CodeEntry> mobileCodes = new HashMap<>();
    private final Map<String, CodeEntry> emailCodes = new HashMap<>();
    String issueMobile(String mobile) { String code = code(); mobileCodes.put(mobile, new CodeEntry(code, LocalDateTime.now().plusMinutes(5))); return code; }
    String issueEmail(String email) { String code = code(); emailCodes.put(email, new CodeEntry(code, LocalDateTime.now().plusMinutes(5))); return code; }
    boolean verifyMobile(String mobile, String code) { return verify(mobileCodes, mobile, code); }
    boolean verifyEmail(String email, String code) { return verify(emailCodes, email, code); }
    private boolean verify(Map<String, CodeEntry> store, String key, String code) {
        CodeEntry entry = store.get(key);
        if (entry == null || entry.expiresAt().isBefore(LocalDateTime.now()) || !entry.code().equals(code)) return false;
        store.remove(key);
        return true;
    }
    private String code() { return String.valueOf(100000 + new Random().nextInt(900000)); }
}

enum Role { PATIENT, DOCTOR, ADMIN, PHARMACY, LABORATORY }
enum ApprovalStatus { PENDING, APPROVED, REJECTED, BLOCKED }
enum BookingStatus { PENDING, BOOKED, IN_PROGRESS, COMPLETED, CANCELLED }
enum DeliveryStatus { PRESCRIPTION_REVIEW, ORDER_PLACED, PACKED, OUT_FOR_DELIVERY, DELIVERED }

@MappedSuperclass
abstract class BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    LocalDateTime createdAt = LocalDateTime.now();
    LocalDateTime updatedAt = LocalDateTime.now();
    @PreUpdate void touch() { updatedAt = LocalDateTime.now(); }
}

@Entity @Table(name = "users", indexes = {@Index(name = "idx_users_role", columnList = "role"), @Index(name = "idx_users_status", columnList = "status")})
class User extends BaseEntity {
    @Column(name = "full_name", nullable = false) String name;
    @Column(unique = true) String email;
    @Column(unique = true, nullable = false) String mobileNumber;
    @Column(nullable = false) String password;
    @Enumerated(EnumType.STRING) @Column(nullable = false) Role role;
    @Enumerated(EnumType.STRING) ApprovalStatus status = ApprovalStatus.PENDING;
    boolean emailVerified;
    boolean mobileVerified;
    boolean enabled = true;
}

@Entity @Table(name = "patients")
class Patient extends BaseEntity {
    @OneToOne(optional = false) User user;
    int age;
    String gender;
    String allergyDetails = "No";
    String nearDearContactNumber;
    String diseaseDetails = "No";
}

@Entity @Table(name = "doctors")
class Doctor extends BaseEntity {
    @OneToOne(optional = false) User user;
    int age;
    int experience;
    String speciality;
    String address;
    @Column(unique = true) String nmcId;
}

@Entity @Table(name = "laboratories")
class Laboratory extends BaseEntity {
    @OneToOne(optional = false) User user;
    String labName;
    String address;
}

@Entity @Table(name = "pharmacies")
class Pharmacy extends BaseEntity {
    @OneToOne(optional = false) User user;
    String licenseNumber;
    String address;
}

@Entity @Table(name = "appointment_slots", uniqueConstraints = @UniqueConstraint(columnNames = {"doctor_id", "slot_start"}))
class AppointmentSlot extends BaseEntity {
    @ManyToOne(optional = false) Doctor doctor;
    @Column(name = "slot_start")
    LocalDateTime slotStart;
    @Column(name = "slot_end")
    LocalDateTime slotEnd;
    boolean booked;
}

@Entity @Table(name = "appointments")
class Appointment extends BaseEntity {
    @ManyToOne(optional = false) Patient patient;
    @ManyToOne(optional = false) Doctor doctor;
    @OneToOne AppointmentSlot slot;
    String mode;
    String reason;
    @Enumerated(EnumType.STRING) BookingStatus status = BookingStatus.BOOKED;
    String meetingLink;
}

@Entity @Table(name = "lab_tests")
class LabTest extends BaseEntity {
    @ManyToOne(optional = false) Laboratory laboratory;
    String testName;
    double price;
    String instructions;
    Boolean active = true;
}

@Entity @Table(name = "lab_bookings")
class LabBooking extends BaseEntity {
    @ManyToOne(optional = false) Patient patient;
    @ManyToOne(optional = false) LabTest test;
    @ManyToOne Doctor doctor;
    String sampleCollector;
    String samplePatientName;
    Integer samplePatientAge;
    String samplePatientGender;
    String collectionAddress;
    @Enumerated(EnumType.STRING) BookingStatus status = BookingStatus.PENDING;
    String reportPath;
}

@Entity @Table(name = "prescriptions")
class Prescription extends BaseEntity {
    @ManyToOne(optional = false) Patient patient;
    @ManyToOne Doctor doctor;
    String filePath;
    String notes;
}

@Entity @Table(name = "reports")
class Report extends BaseEntity {
    @ManyToOne(optional = false) Patient patient;
    @ManyToOne Doctor doctor;
    @ManyToOne Laboratory laboratory;
    String reportType;
    String filePath;
}

@Entity @Table(name = "medicines")
class Medicine extends BaseEntity {
    @ManyToOne(optional = false) Pharmacy pharmacy;
    String name;
    double price;
    int stock;
    String imagePath;
}

@Entity @Table(name = "medicine_orders")
class MedicineOrder extends BaseEntity {
    @ManyToOne(optional = false) Patient patient;
    @ManyToOne Pharmacy pharmacy;
    @ManyToOne Prescription prescription;
    double totalAmount;
    String deliveryAddress;
    @Enumerated(EnumType.STRING) DeliveryStatus deliveryStatus = DeliveryStatus.PRESCRIPTION_REVIEW;
}

@Entity @Table(name = "smartwatch_data", indexes = @Index(name = "idx_watch_patient_time", columnList = "patient_id,recorded_at"))
class SmartwatchData extends BaseEntity {
    @ManyToOne(optional = false) Patient patient;
    int systolicBp;
    int diastolicBp;
    int pulse;
    int heartRate;
    int oxygenLevel;
    double sleepHours;
    @Column(name = "recorded_at") LocalDateTime recordedAt = LocalDateTime.now();
}

@Entity @Table(name = "emergency_alerts")
class EmergencyAlert extends BaseEntity {
    @ManyToOne(optional = false) Patient patient;
    @ManyToOne Doctor doctor;
    String alertType;
    String message;
    boolean smsSent;
    boolean doctorNotified;
}

@Entity @Table(name = "chatbot_history")
class ChatbotHistory extends BaseEntity {
    @ManyToOne(optional = false) Patient patient;
    @Column(length = 2000) String prompt;
    @Column(length = 4000) String response;
}

@Entity @Table(name = "payments")
class Payment extends BaseEntity {
    @ManyToOne User user;
    String referenceType;
    Long referenceId;
    double amount;
    String status;
}

@Entity @Table(name = "notifications")
class Notification extends BaseEntity {
    @ManyToOne User user;
    String title;
    String message;
    boolean readFlag;
}

interface UserRepository extends JpaRepository<User, Long> { Optional<User> findByMobileNumber(String mobileNumber); Optional<User> findByEmail(String email); long countByRole(Role role); }
interface PatientRepository extends JpaRepository<Patient, Long> { Optional<Patient> findByUser(User user); }
interface DoctorRepository extends JpaRepository<Doctor, Long> { Optional<Doctor> findByUser(User user); Optional<Doctor> findByNmcId(String nmcId); List<Doctor> findBySpecialityContainingIgnoreCaseAndAddressContainingIgnoreCase(String speciality, String address); }
interface LaboratoryRepository extends JpaRepository<Laboratory, Long> { Optional<Laboratory> findByUser(User user); List<Laboratory> findByAddressContainingIgnoreCase(String address); }
interface PharmacyRepository extends JpaRepository<Pharmacy, Long> { Optional<Pharmacy> findByUser(User user); }
interface SlotRepository extends JpaRepository<AppointmentSlot, Long> { List<AppointmentSlot> findByDoctorIdAndBookedFalse(Long doctorId); }
interface AppointmentRepository extends JpaRepository<Appointment, Long> { List<Appointment> findByPatientId(Long id); List<Appointment> findByDoctorId(Long id); long countByStatus(BookingStatus status); }
interface LabTestRepository extends JpaRepository<LabTest, Long> {
    @Query("select t from LabTest t where t.laboratory.id = ?1 and (t.active is null or t.active = true)")
    List<LabTest> findActiveByLaboratoryId(Long id);
    @Query("select t from LabTest t where t.active is null or t.active = true")
    List<LabTest> findAllActive();
}
interface LabBookingRepository extends JpaRepository<LabBooking, Long> { List<LabBooking> findByPatientId(Long id); boolean existsByTestId(Long id); }
interface PrescriptionRepository extends JpaRepository<Prescription, Long> { List<Prescription> findByPatientId(Long id); }
interface ReportRepository extends JpaRepository<Report, Long> { List<Report> findByPatientId(Long id); }
interface MedicineRepository extends JpaRepository<Medicine, Long> { List<Medicine> findByPharmacyId(Long id); }
interface MedicineOrderRepository extends JpaRepository<MedicineOrder, Long> { List<MedicineOrder> findByPatientId(Long id); List<MedicineOrder> findByPharmacyId(Long id); }
interface SmartwatchRepository extends JpaRepository<SmartwatchData, Long> { List<SmartwatchData> findTop20ByPatientIdOrderByRecordedAtDesc(Long id); }
interface EmergencyAlertRepository extends JpaRepository<EmergencyAlert, Long> { List<EmergencyAlert> findTop20ByOrderByCreatedAtDesc(); }
interface ChatbotRepository extends JpaRepository<ChatbotHistory, Long> { List<ChatbotHistory> findTop20ByPatientIdOrderByCreatedAtDesc(Long id); }
interface PaymentRepository extends JpaRepository<Payment, Long> { @Query("select coalesce(sum(p.amount),0) from Payment p where p.status='SUCCESS'") double revenue(); }
interface NotificationRepository extends JpaRepository<Notification, Long> { List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId); }

record AuthRequest(@NotBlank String mobileNumber, @NotBlank String password) {}
record AuthResponse(String token, Role role, ApprovalStatus status, Long userId, String name) {}
record PatientSignup(@NotBlank String fullName, int age, String gender, String allergyDetails, @NotBlank String mobileNumber, @Email String email, String nearDearContactNumber, String diseaseDetails, @NotBlank String password, boolean mobileVerified, boolean emailVerified) {}
record DoctorSignup(@NotBlank String name, int age, int experience, String speciality, String address, @NotBlank String nmcId, @NotBlank String mobileNumber, @Email String email, @NotBlank String password, boolean mobileVerified, boolean emailVerified) {}
record PartnerSignup(@NotBlank String name, String licenseNumber, String address, @NotBlank String mobileNumber, @Email String email, @NotBlank String password, boolean mobileVerified, boolean emailVerified) {}
record SlotRequest(Long doctorId, LocalDateTime slotStart, LocalDateTime slotEnd) {}
record AppointmentRequest(Long patientId, Long doctorId, Long slotId, String mode, String reason) {}
record ProfileResponse(Long userId, Long profileId, Role role, String name) {}
record AppointmentView(Long id, String patientName, String doctorName, String speciality, String slot, String mode, String status, String meetingLink) {}
record DoctorSearchView(Long id, String name, String speciality, String city, int experience, String nmcId) {}
record LabBookingView(Long id, String patientName, String testName, String labName, String collectionAddress, String status, String bookedAt) {}
record LabTestView(Long id, String testName, double price, String instructions, String labName, String city) {}
record PharmacyOrderView(Long id, String patientName, String mobileNumber, String medicine, String deliveryAddress, double totalAmount, String status, String orderedAt) {}
record AdminUserView(Long id, String name, String email, String mobileNumber, Role role, ApprovalStatus status, boolean emailVerified, boolean mobileVerified) {}
record SmartwatchRequest(Long patientId, int systolicBp, int diastolicBp, int pulse, int heartRate, int oxygenLevel, double sleepHours) {}
record ChatRequest(Long patientId, String message) {}
record AdminApprovalItem(Long userId, Long profileId, Role role, ApprovalStatus status, String name, String email, String mobileNumber, String documentId, String speciality, String experience, String address, boolean emailVerified, boolean mobileVerified) {}
record PaymentRequest(Long userId, String referenceType, Long referenceId, double amount) {}
record RazorpayOrderRequest(Long userId, String referenceType, Long referenceId, double amount) {}
record RazorpayVerifyRequest(Long userId, String referenceType, Long referenceId, double amount, String razorpayOrderId, String razorpayPaymentId, String razorpaySignature) {}
record ForgotPasswordRequest(@Email String email) {}
record ResetPasswordRequest(@Email String email, String code, String newPassword) {}

@Service
class JwtService {
    private final SecretKey key;
    private final long expiry;
    JwtService(@Value("${app.jwt-secret}") String secret, @Value("${app.jwt-expiration-ms}") long expiry) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
        this.expiry = expiry;
    }
    String generate(User user) {
        return Jwts.builder().subject(user.mobileNumber).claim("role", user.role.name()).issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusMillis(expiry))).signWith(key).compact();
    }
    String subject(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload().getSubject();
    }
}

@Service
class AppUserDetailsService implements UserDetailsService {
    private final UserRepository users;
    AppUserDetailsService(UserRepository users) { this.users = users; }
    public UserDetails loadUserByUsername(String username) {
        User user = users.findByMobileNumber(username).orElseThrow();
        return new org.springframework.security.core.userdetails.User(
                user.mobileNumber, user.password, user.enabled,
                true, true, user.status != ApprovalStatus.BLOCKED,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.role.name())));
    }
}

@Component
class JwtFilter extends OncePerRequestFilter {
    private final JwtService jwt;
    private final AppUserDetailsService detailsService;
    JwtFilter(JwtService jwt, AppUserDetailsService detailsService) { this.jwt = jwt; this.detailsService = detailsService; }
    protected void doFilterInternal(jakarta.servlet.http.HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response, jakarta.servlet.FilterChain chain) throws IOException, jakarta.servlet.ServletException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String mobile = jwt.subject(header.substring(7));
            UserDetails userDetails = detailsService.loadUserByUsername(mobile);
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));
        }
        chain.doFilter(request, response);
    }
}

@Configuration @EnableMethodSecurity
class SecurityConfig {
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
    @Bean AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception { return configuration.getAuthenticationManager(); }
    @Bean CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:3000", "http://127.0.0.1:3000"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "Origin", "X-Requested-With"));
        config.setExposedHeaders(List.of("Authorization"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
    @Bean SecurityFilterChain security(HttpSecurity http, JwtFilter jwtFilter) throws Exception {
        return http.csrf(csrf -> csrf.disable()).cors(cors -> {})
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**", "/swagger-ui/**", "/v3/api-docs/**", "/ws/**").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class).build();
    }
}

@Configuration @EnableWebSocketMessageBroker
class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    public void registerStompEndpoints(StompEndpointRegistry registry) { registry.addEndpoint("/ws").setAllowedOriginPatterns("*").withSockJS(); }
    public void configureMessageBroker(MessageBrokerRegistry registry) { registry.enableSimpleBroker("/topic"); registry.setApplicationDestinationPrefixes("/app"); }
}

@Service
class AlertService {
    private final EmergencyAlertRepository alerts;
    private final PatientRepository patients;
    private final AppointmentRepository appointments;
    private final SimpMessagingTemplate socket;
    AlertService(EmergencyAlertRepository alerts, PatientRepository patients, AppointmentRepository appointments, SimpMessagingTemplate socket) {
        this.alerts = alerts; this.patients = patients; this.appointments = appointments; this.socket = socket;
    }
    void evaluate(SmartwatchData data) {
        boolean abnormal = data.systolicBp > 140 || data.systolicBp < 90 || data.pulse > 110 || data.pulse < 50 || data.oxygenLevel < 92;
        if (!abnormal) return;
        EmergencyAlert alert = new EmergencyAlert();
        alert.patient = data.patient;
        appointments.findByPatientId(data.patient.id).stream().findFirst().ifPresent(a -> alert.doctor = a.doctor);
        alert.alertType = "VITAL_ABNORMAL";
        alert.message = "Abnormal vitals: BP " + data.systolicBp + "/" + data.diastolicBp + ", pulse " + data.pulse + ", SpO2 " + data.oxygenLevel + "%";
        alert.smsSent = true;
        alert.doctorNotified = true;
        alerts.save(alert);
        socket.convertAndSend("/topic/emergency-alerts", Map.of("patientId", data.patient.id, "message", alert.message));
    }
}

@Service
class MailService {
    private final JavaMailSender mailSender;
    private final String from;
    MailService(JavaMailSender mailSender, @Value("${spring.mail.username:}") String from) {
        this.mailSender = mailSender; this.from = from;
    }
    void sendStatusMail(User user) {
        if (user.email == null || user.email.isBlank() || from == null || from.isBlank()) return;
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(user.email);
            message.setSubject("SmartHealthcare account " + user.status);
            message.setText("Hello " + user.name + ", your SmartHealthcare " + user.role + " account status is " + user.status + ".");
            mailSender.send(message);
        } catch (Exception ignored) {
            // Mail credentials are optional in local development.
        }
    }
    boolean sendVerificationCode(String email, String code) {
        if (email == null || email.isBlank() || from == null || from.isBlank()) return false;
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(email);
            message.setSubject("SmartHealthcare email verification code");
            message.setText("Your SmartHealthcare email verification code is: " + code);
            mailSender.send(message);
            return true;
        } catch (Exception ignored) {
            // Local development can still use the demo code returned by API.
            return false;
        }
    }
    boolean sendPasswordResetCode(String email, String code) {
        if (email == null || email.isBlank() || from == null || from.isBlank()) return false;
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(email);
            message.setSubject("SmartHealthcare password reset code");
            message.setText("Your SmartHealthcare password reset code is: " + code + ". It expires in 5 minutes.");
            mailSender.send(message);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }
}

@Service
class FileStorageService {
    private final Path root;
    FileStorageService(@Value("${app.upload-dir}") String uploadDir) throws IOException {
        root = Path.of(uploadDir);
        Files.createDirectories(root.resolve("prescriptions"));
        Files.createDirectories(root.resolve("reports"));
        Files.createDirectories(root.resolve("products"));
    }
    String store(MultipartFile file, String folder) throws IOException {
        if (file.isEmpty() || !Objects.requireNonNull(file.getOriginalFilename()).toLowerCase().endsWith(".pdf") && !folder.equals("products")) {
            throw new IllegalArgumentException("Only PDF uploads are accepted here.");
        }
        String safeName = UUID.randomUUID() + "-" + file.getOriginalFilename().replaceAll("[^a-zA-Z0-9._-]", "_");
        Path target = root.resolve(folder).resolve(safeName);
        Files.copy(file.getInputStream(), target);
        return target.toString();
    }
}

@RestControllerAdvice
class ApiErrors {
    @ExceptionHandler(Exception.class) ResponseEntity<Map<String, String>> error(Exception e) {
        HttpStatus status = e instanceof NoSuchElementException ? HttpStatus.NOT_FOUND : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(Map.of("error", e.getMessage() == null ? "Request failed" : e.getMessage()));
    }
}

@RestController @RequestMapping("/api/auth")
class AuthController {
    private final UserRepository users; private final PatientRepository patients; private final DoctorRepository doctors; private final LaboratoryRepository labs; private final PharmacyRepository pharmacies;
    private final PasswordEncoder encoder; private final AuthenticationManager auth; private final JwtService jwt;
    private final VerificationCodeStore codes; private final MailService mailService;
    AuthController(UserRepository users, PatientRepository patients, DoctorRepository doctors, LaboratoryRepository labs, PharmacyRepository pharmacies, PasswordEncoder encoder, AuthenticationManager auth, JwtService jwt, VerificationCodeStore codes, MailService mailService) {
        this.users = users; this.patients = patients; this.doctors = doctors; this.labs = labs; this.pharmacies = pharmacies; this.encoder = encoder; this.auth = auth; this.jwt = jwt;
        this.codes = codes; this.mailService = mailService;
    }
    @PostMapping("/login") AuthResponse login(@Valid @RequestBody AuthRequest request) {
        auth.authenticate(new UsernamePasswordAuthenticationToken(request.mobileNumber(), request.password()));
        User user = users.findByMobileNumber(request.mobileNumber()).orElseThrow();
        return new AuthResponse(jwt.generate(user), user.role, user.status, user.id, user.name);
    }
    @PostMapping("/forgot-password") Map<String, Object> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        User user = users.findByEmail(request.email()).orElseThrow(() -> new IllegalArgumentException("No account found with this email."));
        String code = codes.issueEmail(user.email);
        boolean mailSent = mailService.sendPasswordResetCode(user.email, code);
        return Map.of("message", mailSent ? "Password reset code sent." : "Email SMTP failed. Use demo code shown for local development.", "mailSent", mailSent, "demoCode", code);
    }
    @PostMapping("/reset-password") Map<String, Object> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        if (!codes.verifyEmail(request.email(), request.code())) throw new IllegalArgumentException("Invalid or expired reset code.");
        User user = users.findByEmail(request.email()).orElseThrow();
        user.password = encoder.encode(request.newPassword());
        users.save(user);
        return Map.of("message", "Password reset successfully.");
    }
    @PostMapping("/patient/register") AuthResponse patient(@Valid @RequestBody PatientSignup r) {
        requireVerified(r.mobileVerified(), r.emailVerified());
        User u = baseUser(r.fullName(), r.email(), r.mobileNumber(), r.password(), Role.PATIENT, ApprovalStatus.APPROVED, true, true);
        Patient p = new Patient(); p.user = u; p.age = r.age(); p.gender = r.gender(); p.allergyDetails = blankNo(r.allergyDetails()); p.nearDearContactNumber = r.nearDearContactNumber(); p.diseaseDetails = blankNo(r.diseaseDetails());
        patients.save(p); return new AuthResponse(jwt.generate(u), u.role, u.status, u.id, u.name);
    }
    @PostMapping("/doctor/register") Map<String, Object> doctor(@Valid @RequestBody DoctorSignup r) {
        requireVerified(r.mobileVerified(), r.emailVerified());
        User u = baseUser(r.name(), r.email(), r.mobileNumber(), r.password(), Role.DOCTOR, ApprovalStatus.PENDING, true, true);
        Doctor d = new Doctor(); d.user = u; d.age = r.age(); d.experience = r.experience(); d.speciality = r.speciality(); d.address = r.address(); d.nmcId = r.nmcId(); doctors.save(d);
        return Map.of("message", "Doctor registration submitted for admin NMC verification.", "status", u.status);
    }
    @PostMapping("/laboratory/register") Map<String, Object> lab(@Valid @RequestBody PartnerSignup r) {
        requireVerified(r.mobileVerified(), r.emailVerified());
        User u = baseUser(r.name(), r.email(), r.mobileNumber(), r.password(), Role.LABORATORY, ApprovalStatus.PENDING, true, true);
        Laboratory lab = new Laboratory(); lab.user = u; lab.labName = r.name(); lab.address = r.address(); labs.save(lab);
        return Map.of("message", "Laboratory registration submitted for approval.", "status", u.status);
    }
    @PostMapping("/pharmacy/register") Map<String, Object> pharmacy(@Valid @RequestBody PartnerSignup r) {
        requireVerified(r.mobileVerified(), r.emailVerified());
        User u = baseUser(r.name(), r.email(), r.mobileNumber(), r.password(), Role.PHARMACY, ApprovalStatus.PENDING, true, true);
        Pharmacy p = new Pharmacy(); p.user = u; p.licenseNumber = r.licenseNumber(); p.address = r.address(); pharmacies.save(p);
        return Map.of("message", "Pharmacy registration submitted for approval.", "status", u.status);
    }
    @PostMapping("/otp/send") Map<String, Object> sendOtp(@RequestParam String mobileNumber) {
        return Map.of("message", "OTP sent successfully.", "demoOtp", codes.issueMobile(mobileNumber));
    }
    @PostMapping("/otp/verify") Map<String, Object> otp(@RequestParam String mobileNumber, @RequestParam String otp) {
        boolean verified = codes.verifyMobile(mobileNumber, otp);
        users.findByMobileNumber(mobileNumber).ifPresent(user -> { user.mobileVerified = verified; users.save(user); });
        return Map.of("verified", verified, "demoOtp", "123456");
    }
    @PostMapping("/email/send") Map<String, Object> sendEmail(@RequestParam String email) {
        String code = codes.issueEmail(email);
        boolean mailSent = mailService.sendVerificationCode(email, code);
        return Map.of("message", mailSent ? "Email verification code sent." : "Email SMTP not configured or failed. Use demo code in local development.", "mailSent", mailSent, "demoCode", code);
    }
    @PostMapping("/email/verify") Map<String, Object> email(@RequestParam String email, @RequestParam String code) {
        boolean verified = codes.verifyEmail(email, code);
        users.findByEmail(email).ifPresent(user -> { user.emailVerified = verified; users.save(user); });
        return Map.of("verified", verified, "demoCode", "123456");
    }
    private void requireVerified(boolean mobileVerified, boolean emailVerified) {
        if (!mobileVerified) throw new IllegalArgumentException("Mobile OTP verification is required before signup.");
        if (!emailVerified) throw new IllegalArgumentException("Email verification is required before signup.");
    }
    private User baseUser(String name, String email, String mobile, String password, Role role, ApprovalStatus status, boolean mobileVerified, boolean emailVerified) {
        users.findByMobileNumber(mobile).ifPresent(existing -> {
            throw new IllegalArgumentException("Mobile number already registered. Please login or use another mobile number.");
        });
        if (email != null && !email.isBlank()) {
            users.findByEmail(email).ifPresent(existing -> {
                throw new IllegalArgumentException("Email already registered. Please login or use another email.");
            });
        }
        User u = new User(); u.name = name; u.email = email; u.mobileNumber = mobile; u.password = encoder.encode(password); u.role = role; u.status = status; u.mobileVerified = mobileVerified; u.emailVerified = emailVerified; return users.save(u);
    }
    private String blankNo(String value) { return value == null || value.isBlank() ? "No" : value; }
}

@RestController @RequestMapping("/api/me")
class MeController {
    private final UserRepository users; private final PatientRepository patients; private final DoctorRepository doctors; private final LaboratoryRepository labs; private final PharmacyRepository pharmacies;
    MeController(UserRepository users, PatientRepository patients, DoctorRepository doctors, LaboratoryRepository labs, PharmacyRepository pharmacies) {
        this.users = users; this.patients = patients; this.doctors = doctors; this.labs = labs; this.pharmacies = pharmacies;
    }
    @GetMapping("/profile") ProfileResponse profile(Authentication authentication) {
        User user = users.findByMobileNumber(authentication.getName()).orElseThrow();
        Long profileId = switch (user.role) {
            case PATIENT -> patients.findByUser(user).map(p -> p.id).orElse(null);
            case DOCTOR -> doctors.findByUser(user).map(d -> d.id).orElse(null);
            case LABORATORY -> labs.findByUser(user).map(l -> l.id).orElse(null);
            case PHARMACY -> pharmacies.findByUser(user).map(p -> p.id).orElse(null);
            case ADMIN -> user.id;
        };
        return new ProfileResponse(user.id, profileId, user.role, user.name);
    }
}

@RestController @RequestMapping("/api/patient")
class PatientController {
    private final PatientRepository patients; private final DoctorRepository doctors; private final SlotRepository slots; private final AppointmentRepository appointments; private final SmartwatchRepository watches; private final AlertService alerts;
    private final LabTestRepository tests; private final LabBookingRepository labBookings; private final PrescriptionRepository prescriptions; private final ReportRepository reports; private final MedicineOrderRepository orders; private final FileStorageService files; private final PharmacyRepository pharmacies;
    PatientController(PatientRepository patients, DoctorRepository doctors, SlotRepository slots, AppointmentRepository appointments, SmartwatchRepository watches, AlertService alerts, LabTestRepository tests, LabBookingRepository labBookings, PrescriptionRepository prescriptions, ReportRepository reports, MedicineOrderRepository orders, FileStorageService files, PharmacyRepository pharmacies) {
        this.patients = patients; this.doctors = doctors; this.slots = slots; this.appointments = appointments; this.watches = watches; this.alerts = alerts; this.tests = tests; this.labBookings = labBookings; this.prescriptions = prescriptions; this.reports = reports; this.orders = orders; this.files = files;
        this.pharmacies = pharmacies;
    }
    @GetMapping("/{id}/dashboard") Map<String, Object> dashboard(@PathVariable Long id) {
        return Map.of("vitals", watches.findTop20ByPatientIdOrderByRecordedAtDesc(id), "appointments", appointments.findByPatientId(id), "labReports", reports.findByPatientId(id), "prescriptions", prescriptions.findByPatientId(id), "orders", orders.findByPatientId(id));
    }
    @GetMapping("/doctors/search") List<DoctorSearchView> doctors(@RequestParam(defaultValue = "") String speciality, @RequestParam(defaultValue = "") String city) {
        return doctors.findBySpecialityContainingIgnoreCaseAndAddressContainingIgnoreCase(speciality, city).stream()
                .filter(d -> d.user.status == ApprovalStatus.APPROVED)
                .map(d -> new DoctorSearchView(d.id, d.user.name, d.speciality, d.address, d.experience, d.nmcId))
                .toList();
    }
    @GetMapping("/appointments")
    List<AppointmentView> patientAppointments(@RequestParam Long patientId) {
        return appointments.findByPatientId(patientId).stream().map(PatientController::view).toList();
    }
    @PatchMapping("/appointments/{id}/cancel") AppointmentView cancelAppointment(@PathVariable Long id) {
        Appointment appointment = appointments.findById(id).orElseThrow();
        appointment.status = BookingStatus.CANCELLED;
        return view(appointments.save(appointment));
    }
    @PatchMapping("/appointments/{id}/reschedule") AppointmentView rescheduleAppointment(@PathVariable Long id, @RequestParam String slotText) {
        Appointment appointment = appointments.findById(id).orElseThrow();
        appointment.reason = (appointment.reason == null ? "" : appointment.reason + " | ") + "Rescheduled to " + slotText;
        appointment.status = BookingStatus.BOOKED;
        return view(appointments.save(appointment));
    }
    @GetMapping("/lab-bookings")
    List<LabBookingView> patientLabBookings(@RequestParam Long patientId) {
        return labBookings.findByPatientId(patientId).stream().map(PatientController::labView).toList();
    }
    @PatchMapping("/lab-bookings/{id}/cancel") LabBookingView cancelLabBooking(@PathVariable Long id) {
        LabBooking booking = labBookings.findById(id).orElseThrow();
        booking.status = BookingStatus.CANCELLED;
        return labView(labBookings.save(booking));
    }
    @PostMapping("/appointments") Appointment book(@Valid @RequestBody AppointmentRequest r) {
        AppointmentSlot slot = r.slotId() == null ? createGeneratedSlot(r.doctorId()) : slots.findById(r.slotId()).orElseGet(() -> createGeneratedSlot(r.doctorId()));
        if (slot.booked) throw new IllegalStateException("Slot already booked.");
        Appointment a = new Appointment(); a.patient = patients.findById(r.patientId()).orElseThrow(); a.doctor = doctors.findById(r.doctorId()).orElseThrow(); a.slot = slot; a.mode = r.mode(); a.reason = r.reason(); a.meetingLink = "https://meet.jit.si/smarthealthcare-" + UUID.randomUUID();
        slot.booked = true; slots.save(slot); return appointments.save(a);
    }
    private AppointmentSlot createGeneratedSlot(Long doctorId) {
            Doctor doctor = doctors.findById(doctorId).orElseThrow();
            AppointmentSlot generated = new AppointmentSlot();
            generated.doctor = doctor;
            generated.slotStart = LocalDateTime.now().plusDays(1).plusMinutes(UUID.randomUUID().getLeastSignificantBits() & 255).withSecond(0).withNano(0);
            generated.slotEnd = generated.slotStart.plusMinutes(30);
            return slots.save(generated);
    }
    private static AppointmentView view(Appointment a) {
        String slot = a.slot == null ? "Slot pending" : a.slot.slotStart.toString().replace('T', ' ');
        return new AppointmentView(a.id, a.patient.user.name, a.doctor.user.name, a.doctor.speciality, slot, a.mode, a.status.name(), a.meetingLink);
    }
    private static LabBookingView labView(LabBooking b) {
        return new LabBookingView(b.id, b.samplePatientName == null ? b.patient.user.name : b.samplePatientName, b.test.testName, b.test.laboratory.labName, b.collectionAddress, b.status.name(), b.createdAt.toString().replace('T', ' '));
    }
    @PostMapping("/smartwatch/connect") Map<String, Object> connect(@RequestParam Long patientId, @RequestParam(defaultValue = "Google Fit") String provider) { return Map.of("connected", true, "provider", provider, "patientId", patientId); }
    @PostMapping("/smartwatch/data") SmartwatchData vitals(@RequestBody SmartwatchRequest r) {
        SmartwatchData d = new SmartwatchData(); d.patient = patients.findById(r.patientId()).orElseThrow(); d.systolicBp = r.systolicBp(); d.diastolicBp = r.diastolicBp(); d.pulse = r.pulse(); d.heartRate = r.heartRate(); d.oxygenLevel = r.oxygenLevel(); d.sleepHours = r.sleepHours();
        SmartwatchData saved = watches.save(d); alerts.evaluate(saved); return saved;
    }
    @GetMapping("/lab-tests") List<LabTestView> tests() { return tests.findAllActive().stream().map(PatientController::labTestView).toList(); }
    @PostMapping("/lab-bookings") LabBooking labBooking(@RequestParam Long patientId, @RequestParam Long testId, @RequestParam(required = false) Long doctorId, @RequestParam String samplePatientName, @RequestParam Integer samplePatientAge, @RequestParam String samplePatientGender, @RequestParam String collectionAddress) {
        if (collectionAddress == null || collectionAddress.isBlank()) throw new IllegalArgumentException("Sample collection address is required.");
        LabBooking b = new LabBooking(); b.patient = patients.findById(patientId).orElseThrow(); b.test = tests.findById(testId).orElseThrow(); b.sampleCollector = "Auto-assigned collector"; b.samplePatientName = samplePatientName; b.samplePatientAge = samplePatientAge; b.samplePatientGender = samplePatientGender; b.collectionAddress = collectionAddress; return labBookings.save(b);
    }
    @PostMapping(value = "/prescriptions/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE) Prescription uploadPrescription(@RequestParam Long patientId, @RequestParam(required = false) Long doctorId, @RequestPart MultipartFile file) throws IOException {
        Prescription p = new Prescription(); p.patient = patients.findById(patientId).orElseThrow(); doctorIdOptional(doctorId).ifPresent(d -> p.doctor = d); p.filePath = files.store(file, "prescriptions"); return prescriptions.save(p);
    }
    @PostMapping("/medicine-orders") MedicineOrder medicineOrder(@RequestParam Long patientId, @RequestParam double totalAmount, @RequestParam String deliveryAddress) {
        if (deliveryAddress == null || deliveryAddress.isBlank()) throw new IllegalArgumentException("Delivery address is required.");
        MedicineOrder order = new MedicineOrder();
        order.patient = patients.findById(patientId).orElseThrow();
        order.pharmacy = pharmacies.findAll().stream().findFirst().orElseThrow();
        order.totalAmount = totalAmount;
        order.deliveryAddress = deliveryAddress;
        order.deliveryStatus = DeliveryStatus.ORDER_PLACED;
        return orders.save(order);
    }
    static LabTestView labTestView(LabTest t) { return new LabTestView(t.id, t.testName, t.price, t.instructions, t.laboratory.labName, t.laboratory.address); }
    private Optional<Doctor> doctorIdOptional(Long id) { return id == null ? Optional.empty() : doctors.findById(id); }
}

@RestController @RequestMapping("/api/doctor")
class DoctorController {
    private final DoctorRepository doctors; private final PatientRepository patients; private final SlotRepository slots; private final AppointmentRepository appointments; private final PrescriptionRepository prescriptions; private final LabTestRepository tests; private final ReportRepository reports; private final FileStorageService files; private final EmergencyAlertRepository alerts;
    DoctorController(DoctorRepository doctors, PatientRepository patients, SlotRepository slots, AppointmentRepository appointments, PrescriptionRepository prescriptions, LabTestRepository tests, ReportRepository reports, FileStorageService files, EmergencyAlertRepository alerts) {
        this.doctors = doctors; this.patients = patients; this.slots = slots; this.appointments = appointments; this.prescriptions = prescriptions; this.tests = tests; this.reports = reports; this.files = files; this.alerts = alerts;
    }
    @GetMapping("/{id}/dashboard") Map<String, Object> dashboard(@PathVariable Long id) { return Map.of("appointments", doctorAppointmentViews(id), "emergencyAlerts", alerts.findTop20ByOrderByCreatedAtDesc(), "prescriptions", prescriptions.findAll()); }
    @GetMapping("/{id}/appointments") List<AppointmentView> doctorAppointmentViews(@PathVariable Long id) { return appointments.findByDoctorId(id).stream().map(DoctorController::view).toList(); }
    private static AppointmentView view(Appointment a) {
        String slot = a.slot == null ? "Slot pending" : a.slot.slotStart.toString().replace('T', ' ');
        return new AppointmentView(a.id, a.patient.user.name, a.doctor.user.name, a.doctor.speciality, slot, a.mode, a.status.name(), a.meetingLink);
    }
    @PostMapping("/slots") AppointmentSlot slot(@RequestBody SlotRequest r) { AppointmentSlot s = new AppointmentSlot(); s.doctor = doctors.findById(r.doctorId()).orElseThrow(); s.slotStart = r.slotStart(); s.slotEnd = r.slotEnd(); return slots.save(s); }
    @GetMapping("/{doctorId}/slots") List<AppointmentSlot> slots(@PathVariable Long doctorId) { return slots.findByDoctorIdAndBookedFalse(doctorId); }
    @PostMapping(value = "/prescriptions/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE) Prescription prescription(@RequestParam Long doctorId, @RequestParam Long patientId, @RequestParam String notes, @RequestPart MultipartFile file) throws IOException {
        Prescription p = new Prescription(); p.doctor = doctors.findById(doctorId).orElseThrow(); p.patient = patients.findById(patientId).orElseThrow(); p.notes = notes; p.filePath = files.store(file, "prescriptions"); return prescriptions.save(p);
    }
}

@RestController @RequestMapping("/api/labs")
class LabController {
    private final LaboratoryRepository labs; private final LabTestRepository tests; private final LabBookingRepository bookings; private final PatientRepository patients; private final DoctorRepository doctors; private final ReportRepository reports; private final FileStorageService files;
    LabController(LaboratoryRepository labs, LabTestRepository tests, LabBookingRepository bookings, PatientRepository patients, DoctorRepository doctors, ReportRepository reports, FileStorageService files) { this.labs = labs; this.tests = tests; this.bookings = bookings; this.patients = patients; this.doctors = doctors; this.reports = reports; this.files = files; }
    @GetMapping("/search") List<Laboratory> search(@RequestParam(defaultValue = "") String city) { return labs.findByAddressContainingIgnoreCase(city); }
    @PostMapping("/{labId}/tests") LabTestView add(@PathVariable Long labId, @RequestBody Map<String, String> body) { LabTest t = new LabTest(); t.laboratory = labs.findById(labId).orElseThrow(); t.testName = body.get("testName"); t.price = Double.parseDouble(body.getOrDefault("price", "0")); t.instructions = body.get("instructions"); return PatientController.labTestView(tests.save(t)); }
    @GetMapping("/{labId}/tests") List<LabTestView> list(@PathVariable Long labId) { return tests.findActiveByLaboratoryId(labId).stream().map(PatientController::labTestView).toList(); }
    @PutMapping("/tests/{testId}") LabTestView update(@PathVariable Long testId, @RequestBody Map<String, String> body) {
        LabTest t = tests.findById(testId).orElseThrow();
        t.testName = body.getOrDefault("testName", t.testName);
        t.price = Double.parseDouble(body.getOrDefault("price", String.valueOf(t.price)));
        t.instructions = body.getOrDefault("instructions", t.instructions);
        return PatientController.labTestView(tests.save(t));
    }
    @DeleteMapping("/tests/{testId}") void delete(@PathVariable Long testId) {
        if (bookings.existsByTestId(testId)) {
            LabTest t = tests.findById(testId).orElseThrow();
            t.active = false;
            tests.save(t);
            return;
        }
        tests.deleteById(testId);
    }
    @GetMapping("/bookings") List<LabBooking> bookings() { return bookings.findAll(); }
    @PostMapping(value = "/reports/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE) Report report(@RequestParam Long labId, @RequestParam Long patientId, @RequestParam(required = false) Long doctorId, @RequestParam String reportType, @RequestPart MultipartFile file) throws IOException {
        Report r = new Report(); r.laboratory = labs.findById(labId).orElseThrow(); r.patient = patients.findById(patientId).orElseThrow(); if (doctorId != null) r.doctor = doctors.findById(doctorId).orElse(null); r.reportType = reportType; r.filePath = files.store(file, "reports"); return reports.save(r);
    }
}

@RestController @RequestMapping("/api/pharmacy")
class PharmacyController {
    private final PharmacyRepository pharmacies; private final MedicineRepository medicines; private final MedicineOrderRepository orders; private final PatientRepository patients; private final PrescriptionRepository prescriptions;
    PharmacyController(PharmacyRepository pharmacies, MedicineRepository medicines, MedicineOrderRepository orders, PatientRepository patients, PrescriptionRepository prescriptions) { this.pharmacies = pharmacies; this.medicines = medicines; this.orders = orders; this.patients = patients; this.prescriptions = prescriptions; }
    @PostMapping("/{pharmacyId}/medicines") Medicine medicine(@PathVariable Long pharmacyId, @RequestBody Medicine m) { m.pharmacy = pharmacies.findById(pharmacyId).orElseThrow(); return medicines.save(m); }
    @GetMapping("/{pharmacyId}/medicines") List<Medicine> medicines(@PathVariable Long pharmacyId) { return medicines.findByPharmacyId(pharmacyId); }
    @GetMapping("/{pharmacyId}/orders") List<PharmacyOrderView> orders(@PathVariable Long pharmacyId) {
        return orders.findByPharmacyId(pharmacyId).stream().map(o -> new PharmacyOrderView(
                o.id,
                o.patient.user.name,
                o.patient.user.mobileNumber,
                "Medicine order",
                o.deliveryAddress,
                o.totalAmount,
                o.deliveryStatus.name(),
                o.createdAt.toString().replace('T', ' ')
        )).toList();
    }
    @PostMapping("/orders") MedicineOrder order(@RequestParam Long patientId, @RequestParam Long pharmacyId, @RequestParam(required = false) Long prescriptionId, @RequestParam double totalAmount) {
        MedicineOrder o = new MedicineOrder(); o.patient = patients.findById(patientId).orElseThrow(); o.pharmacy = pharmacies.findById(pharmacyId).orElseThrow(); if (prescriptionId != null) o.prescription = prescriptions.findById(prescriptionId).orElse(null); o.totalAmount = totalAmount; return orders.save(o);
    }
    @PatchMapping("/orders/{id}/status") MedicineOrder status(@PathVariable Long id, @RequestParam DeliveryStatus status) { MedicineOrder o = orders.findById(id).orElseThrow(); o.deliveryStatus = status; return orders.save(o); }
}

@RestController @RequestMapping("/api/admin")
class AdminController {
    private final UserRepository users; private final DoctorRepository doctors; private final PatientRepository patients; private final LaboratoryRepository labs; private final PharmacyRepository pharmacies; private final AppointmentRepository appointments; private final MedicineOrderRepository orders; private final EmergencyAlertRepository alerts; private final PaymentRepository payments;
    private final MailService mailService;
    AdminController(UserRepository users, DoctorRepository doctors, PatientRepository patients, LaboratoryRepository labs, PharmacyRepository pharmacies, AppointmentRepository appointments, MedicineOrderRepository orders, EmergencyAlertRepository alerts, PaymentRepository payments, MailService mailService) {
        this.users = users; this.doctors = doctors; this.patients = patients; this.labs = labs; this.pharmacies = pharmacies; this.appointments = appointments; this.orders = orders; this.alerts = alerts; this.payments = payments;
        this.mailService = mailService;
    }
    @GetMapping("/dashboard") Map<String, Object> dashboard() { return Map.of("totalUsers", users.count(), "doctors", users.countByRole(Role.DOCTOR), "patients", users.countByRole(Role.PATIENT), "labs", users.countByRole(Role.LABORATORY), "pharmacies", users.countByRole(Role.PHARMACY), "appointments", appointments.count(), "orders", orders.count(), "emergencyCases", alerts.count(), "revenue", payments.revenue()); }
    @GetMapping("/approvals") List<AdminApprovalItem> approvals() {
        List<AdminApprovalItem> queue = new ArrayList<>();
        doctors.findAll().forEach(d -> queue.add(new AdminApprovalItem(d.user.id, d.id, Role.DOCTOR, d.user.status, d.user.name, d.user.email, d.user.mobileNumber, d.nmcId, d.speciality, d.experience + " years", d.address, d.user.emailVerified, d.user.mobileVerified)));
        labs.findAll().forEach(l -> queue.add(new AdminApprovalItem(l.user.id, l.id, Role.LABORATORY, l.user.status, l.user.name, l.user.email, l.user.mobileNumber, "Email verification", null, null, l.address, l.user.emailVerified, l.user.mobileVerified)));
        pharmacies.findAll().forEach(p -> queue.add(new AdminApprovalItem(p.user.id, p.id, Role.PHARMACY, p.user.status, p.user.name, p.user.email, p.user.mobileNumber, p.licenseNumber, null, null, p.address, p.user.emailVerified, p.user.mobileVerified)));
        return queue;
    }
    @PatchMapping("/users/{id}/status") User status(@PathVariable Long id, @RequestParam ApprovalStatus status) {
        User u = users.findById(id).orElseThrow();
        u.status = status;
        u.enabled = status != ApprovalStatus.BLOCKED && status != ApprovalStatus.REJECTED;
        User saved = users.save(u);
        mailService.sendStatusMail(saved);
        return saved;
    }
    @GetMapping("/users") List<AdminUserView> users() {
        return users.findAll().stream()
                .map(u -> new AdminUserView(u.id, u.name, u.email, u.mobileNumber, u.role, u.status, u.emailVerified, u.mobileVerified))
                .toList();
    }
}

@RestController @RequestMapping("/api/payments")
class PaymentController {
    private final PaymentRepository payments; private final UserRepository users; private final RazorpayService razorpay;
    PaymentController(PaymentRepository payments, UserRepository users, RazorpayService razorpay) { this.payments = payments; this.users = users; this.razorpay = razorpay; }
    @PostMapping("/demo") Payment demo(@RequestBody PaymentRequest request) {
        Payment payment = new Payment();
        payment.user = users.findById(request.userId()).orElseThrow();
        payment.referenceType = request.referenceType();
        payment.referenceId = request.referenceId();
        payment.amount = request.amount();
        payment.status = "SUCCESS";
        return payments.save(payment);
    }
    @PostMapping("/razorpay/order") Map<String, Object> order(@RequestBody RazorpayOrderRequest request) {
        return razorpay.createOrder(request);
    }
    @PostMapping("/razorpay/verify") Payment verify(@RequestBody RazorpayVerifyRequest request) {
        if (!razorpay.verify(request.razorpayOrderId(), request.razorpayPaymentId(), request.razorpaySignature())) {
            throw new IllegalArgumentException("Razorpay payment signature verification failed.");
        }
        Payment payment = new Payment();
        payment.user = users.findById(request.userId()).orElseThrow();
        payment.referenceType = request.referenceType();
        payment.referenceId = request.referenceId();
        payment.amount = request.amount();
        payment.status = "SUCCESS";
        return payments.save(payment);
    }
}

@Service
class RazorpayService {
    private final String keyId;
    private final String keySecret;
    private final HttpClient client = HttpClient.newHttpClient();
    RazorpayService(@Value("${app.razorpay-key-id:}") String keyId, @Value("${app.razorpay-key-secret:}") String keySecret) {
        this.keyId = keyId; this.keySecret = keySecret;
    }
    Map<String, Object> createOrder(RazorpayOrderRequest request) {
        int paise = (int) Math.round(request.amount() * 100);
        if (keyId == null || keyId.isBlank() || keySecret == null || keySecret.isBlank()) {
            return Map.of("keyId", "rzp_test_demo", "orderId", "order_demo_" + UUID.randomUUID(), "amount", paise, "currency", "INR", "demo", true);
        }
        try {
            String auth = Base64.getEncoder().encodeToString((keyId + ":" + keySecret).getBytes(StandardCharsets.UTF_8));
            String body = "{\"amount\":" + paise + ",\"currency\":\"INR\",\"receipt\":\"" + request.referenceType() + "-" + request.referenceId() + "-" + System.currentTimeMillis() + "\"}";
            HttpRequest httpRequest = HttpRequest.newBuilder(URI.create("https://api.razorpay.com/v1/orders"))
                    .header("Authorization", "Basic " + auth)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            String orderId = response.body().replaceAll("(?s).*\"id\"\\s*:\\s*\"([^\"]+)\".*", "$1");
            if (response.statusCode() >= 300 || orderId.equals(response.body())) throw new IllegalStateException(response.body());
            return Map.of("keyId", keyId, "orderId", orderId, "amount", paise, "currency", "INR", "demo", false);
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to create Razorpay order: " + e.getMessage());
        }
    }
    boolean verify(String orderId, String paymentId, String signature) {
        if (orderId != null && orderId.startsWith("order_demo_")) return true;
        if (keySecret == null || keySecret.isBlank()) return false;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(keySecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal((orderId + "|" + paymentId).getBytes(StandardCharsets.UTF_8));
            StringBuilder expected = new StringBuilder();
            for (byte b : digest) expected.append(String.format("%02x", b));
            return expected.toString().equals(signature);
        } catch (Exception e) {
            return false;
        }
    }
}

@RestController @RequestMapping("/api/chatbot")
class ChatbotController {
    private final ChatbotRepository history; private final PatientRepository patients; private final OpenAiHealthService openAi;
    ChatbotController(ChatbotRepository history, PatientRepository patients, OpenAiHealthService openAi) { this.history = history; this.patients = patients; this.openAi = openAi; }
    @PostMapping("/ask") Map<String, Object> ask(@RequestBody ChatRequest r) {
        Patient patient = patients.findById(r.patientId()).orElseThrow();
        String disease = Optional.ofNullable(patient.diseaseDetails).orElse("").toLowerCase();
        String fallback = "I can share general wellness guidance, but please contact a doctor for diagnosis. ";
        if (disease.contains("asthma")) fallback += "For asthma: avoid smoke and dust, practice pursed-lip breathing, track wheezing, and seek emergency care for blue lips or severe breathlessness.";
        else if (disease.contains("diabetes")) fallback += "For diabetes: monitor sugar, favor high-fiber meals, hydrate, walk after meals, and watch for dizziness, confusion, or very high readings.";
        else fallback += "Based on your message, keep monitoring vitals, rest, hydrate, and use the emergency button if symptoms become severe.";
        String answer = openAi.answer(r.message(), patient.diseaseDetails, fallback);
        ChatbotHistory item = new ChatbotHistory(); item.patient = patient; item.prompt = r.message(); item.response = answer; history.save(item);
        return Map.of("response", answer, "openAiEnabled", openAi.enabled());
    }
    @GetMapping("/history/{patientId}") List<ChatbotHistory> history(@PathVariable Long patientId) { return history.findTop20ByPatientIdOrderByCreatedAtDesc(patientId); }
}

@Service
class OpenAiHealthService {
    private final String apiKey;
    private final String model;
    private final RestClient client = RestClient.create("https://api.openai.com/v1");
    OpenAiHealthService(@Value("${app.openai-api-key:}") String apiKey, @Value("${app.openai-model:gpt-4o-mini}") String model) {
        this.apiKey = apiKey; this.model = model;
    }
    boolean enabled() { return apiKey != null && !apiKey.isBlank(); }
    String answer(String message, String disease, String fallback) {
        if (!enabled()) return fallback;
        try {
            Map<String, Object> body = Map.of(
                    "model", model,
                    "messages", List.of(
                            Map.of("role", "system", "content", "You are a healthcare assistant. Give concise precautions, exercise suggestions, health tips, and emergency warning signs. Do not diagnose. Encourage professional care for urgent symptoms."),
                            Map.of("role", "user", "content", "Known disease: " + Optional.ofNullable(disease).orElse("Not specified") + "\nQuestion: " + message)
                    )
            );
            Map<?, ?> response = client.post().uri("/chat/completions").header("Authorization", "Bearer " + apiKey).body(body).retrieve().body(Map.class);
            List<?> choices = (List<?>) response.get("choices");
            Map<?, ?> first = (Map<?, ?>) choices.getFirst();
            Map<?, ?> msg = (Map<?, ?>) first.get("message");
            return String.valueOf(msg.get("content"));
        } catch (Exception ignored) {
            return fallback;
        }
    }
}
