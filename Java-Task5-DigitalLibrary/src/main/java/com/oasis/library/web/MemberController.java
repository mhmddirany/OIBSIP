package com.oasis.library.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.oasis.library.dto.Requests;
import com.oasis.library.dto.Responses;
import com.oasis.library.security.LibraryUserDetails;
import com.oasis.library.service.CirculationService;
import com.oasis.library.service.ContactService;

import jakarta.validation.Valid;

/** Everything a logged-in member does with their own account. */
@RestController
@RequestMapping("/api/me")
public class MemberController {

    private final CirculationService circulation;
    private final ContactService contactService;

    public MemberController(CirculationService circulation, ContactService contactService) {
        this.circulation = circulation;
        this.contactService = contactService;
    }

    @GetMapping("/summary")
    public Responses.MemberSummary summary(@AuthenticationPrincipal LibraryUserDetails me) {
        return circulation.summaryOf(me.getId());
    }

    // ----------------------------------------------------------------- loans

    @GetMapping("/loans")
    public List<Responses.LoanInfo> loans(@AuthenticationPrincipal LibraryUserDetails me) {
        return circulation.loansOf(me.getId());
    }

    /** Issue a book: body {"bookId": 1}. */
    @PostMapping("/loans")
    @ResponseStatus(HttpStatus.CREATED)
    public Responses.LoanInfo issue(@AuthenticationPrincipal LibraryUserDetails me,
                                    @Valid @RequestBody Requests.BookRef req) {
        return circulation.issue(me.getId(), req.bookId());
    }

    @PostMapping("/loans/{id}/return")
    public Responses.LoanInfo returnBook(@AuthenticationPrincipal LibraryUserDetails me, @PathVariable Long id) {
        return circulation.returnBook(id, me.getId(), false);
    }

    // ---------------------------------------------------------- reservations

    @GetMapping("/reservations")
    public List<Responses.ReservationInfo> reservations(@AuthenticationPrincipal LibraryUserDetails me) {
        return circulation.reservationsOf(me.getId());
    }

    /** Reserve a book that is currently issued: body {"bookId": 1}. */
    @PostMapping("/reservations")
    @ResponseStatus(HttpStatus.CREATED)
    public Responses.ReservationInfo reserve(@AuthenticationPrincipal LibraryUserDetails me,
                                             @Valid @RequestBody Requests.BookRef req) {
        return circulation.reserve(me.getId(), req.bookId());
    }

    @DeleteMapping("/reservations/{id}")
    public Responses.ReservationInfo cancel(@AuthenticationPrincipal LibraryUserDetails me, @PathVariable Long id) {
        return circulation.cancelReservation(id, me.getId());
    }

    // --------------------------------------------------------------- contact

    @PostMapping("/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public Responses.MessageInfo contact(@AuthenticationPrincipal LibraryUserDetails me,
                                         @Valid @RequestBody Requests.Contact req) {
        return contactService.send(me.getId(), req);
    }
}
