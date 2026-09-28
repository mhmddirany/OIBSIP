package com.oasis.library.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.oasis.library.dto.Responses;
import com.oasis.library.model.ContactMessage;
import com.oasis.library.model.Loan;
import com.oasis.library.model.Reservation;
import com.oasis.library.model.ReservationStatus;
import com.oasis.library.model.Role;
import com.oasis.library.model.User;
import com.oasis.library.repository.BookRepository;
import com.oasis.library.repository.ContactMessageRepository;
import com.oasis.library.repository.LoanRepository;
import com.oasis.library.repository.ReservationRepository;
import com.oasis.library.repository.UserRepository;

/** Admin-only views and actions: dashboard, issued books, members, fines, messages. */
@Service
@Transactional
public class AdminService {

    private final BookRepository books;
    private final LoanRepository loans;
    private final ReservationRepository reservations;
    private final UserRepository users;
    private final ContactMessageRepository messages;
    private final DtoMapper mapper;

    public AdminService(BookRepository books, LoanRepository loans, ReservationRepository reservations,
                        UserRepository users, ContactMessageRepository messages, DtoMapper mapper) {
        this.books = books;
        this.loans = loans;
        this.reservations = reservations;
        this.users = users;
        this.messages = messages;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public Responses.Stats stats() {
        LocalDate today = LocalDate.now();
        return new Responses.Stats(
                books.count(),
                books.sumTotalCopies(),
                books.sumAvailableCopies(),
                loans.countByReturnDateIsNull(),
                loans.countByReturnDateIsNullAndDueDateBefore(today),
                users.countByRole(Role.USER),
                reservations.countByStatus(ReservationStatus.WAITING),
                loans.sumUnpaidFines(),
                messages.countByResolvedFalse());
    }

    /** filter: "active" (default), "overdue" or "all". */
    @Transactional(readOnly = true)
    public List<Responses.LoanInfo> loans(String filter) {
        List<Loan> result;
        String f = filter == null ? "active" : filter.toLowerCase();
        switch (f) {
            case "overdue" -> result = loans.findByReturnDateIsNullAndDueDateBeforeOrderByDueDateAsc(LocalDate.now());
            case "all" -> result = loans.findAllByOrderByIssueDateDescIdDesc();
            case "active" -> result = loans.findByReturnDateIsNullOrderByDueDateAsc();
            default -> throw ApiException.badRequest("Unknown filter '" + filter + "'. Use active, overdue or all");
        }
        return result.stream().map(mapper::loan).toList();
    }

    @Transactional(readOnly = true)
    public List<Responses.LoanInfo> fines() {
        return loans.findLoansWithFines().stream().map(mapper::loan).toList();
    }

    public Responses.LoanInfo markFinePaid(Long loanId) {
        Loan loan = loans.findById(loanId).orElseThrow(() -> ApiException.notFound("Loan"));
        if (loan.isActive()) {
            throw ApiException.conflict("The book is still issued. Fines are settled after the book is returned");
        }
        if (loan.getFine().signum() == 0) {
            throw ApiException.conflict("This loan has no fine");
        }
        if (loan.isFinePaid()) {
            throw ApiException.conflict("This fine is already paid");
        }
        loan.markFinePaid();
        return mapper.loan(loan);
    }

    @Transactional(readOnly = true)
    public List<Responses.ReservationInfo> reservations() {
        // Waiting reservations first, then the rest, oldest first.
        return reservations.findAll().stream()
                .sorted(Comparator
                        .comparing((Reservation r) -> r.getStatus() != ReservationStatus.WAITING)
                        .thenComparing(Reservation::getId))
                .map(mapper::reservation)
                .toList();
    }

    // ------------------------------------------------------------------ members

    @Transactional(readOnly = true)
    public List<Responses.Member> members() {
        return users.findAllByOrderByRoleAscFullNameAsc().stream()
                .map(u -> new Responses.Member(u.getId(), u.getFullName(), u.getEmail(), u.getRole().name(),
                        u.isActive(), u.getCreatedAt(),
                        loans.countByUser_IdAndReturnDateIsNull(u.getId()),
                        loans.sumUnpaidFinesForUser(u.getId())))
                .toList();
    }

    public Responses.Member setMemberActive(Long memberId, boolean active) {
        User user = users.findById(memberId).orElseThrow(() -> ApiException.notFound("Member"));
        if (user.getRole() == Role.ADMIN) {
            throw ApiException.conflict("Admin accounts cannot be deactivated here");
        }
        user.setActive(active);
        return new Responses.Member(user.getId(), user.getFullName(), user.getEmail(), user.getRole().name(),
                user.isActive(), user.getCreatedAt(),
                loans.countByUser_IdAndReturnDateIsNull(user.getId()),
                loans.sumUnpaidFinesForUser(user.getId()));
    }

    public void deleteMember(Long memberId) {
        User user = users.findById(memberId).orElseThrow(() -> ApiException.notFound("Member"));
        if (user.getRole() == Role.ADMIN) {
            throw ApiException.conflict("Admin accounts cannot be deleted");
        }
        if (loans.countByUser_IdAndReturnDateIsNull(memberId) > 0) {
            throw ApiException.conflict("This member still has books issued");
        }
        if (loans.sumUnpaidFinesForUser(memberId).compareTo(BigDecimal.ZERO) > 0) {
            throw ApiException.conflict("This member has unpaid fines");
        }
        reservations.deleteByUserId(memberId);
        loans.deleteByUserId(memberId);
        messages.detachUser(memberId);
        users.delete(user);
    }

    // ----------------------------------------------------------------- messages

    @Transactional(readOnly = true)
    public List<Responses.MessageInfo> messages() {
        return messages.findAllByOrderByResolvedAscCreatedAtDesc().stream().map(mapper::message).toList();
    }

    public Responses.MessageInfo resolveMessage(Long id) {
        ContactMessage message = messages.findById(id).orElseThrow(() -> ApiException.notFound("Message"));
        message.markResolved();
        return mapper.message(message);
    }
}
