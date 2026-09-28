package atm;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A bank account. Fields are private (encapsulation); the balance can only
 * change through deposit/withdraw, and every change is logged as a Transaction.
 * The PIN is never stored in plain text — only its SHA-256 hash.
 */
public class Account {

    private final String userId;
    private final String holderName;
    private final String pinHash;
    private BigDecimal balance;
    private final List<Transaction> history = new ArrayList<>();

    public Account(String userId, String holderName, String pin, BigDecimal openingBalance) {
        this.userId = userId;
        this.holderName = holderName;
        this.pinHash = hash(pin);
        this.balance = openingBalance;
    }

    public String getUserId() { return userId; }
    public String getHolderName() { return holderName; }
    public BigDecimal getBalance() { return balance; }

    /** Read-only view so outside code cannot tamper with the history. */
    public List<Transaction> getHistory() {
        return Collections.unmodifiableList(history);
    }

    public boolean checkPin(String pin) {
        return pinHash.equals(hash(pin));
    }

    public boolean hasSufficientFunds(BigDecimal amount) {
        return balance.compareTo(amount) >= 0;
    }

    void deposit(BigDecimal amount, Transaction.Type type, String note) {
        balance = balance.add(amount);
        history.add(new Transaction(type, amount, balance, note));
    }

    void withdraw(BigDecimal amount, Transaction.Type type, String note) {
        if (!hasSufficientFunds(amount)) {
            throw new IllegalStateException("Insufficient Funds");
        }
        balance = balance.subtract(amount);
        history.add(new Transaction(type, amount, balance, note));
    }

    private static String hash(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
