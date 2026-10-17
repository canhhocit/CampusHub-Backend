package com.campushub.modules.locker;

import com.campushub.common.enums.Role;
import com.campushub.common.enums.UserStatus;
import com.campushub.modules.locker.dto.*;
import com.campushub.modules.locker.entity.Compartment;
import com.campushub.modules.locker.entity.CompartmentStatus;
import com.campushub.modules.locker.entity.Locker;
import com.campushub.modules.locker.entity.LockerStatus;
import com.campushub.modules.locker.repository.CompartmentRepository;
import com.campushub.modules.locker.repository.LockerRepository;
import com.campushub.modules.user.entity.User;
import com.campushub.modules.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@org.springframework.test.context.ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class LockerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private LockerRepository lockerRepository;

    @Autowired
    private CompartmentRepository compartmentRepository;

    @Autowired
    private UserRepository userRepository;

    private User adminUser;
    private User studentUser;

    @BeforeEach
    public void setup() {
        if (!userRepository.existsByUsername("admin_tester")) {
            adminUser = new User();
            adminUser.setUsername("admin_tester");
            adminUser.setEmail("admin_tester@test.com");
            adminUser.setPasswordHash("hash");
            adminUser.setFullName("Admin Tester");
            adminUser.setRole(Role.ADMIN);
            adminUser.setStatus(UserStatus.ACTIVE);
            adminUser = userRepository.save(adminUser);
        }

        if (!userRepository.existsByUsername("student_tester")) {
            studentUser = new User();
            studentUser.setUsername("student_tester");
            studentUser.setEmail("student_tester@test.com");
            studentUser.setPasswordHash("hash");
            studentUser.setFullName("Student Tester");
            studentUser.setRole(Role.STUDENT);
            studentUser.setStatus(UserStatus.ACTIVE);
            studentUser = userRepository.save(studentUser);
        }
    }

    @Test
    public void testAnonymousCannotAccessLockers() throws Exception {
        mockMvc.perform(get("/api/v1/lockers"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "admin_tester", roles = "ADMIN")
    public void testAdminCreateLockerSuccess() throws Exception {
        LockerCreateRequest req = new LockerCreateRequest();
        req.setCode("LK-TEST-1");
        req.setName("Test Locker 1");
        req.setLocation("Engineering Hall Floor 1");

        mockMvc.perform(post("/api/v1/lockers").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("LK-TEST-1"))
                .andExpect(jsonPath("$.name").value("Test Locker 1"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @WithMockUser(username = "admin_tester", roles = "ADMIN")
    public void testDuplicateLockerCodeRejected() throws Exception {
        Locker locker = new Locker();
        locker.setCode("LK-DUP-1");
        locker.setName("Original");
        locker.setLocation("Loc");
        locker.setStatus(LockerStatus.ACTIVE);
        lockerRepository.saveAndFlush(locker);

        LockerCreateRequest req = new LockerCreateRequest();
        req.setCode("LK-DUP-1");
        req.setName("Duplicate");
        req.setLocation("Other Loc");

        mockMvc.perform(post("/api/v1/lockers").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("LOCKER_CODE_ALREADY_EXISTS"));
    }

    @Test
    @WithMockUser(username = "student_tester", roles = "STUDENT")
    public void testStudentCannotCreateOrUpdateLocker() throws Exception {
        LockerCreateRequest createReq = new LockerCreateRequest();
        createReq.setCode("LK-HACK");
        createReq.setName("Hacked Locker");
        createReq.setLocation("Nowhere");

        // Create blocked
        mockMvc.perform(post("/api/v1/lockers").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isForbidden());

        // Update blocked
        Locker locker = new Locker();
        locker.setCode("LK-STUDENT-TEST");
        locker.setName("Locker");
        locker.setLocation("Loc");
        locker.setStatus(LockerStatus.ACTIVE);
        locker = lockerRepository.saveAndFlush(locker);

        LockerUpdateRequest updateReq = new LockerUpdateRequest();
        updateReq.setName("Updated By Student");
        updateReq.setLocation("New Loc");

        mockMvc.perform(put("/api/v1/lockers/" + locker.getId()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin_tester", roles = "ADMIN")
    public void testAdminCreateCompartmentSuccess() throws Exception {
        Locker locker = new Locker();
        locker.setCode("LK-COMP-1");
        locker.setName("Locker for Compartments");
        locker.setLocation("Loc");
        locker.setStatus(LockerStatus.ACTIVE);
        locker = lockerRepository.saveAndFlush(locker);

        CompartmentCreateRequest req = new CompartmentCreateRequest();
        req.setCode("CP-01");

        mockMvc.perform(post("/api/v1/lockers/" + locker.getId() + "/compartments").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("CP-01"))
                .andExpect(jsonPath("$.status").value("AVAILABLE"));
    }

    @Test
    @WithMockUser(username = "admin_tester", roles = "ADMIN")
    public void testDuplicateCompartmentCodeInSameLockerRejected() throws Exception {
        Locker locker = new Locker();
        locker.setCode("LK-COMP-DUP");
        locker.setName("Locker");
        locker.setLocation("Loc");
        locker.setStatus(LockerStatus.ACTIVE);
        locker = lockerRepository.saveAndFlush(locker);

        Compartment c1 = new Compartment();
        c1.setLocker(locker);
        c1.setCode("C01");
        c1.setStatus(CompartmentStatus.AVAILABLE);
        compartmentRepository.saveAndFlush(c1);

        CompartmentCreateRequest req = new CompartmentCreateRequest();
        req.setCode("C01");

        mockMvc.perform(post("/api/v1/lockers/" + locker.getId() + "/compartments").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("COMPARTMENT_CODE_ALREADY_EXISTS"));
    }

    @Test
    @WithMockUser(username = "admin_tester", roles = "ADMIN")
    public void testCompartmentCannotReferenceNonExistentLocker() throws Exception {
        CompartmentCreateRequest req = new CompartmentCreateRequest();
        req.setCode("C01");

        mockMvc.perform(post("/api/v1/lockers/999999/compartments").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("LOCKER_NOT_FOUND"));
    }

    @Test
    @WithMockUser(username = "student_tester", roles = "STUDENT")
    public void testStudentCannotChangeCompartmentStatus() throws Exception {
        Locker locker = new Locker();
        locker.setCode("LK-STATUS-GUARD");
        locker.setName("Guard Locker");
        locker.setLocation("Loc");
        locker.setStatus(LockerStatus.ACTIVE);
        locker = lockerRepository.saveAndFlush(locker);

        Compartment c = new Compartment();
        c.setLocker(locker);
        c.setCode("C10");
        c.setStatus(CompartmentStatus.AVAILABLE);
        c = compartmentRepository.saveAndFlush(c);

        CompartmentStatusUpdateRequest req = new CompartmentStatusUpdateRequest();
        req.setStatus(CompartmentStatus.OUT_OF_SERVICE);

        mockMvc.perform(patch("/api/v1/lockers/" + locker.getId() + "/compartments/" + c.getId() + "/status").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin_tester", roles = "ADMIN")
    public void testCannotManuallyFreeOccupiedOrReservedCompartment() throws Exception {
        Locker locker = new Locker();
        locker.setCode("LK-SAFE-TEST");
        locker.setName("Safety Check Locker");
        locker.setLocation("Loc");
        locker.setStatus(LockerStatus.ACTIVE);
        locker = lockerRepository.saveAndFlush(locker);

        Compartment occupied = new Compartment();
        occupied.setLocker(locker);
        occupied.setCode("OCC-01");
        occupied.setStatus(CompartmentStatus.OCCUPIED);
        occupied = compartmentRepository.saveAndFlush(occupied);

        Compartment reserved = new Compartment();
        reserved.setLocker(locker);
        reserved.setCode("RES-01");
        reserved.setStatus(CompartmentStatus.RESERVED);
        reserved = compartmentRepository.saveAndFlush(reserved);

        CompartmentStatusUpdateRequest req = new CompartmentStatusUpdateRequest();
        req.setStatus(CompartmentStatus.AVAILABLE);

        // Attempt to free OCCUPIED -> Rejected
        mockMvc.perform(patch("/api/v1/lockers/" + locker.getId() + "/compartments/" + occupied.getId() + "/status").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("UNSAFE_COMPARTMENT_STATUS_TRANSITION"));

        // Attempt to free RESERVED -> Rejected
        mockMvc.perform(patch("/api/v1/lockers/" + locker.getId() + "/compartments/" + reserved.getId() + "/status").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("UNSAFE_COMPARTMENT_STATUS_TRANSITION"));
    }

    @Test
    @WithMockUser(username = "admin_tester", roles = "ADMIN")
    public void testValidCompartmentStatusTransitions() throws Exception {
        Locker locker = new Locker();
        locker.setCode("LK-VALID-STATUS");
        locker.setName("Locker");
        locker.setLocation("Loc");
        locker.setStatus(LockerStatus.ACTIVE);
        locker = lockerRepository.saveAndFlush(locker);

        Compartment c = new Compartment();
        c.setLocker(locker);
        c.setCode("C20");
        c.setStatus(CompartmentStatus.AVAILABLE);
        c = compartmentRepository.saveAndFlush(c);

        // AVAILABLE -> OUT_OF_SERVICE
        CompartmentStatusUpdateRequest req1 = new CompartmentStatusUpdateRequest();
        req1.setStatus(CompartmentStatus.OUT_OF_SERVICE);

        mockMvc.perform(patch("/api/v1/lockers/" + locker.getId() + "/compartments/" + c.getId() + "/status").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OUT_OF_SERVICE"));

        // OUT_OF_SERVICE -> AVAILABLE
        CompartmentStatusUpdateRequest req2 = new CompartmentStatusUpdateRequest();
        req2.setStatus(CompartmentStatus.AVAILABLE);

        mockMvc.perform(patch("/api/v1/lockers/" + locker.getId() + "/compartments/" + c.getId() + "/status").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AVAILABLE"));
    }

    @Test
    @WithMockUser(username = "admin_tester", roles = "ADMIN")
    public void testLockerDetailSummary() throws Exception {
        Locker locker = new Locker();
        locker.setCode("LK-SUMMARY");
        locker.setName("Summary Locker");
        locker.setLocation("B1");
        locker.setStatus(LockerStatus.ACTIVE);
        locker = lockerRepository.saveAndFlush(locker);

        Compartment c1 = new Compartment();
        c1.setLocker(locker);
        c1.setCode("C01");
        c1.setStatus(CompartmentStatus.AVAILABLE);
        compartmentRepository.saveAndFlush(c1);

        Compartment c2 = new Compartment();
        c2.setLocker(locker);
        c2.setCode("C02");
        c2.setStatus(CompartmentStatus.OCCUPIED);
        compartmentRepository.saveAndFlush(c2);

        Compartment c3 = new Compartment();
        c3.setLocker(locker);
        c3.setCode("C03");
        c3.setStatus(CompartmentStatus.OUT_OF_SERVICE);
        compartmentRepository.saveAndFlush(c3);

        mockMvc.perform(get("/api/v1/lockers/" + locker.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("LK-SUMMARY"))
                .andExpect(jsonPath("$.compartmentSummary.total").value(3))
                .andExpect(jsonPath("$.compartmentSummary.available").value(1))
                .andExpect(jsonPath("$.compartmentSummary.occupied").value(1))
                .andExpect(jsonPath("$.compartmentSummary.outOfService").value(1))
                .andExpect(jsonPath("$.compartments").isArray());
    }
}
