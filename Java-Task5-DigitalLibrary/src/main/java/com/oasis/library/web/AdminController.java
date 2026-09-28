package com.oasis.library.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.oasis.library.dto.Requests;
import com.oasis.library.dto.Responses;
import com.oasis.library.security.LibraryUserDetails;
import com.oasis.library.service.AdminService;
import com.oasis.library.service.BookService;
import com.oasis.library.service.CirculationService;

import jakarta.validation.Valid;

/** Admin module. Every endpoint here requires ROLE_ADMIN (see SecurityConfig). */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;
    private final BookService bookService;
    private final CirculationService circulation;

    public AdminController(AdminService adminService, BookService bookService, CirculationService circulation) {
        this.adminService = adminService;
        this.bookService = bookService;
        this.circulation = circulation;
    }

    @GetMapping("/stats")
    public Responses.Stats stats() {
        return adminService.stats();
    }

    // ------------------------------------------------------------------ books

    @PostMapping("/books")
    @ResponseStatus(HttpStatus.CREATED)
    public Responses.BookInfo addBook(@Valid @RequestBody Requests.BookForm form) {
        return bookService.create(form);
    }

    @PutMapping("/books/{id}")
    public Responses.BookInfo editBook(@PathVariable Long id, @Valid @RequestBody Requests.BookForm form) {
        return bookService.update(id, form);
    }

    @DeleteMapping("/books/{id}")
    public Responses.Message deleteBook(@PathVariable Long id) {
        bookService.delete(id);
        return new Responses.Message("Book deleted");
    }

    // ------------------------------------------------------------------ loans

    /** GET /api/admin/loans?filter=active|overdue|all */
    @GetMapping("/loans")
    public List<Responses.LoanInfo> loans(@RequestParam(defaultValue = "active") String filter) {
        return adminService.loans(filter);
    }

    /** Admin can check a book back in at the desk. */
    @PostMapping("/loans/{id}/return")
    public Responses.LoanInfo returnBook(@AuthenticationPrincipal LibraryUserDetails me, @PathVariable Long id) {
        return circulation.returnBook(id, me.getId(), true);
    }

    @GetMapping("/fines")
    public List<Responses.LoanInfo> fines() {
        return adminService.fines();
    }

    @PostMapping("/loans/{id}/pay-fine")
    public Responses.LoanInfo payFine(@PathVariable Long id) {
        return adminService.markFinePaid(id);
    }

    @GetMapping("/reservations")
    public List<Responses.ReservationInfo> reservations() {
        return adminService.reservations();
    }

    // ---------------------------------------------------------------- members

    @GetMapping("/members")
    public List<Responses.Member> members() {
        return adminService.members();
    }

    /** Activate / deactivate: body {"active": false}. */
    @PatchMapping("/members/{id}/status")
    public Responses.Member setStatus(@PathVariable Long id, @Valid @RequestBody Requests.MemberStatus req) {
        return adminService.setMemberActive(id, req.active());
    }

    @DeleteMapping("/members/{id}")
    public Responses.Message deleteMember(@PathVariable Long id) {
        adminService.deleteMember(id);
        return new Responses.Message("Member deleted");
    }

    // --------------------------------------------------------------- messages

    @GetMapping("/messages")
    public List<Responses.MessageInfo> messages() {
        return adminService.messages();
    }

    @PatchMapping("/messages/{id}/resolve")
    public Responses.MessageInfo resolve(@PathVariable Long id) {
        return adminService.resolveMessage(id);
    }
}
