# ATM Interface — Oasis Infobyte Java Development (Task 3)

A console-based ATM simulation written in Java with an object-oriented design. Users log in with a User ID and PIN, then withdraw, deposit, transfer money to another account, and view their transaction history.

## Features

- **Secure login.** User ID and PIN are checked, and access is denied after **3 incorrect attempts**. The error message never reveals whether the ID or the PIN was wrong.
- **PINs are hashed.** Only the SHA-256 hash is stored, never the plain PIN.
- **Main menu:**
  1. **Transaction History** lists every transaction in the current session with timestamp, type, amount and balance after.
  2. **Withdraw** checks the balance first and shows *"Insufficient Funds"* if it is too low.
  3. **Deposit** adds money to the account.
  4. **Transfer** checks that the recipient exists and that the balance is sufficient, then updates **both** accounts and logs the transaction on each side.
  5. **Quit** shows a goodbye message and exits.
- **Input validation.** Rejects non-numeric, zero or negative amounts, amounts with more than 2 decimals, amounts over 1,000,000, invalid menu options, and transfers to your own account.
- **Exact money arithmetic.** Uses `BigDecimal` instead of `double`, so there are no floating-point rounding errors.
- **Transaction storage.** Transactions are stored in an `ArrayList`.

## Class Design

| Class | Responsibility |
|---|---|
| `Main` | Entry point. Creates the bank, adds demo accounts, starts the ATM. |
| `ATM` | Console user interface: login loop, menu, prompts and input validation. |
| `Bank` | Holds all accounts. Handles authentication and the rules for deposit, withdraw and transfer. |
| `Account` | Encapsulated account data (private fields and getters), hashed PIN, balance and transaction list. |
| `Transaction` | Immutable record of one operation: type, amount, balance after, note, timestamp. |

```
Main ──creates──> Bank ──holds──> Account ──logs──> Transaction
  └──starts──> ATM ──uses──> Bank
```

The `ATM` class only handles input and output. All business rules live in `Bank` and `Account`, which keeps the design clean and easy to test.

## How to Run

Requires **Java 11 or newer**.

```bash
cd Java-Task3-ATMInterface
javac -d out src/atm/*.java
java -cp out atm.Main
```

You can also open the folder in IntelliJ IDEA or VS Code and run `Main.java`.

## Demo Accounts

| User ID | PIN | Holder | Opening Balance |
|---|---|---|---|
| 1001 | 1234 | Mohammad Dirani | 5,000.00 |
| 1002 | 4321 | Sara Haddad | 2,500.00 |
| 1003 | 1111 | Ali Khalil | 750.00 |

Data is kept in memory, so balances reset each time the program starts.

## Sample Session

```
===============================
   Oasis Bank ATM
===============================
User ID: 1001
PIN: ****
Welcome, Mohammad Dirani!

========== MAIN MENU ==========
 1. Transaction History
 2. Withdraw
 3. Deposit
 4. Transfer
 5. Quit
-------------------------------
 Current balance: 5,000.00
Choose an option (1-5): 4
Recipient account ID: 1002
Amount to transfer: 1000
Transferred 1,000.00 to Sara Haddad (1002).
New balance: 4,000.00
```

## Screenshots

Screenshots are in the [`screenshots/`](screenshots) folder.

## Author

Mohammad Dirani, Java Development Intern at Oasis Infobyte (#oasisinfobyte)
