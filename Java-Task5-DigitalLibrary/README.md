# Digital Library Management System — Oasis Infobyte Java Development (Task 5)

A web-based library system with separate **Admin** and **Member** roles. It manages a book catalogue, issues and returns books, calculates overdue fines automatically, supports advance booking (reservations) and includes a contact form.

The backend is a **Spring Boot REST API** (JSON). The frontend is **plain HTML, CSS and JavaScript** that calls the API with `fetch()`, with no frontend framework.

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Java 17+, Spring Boot 3.5 (Web, Data JPA, Security, Validation) |
| Database | H2 (file-based, zero setup; data stored in `./data/`) |
| Auth | Spring Security, session-based login, BCrypt password hashing, role-based access |
| Frontend | HTML5, CSS3 (custom, responsive), vanilla JavaScript (`fetch` API) |
| Tests | JUnit 5, Spring Boot Test, MockMvc (end-to-end API tests) |

## Features

### Admin Module
- Admin login with access to every feature (`/api/admin/**` requires `ROLE_ADMIN`)
- **Dashboard**: titles, copies, issued, overdue, members, reservations, unpaid fines, open messages
- **Books**: add, edit or delete books (title, author, ISBN, category, quantity). ISBNs are unique; quantity can't drop below the copies already issued; books with issued copies can't be deleted.
- **Issued books**: view current, overdue or all loans with due dates, and check books back in at the desk
- **Members**: view all accounts with books out and unpaid fines; activate, deactivate or delete (deactivated members can't log in)
- **Fines**: view all fines and mark them as paid
- **Reservations**: view the waiting queue for every book
- **Messages**: read member queries and mark them resolved

### Member Module
- Registration (password at least 8 characters with a number, duplicate emails rejected) and login
- Browse the catalogue **by category** and **search** by title, author or ISBN
- **Issue a book**: available copies go down by one and the due date is set (14 days)
- **Return a book**: available copies go up by one
- **Automatic fines**: ₹5 per day overdue, calculated on return. Running fines are shown for books still out.
- **Advance booking**: reserve a book whose copies are all issued. When a copy is returned, it is **automatically issued to the first member in the queue**.
- **Contact form**: messages are stored in the database and shown in the admin panel

### Library Rules (configurable in `application.properties`)
| Rule | Default |
|---|---|
| Loan period | 14 days |
| Fine per overdue day | ₹5 |
| Max books per member | 3 |

Members can't issue new books while they have **overdue books** or **unpaid fines**.

## How to Run

Requires **JDK 17 or newer**.

```bash
cd Java-Task5-DigitalLibrary
./mvnw spring-boot:run        # Windows: mvnw.cmd spring-boot:run
```

Open **http://localhost:8080**. On first start, the database is filled with demo data.

You can also open the folder in IntelliJ IDEA and run `LibraryApplication`.

Run the tests with:

```bash
./mvnw test
```

### Demo Accounts

| Role | Email | Password | What to try |
|---|---|---|---|
| Admin | `admin@library.com` | `Admin@123` | All admin features |
| Member | `mohammad@library.com` | `User@1234` | Issue, return and reserve *Clean Code* |
| Member | `sara@library.com` | `User@1234` | Has an overdue book and an unpaid fine |

To reset the demo data, stop the app and delete the `data/` folder.

## REST API

All responses are JSON. Errors look like `{"error": "message"}`.

| Method | Endpoint | Access | Description |
|---|---|---|---|
| POST | `/api/auth/register` | Public | Create a member account |
| POST | `/api/auth/login` | Public | Log in (starts a session) |
| POST | `/api/auth/logout` | Logged in | Log out |
| GET | `/api/auth/me` | Logged in | Current user |
| GET | `/api/books?q=&category=` | Logged in | Search and filter the catalogue |
| GET | `/api/books/categories` | Logged in | List categories |
| GET | `/api/me/summary` | Member | Active or overdue books, fines, rules |
| GET / POST | `/api/me/loans` | Member | My loans / issue a book `{bookId}` |
| POST | `/api/me/loans/{id}/return` | Member | Return my book |
| GET / POST | `/api/me/reservations` | Member | My reservations / reserve `{bookId}` |
| DELETE | `/api/me/reservations/{id}` | Member | Cancel a reservation |
| POST | `/api/me/messages` | Member | Send a contact message |
| GET | `/api/admin/stats` | Admin | Dashboard numbers |
| POST / PUT / DELETE | `/api/admin/books[/{id}]` | Admin | Add, edit or delete a book |
| GET | `/api/admin/loans?filter=active\|overdue\|all` | Admin | Issued books |
| POST | `/api/admin/loans/{id}/return` | Admin | Check a book back in |
| GET | `/api/admin/fines` | Admin | All fines |
| POST | `/api/admin/loans/{id}/pay-fine` | Admin | Mark a fine as paid |
| GET | `/api/admin/reservations` | Admin | All reservations |
| GET | `/api/admin/members` | Admin | All accounts |
| PATCH | `/api/admin/members/{id}/status` | Admin | Activate or deactivate `{active}` |
| DELETE | `/api/admin/members/{id}` | Admin | Delete a member |
| GET | `/api/admin/messages` | Admin | Contact messages |
| PATCH | `/api/admin/messages/{id}/resolve` | Admin | Mark a message resolved |

## Project Structure

```
src/main/java/com/oasis/library
├── config/        SecurityConfig, DataSeeder (demo data), LibraryProperties (rules)
├── model/         JPA entities: User, Book, Loan, Reservation, ContactMessage
├── repository/    Spring Data JPA repositories
├── dto/           Request (validated) and response records
├── security/      UserDetails / UserDetailsService for Spring Security
├── service/       Business rules: AuthService, BookService, CirculationService, AdminService, ContactService
└── web/           REST controllers + GlobalExceptionHandler
src/main/resources/static
├── index.html, register.html   Login and registration
├── app.html                    Member dashboard
├── admin.html                  Admin dashboard
├── css/style.css
└── js/  common.js (fetch wrapper, helpers), auth.js, app.js, admin.js
```

### Database Design

```
users (id, full_name, email UNIQUE, password_hash, role, active, created_at)
books (id, title, author, isbn UNIQUE, category, total_quantity, available_quantity, created_at)
loans (id, book_id → books, user_id → users, issue_date, due_date, return_date, fine, fine_paid)
reservations (id, book_id → books, user_id → users, reserved_at, status, closed_at)
contact_messages (id, user_id → users NULL, name, email, subject, message, created_at, resolved)
```

## Design Decisions

- **Passwords** are hashed with BCrypt. Login errors never reveal whether the email or the password was wrong.
- **Entities are never returned directly.** Responses are DTO records, so password hashes can't leak.
- **Validation** runs twice: in the browser for quick feedback, and on the server with Bean Validation, which is the one that counts.
- **XSS protection**: every value is escaped before being inserted into the page.
- **CSRF**: the frontend is same-origin and the session cookie is `HttpOnly` and `SameSite=Strict`, so browsers never send it on cross-site requests.
- **Concurrency**: issuing takes a row lock on the book (`PESSIMISTIC_WRITE`), so two members can't take the last copy at the same moment.
- **Money** uses `BigDecimal`.

## Screenshots

See the [`screenshots/`](screenshots) folder.

## Author

Mohammad Dirani, Java Development Intern at Oasis Infobyte (#oasisinfobyte)
