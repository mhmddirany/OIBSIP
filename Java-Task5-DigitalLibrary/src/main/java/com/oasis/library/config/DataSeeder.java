package com.oasis.library.config;

import java.time.LocalDate;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.oasis.library.model.Book;
import com.oasis.library.model.ContactMessage;
import com.oasis.library.model.Loan;
import com.oasis.library.model.Role;
import com.oasis.library.model.User;
import com.oasis.library.repository.BookRepository;
import com.oasis.library.repository.ContactMessageRepository;
import com.oasis.library.repository.LoanRepository;
import com.oasis.library.repository.UserRepository;

/**
 * Fills an empty database with demo accounts, books and a few loans so every
 * feature (overdue fines, reservations, messages) can be shown right away.
 * Runs only when there are no users yet.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository users;
    private final BookRepository books;
    private final LoanRepository loans;
    private final ContactMessageRepository messages;
    private final PasswordEncoder passwordEncoder;
    private final LibraryProperties props;

    public DataSeeder(UserRepository users, BookRepository books, LoanRepository loans,
                      ContactMessageRepository messages, PasswordEncoder passwordEncoder, LibraryProperties props) {
        this.users = users;
        this.books = books;
        this.loans = loans;
        this.messages = messages;
        this.passwordEncoder = passwordEncoder;
        this.props = props;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (users.count() > 0) {
            return;
        }
        log.info("Empty database - adding demo data");

        users.save(new User("Library Admin", "admin@library.com",
                passwordEncoder.encode("Admin@123"), Role.ADMIN));
        User mohammad = users.save(new User("Mohammad Dirani", "mohammad@library.com",
                passwordEncoder.encode("User@1234"), Role.USER));
        User sara = users.save(new User("Sara Haddad", "sara@library.com",
                passwordEncoder.encode("User@1234"), Role.USER));

        List<Book> catalogue = books.saveAll(List.of(
                new Book("Clean Code", "Robert C. Martin", "9780132350884", "Programming", 1),
                new Book("Effective Java", "Joshua Bloch", "9780134685991", "Programming", 3),
                new Book("Head First Java", "Kathy Sierra, Bert Bates", "9780596009205", "Programming", 2),
                new Book("Spring in Action", "Craig Walls", "9781617297571", "Programming", 2),
                new Book("The Pragmatic Programmer", "David Thomas, Andrew Hunt", "9780135957059", "Software Engineering", 2),
                new Book("Design Patterns", "Erich Gamma et al.", "9780201633610", "Software Engineering", 2),
                new Book("Refactoring", "Martin Fowler", "9780134757599", "Software Engineering", 1),
                new Book("1984", "George Orwell", "9780451524935", "Fiction", 3),
                new Book("To Kill a Mockingbird", "Harper Lee", "9780061120084", "Fiction", 2),
                new Book("The Great Gatsby", "F. Scott Fitzgerald", "9780743273565", "Fiction", 2),
                new Book("A Brief History of Time", "Stephen Hawking", "9780553380163", "Science", 2),
                new Book("Cosmos", "Carl Sagan", "9780345539434", "Science", 1),
                new Book("Sapiens", "Yuval Noah Harari", "9780062316097", "History", 2),
                new Book("Atomic Habits", "James Clear", "9780735211292", "Self-Help", 3),
                new Book("Deep Work", "Cal Newport", "9781455586691", "Self-Help", 2)));

        Book cleanCode = catalogue.get(0);
        Book effectiveJava = catalogue.get(1);
        Book nineteen84 = catalogue.get(7);
        LocalDate today = LocalDate.now();

        // Sara holds the only copy of Clean Code and it is 6 days overdue
        // -> shows an overdue loan with a running fine, and lets others reserve it.
        issue(cleanCode, sara, today.minusDays(props.loanDays() + 6L));

        // Sara returned 1984 four days late -> an unpaid fine the admin can mark as paid.
        Loan late = issue(nineteen84, sara, today.minusDays(props.loanDays() + 20L));
        late.markReturned(today.minusDays(16), props.finePerDay());
        nineteen84.returnCopy();

        // Mohammad has one normal loan.
        issue(effectiveJava, mohammad, today.minusDays(3));

        messages.save(new ContactMessage(sara, "Extend my loan?",
                "Hi, can I keep Clean Code a few more days? I'm using it for a project."));

        log.info("Demo data ready. Admin: admin@library.com / Admin@123 - Member: mohammad@library.com / User@1234");
    }

    private Loan issue(Book book, User user, LocalDate issueDate) {
        book.takeCopy();
        return loans.save(new Loan(book, user, issueDate, issueDate.plusDays(props.loanDays())));
    }
}
