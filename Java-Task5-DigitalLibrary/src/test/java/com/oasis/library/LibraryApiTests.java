package com.oasis.library;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;
import com.oasis.library.model.Book;
import com.oasis.library.repository.BookRepository;

/**
 * End-to-end tests of the REST API against an in-memory H2 database filled by DataSeeder.
 * Each test runs in a transaction that is rolled back, so tests don't affect each other.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LibraryApiTests {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private BookRepository books;

    // ------------------------------------------------------------ helpers

    private MockHttpSession login(String email, String password) throws Exception {
        MvcResult result = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private MockHttpSession admin() throws Exception {
        return login("admin@library.com", "Admin@123");
    }

    private MockHttpSession mohammad() throws Exception {
        return login("mohammad@library.com", "User@1234");
    }

    private MockHttpSession sara() throws Exception {
        return login("sara@library.com", "User@1234");
    }

    private Book bookByTitle(String title) {
        return books.search(title, "").stream().filter(b -> b.getTitle().equals(title)).findFirst().orElseThrow();
    }

    private String bookRef(Book book) {
        return "{\"bookId\":" + book.getId() + "}";
    }

    // -------------------------------------------------------- auth & roles

    @Test
    void apiRequiresLogin() throws Exception {
        mvc.perform(get("/api/books"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void wrongPasswordIsRejectedWithGenericMessage() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@library.com\",\"password\":\"wrong-pass1\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid email or password"));
    }

    @Test
    void membersCannotUseAdminEndpoints() throws Exception {
        mvc.perform(get("/api/admin/stats").session(mohammad())).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/stats").session(admin())).andExpect(status().isOk());
    }

    @Test
    void registrationValidatesAndPreventsDuplicates() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Test\",\"email\":\"t@x.com\",\"password\":\"short\"}"))
                .andExpect(status().isBadRequest());

        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"New Reader\",\"email\":\"new@reader.com\",\"password\":\"reader123\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("USER"));

        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Again\",\"email\":\"NEW@reader.com\",\"password\":\"reader123\"}"))
                .andExpect(status().isConflict());
    }

    // ------------------------------------------------------ issue & return

    @Test
    void issueAndReturnUpdatesAvailableCopies() throws Exception {
        MockHttpSession session = mohammad();
        Book book = bookByTitle("Head First Java");
        int before = book.getAvailableQuantity();

        MvcResult issued = mvc.perform(post("/api/me/loans").session(session)
                        .contentType(MediaType.APPLICATION_JSON).content(bookRef(book)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andReturn();
        assertThat(bookByTitle("Head First Java").getAvailableQuantity()).isEqualTo(before - 1);

        // Same book twice is not allowed.
        mvc.perform(post("/api/me/loans").session(session)
                        .contentType(MediaType.APPLICATION_JSON).content(bookRef(book)))
                .andExpect(status().isConflict());

        Integer loanId = JsonPath.read(issued.getResponse().getContentAsString(), "$.id");
        mvc.perform(post("/api/me/loans/" + loanId + "/return").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RETURNED"))
                .andExpect(jsonPath("$.fine").value(0));
        assertThat(bookByTitle("Head First Java").getAvailableQuantity()).isEqualTo(before);
    }

    @Test
    void membersCannotReturnSomeoneElsesLoan() throws Exception {
        MvcResult overdue = mvc.perform(get("/api/admin/loans?filter=overdue").session(admin())).andReturn();
        List<Integer> ids = JsonPath.read(overdue.getResponse().getContentAsString(), "$[?(@.bookTitle=='Clean Code')].id");

        mvc.perform(post("/api/me/loans/" + ids.get(0) + "/return").session(mohammad()))
                .andExpect(status().isNotFound());
    }

    // ------------------------------------- reservations, fines, auto-issue

    @Test
    void reservationIsAutoIssuedWhenBookIsReturnedAndFineIsCharged() throws Exception {
        MockHttpSession member = mohammad();
        MockHttpSession admin = admin();
        Book cleanCode = bookByTitle("Clean Code"); // only copy is with Sara, 6 days overdue

        // Can't issue: no copies. Can reserve.
        mvc.perform(post("/api/me/loans").session(member)
                        .contentType(MediaType.APPLICATION_JSON).content(bookRef(cleanCode)))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/me/reservations").session(member)
                        .contentType(MediaType.APPLICATION_JSON).content(bookRef(cleanCode)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.queuePosition").value(1));

        // Admin checks Sara's overdue copy back in: 6 days x 5 = 30.
        MvcResult overdue = mvc.perform(get("/api/admin/loans?filter=overdue").session(admin)).andReturn();
        List<Integer> ids = JsonPath.read(overdue.getResponse().getContentAsString(), "$[?(@.bookTitle=='Clean Code')].id");
        mvc.perform(post("/api/admin/loans/" + ids.get(0) + "/return").session(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overdueDays").value(6))
                .andExpect(jsonPath("$.fine").value(30.0));

        // The copy went straight to Mohammad.
        mvc.perform(get("/api/me/reservations").session(member))
                .andExpect(jsonPath("$[0].status").value("FULFILLED"));
        mvc.perform(get("/api/me/loans").session(member))
                .andExpect(jsonPath("$[?(@.bookTitle=='Clean Code')].status").value("ACTIVE"));
        assertThat(bookByTitle("Clean Code").getAvailableQuantity()).isZero();
    }

    @Test
    void unpaidFinesBlockIssuingUntilAdminMarksThemPaid() throws Exception {
        MockHttpSession sara = sara();
        MockHttpSession admin = admin();
        Book atomicHabits = bookByTitle("Atomic Habits");

        // Return Sara's overdue book first so only the unpaid fines rule applies.
        MvcResult overdue = mvc.perform(get("/api/admin/loans?filter=overdue").session(admin)).andReturn();
        List<Integer> overdueIds = JsonPath.read(overdue.getResponse().getContentAsString(), "$[*].id");
        for (Integer id : overdueIds) {
            mvc.perform(post("/api/admin/loans/" + id + "/return").session(admin)).andExpect(status().isOk());
        }

        mvc.perform(post("/api/me/loans").session(sara)
                        .contentType(MediaType.APPLICATION_JSON).content(bookRef(atomicHabits)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.containsString("unpaid fines")));

        MvcResult fines = mvc.perform(get("/api/admin/fines").session(admin)).andReturn();
        List<Integer> unpaid = JsonPath.read(fines.getResponse().getContentAsString(), "$[?(@.finePaid==false)].id");
        for (Integer id : unpaid) {
            mvc.perform(post("/api/admin/loans/" + id + "/pay-fine").session(admin)).andExpect(status().isOk());
        }

        mvc.perform(post("/api/me/loans").session(sara)
                        .contentType(MediaType.APPLICATION_JSON).content(bookRef(atomicHabits)))
                .andExpect(status().isCreated());
    }

    // ------------------------------------------------------------- admin

    @Test
    void adminCanAddEditAndDeleteBooksWithValidation() throws Exception {
        MockHttpSession admin = admin();
        String body = "{\"title\":\"Java Concurrency in Practice\",\"author\":\"Brian Goetz\","
                + "\"isbn\":\"9780321349606\",\"category\":\"Programming\",\"totalQuantity\":2}";

        MvcResult created = mvc.perform(post("/api/admin/books").session(admin)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.availableQuantity").value(2))
                .andReturn();
        Integer id = JsonPath.read(created.getResponse().getContentAsString(), "$.id");

        // Duplicate ISBN
        mvc.perform(post("/api/admin/books").session(admin)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());

        // Invalid data
        mvc.perform(post("/api/admin/books").session(admin).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\",\"author\":\"x\",\"isbn\":\"abc\",\"category\":\"x\",\"totalQuantity\":0}"))
                .andExpect(status().isBadRequest());

        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put("/api/admin/books/" + id).session(admin)
                        .contentType(MediaType.APPLICATION_JSON).content(body.replace("\"totalQuantity\":2", "\"totalQuantity\":5")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalQuantity").value(5));

        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/api/admin/books/" + id).session(admin))
                .andExpect(status().isOk());
    }

    @Test
    void deactivatedMembersCannotLogIn() throws Exception {
        MockHttpSession admin = admin();
        MvcResult members = mvc.perform(get("/api/admin/members").session(admin)).andReturn();
        List<Integer> ids = JsonPath.read(members.getResponse().getContentAsString(),
                "$[?(@.email=='mohammad@library.com')].id");

        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .patch("/api/admin/members/" + ids.get(0) + "/status").session(admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"active\":false}"))
                .andExpect(status().isOk());

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"mohammad@library.com\",\"password\":\"User@1234\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void contactMessagesReachTheAdmin() throws Exception {
        mvc.perform(post("/api/me/messages").session(mohammad()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"subject\":\"New book request\",\"message\":\"Please add Effective Kotlin.\"}"))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/admin/messages").session(admin()))
                .andExpect(jsonPath("$[?(@.subject=='New book request')].name").value("Mohammad Dirani"));
    }
}
