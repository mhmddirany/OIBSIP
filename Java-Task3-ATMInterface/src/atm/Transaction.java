package atm;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * An immutable record of a single money movement on an account.
 */
public class Transaction {

    public enum Type { DEPOSIT, WITHDRAW, TRANSFER_OUT, TRANSFER_IN }

    private static final DateTimeFormatter FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Type type;
    private final BigDecimal amount;
    private final BigDecimal balanceAfter;
    private final String note;
    private final LocalDateTime timestamp;

    public Transaction(Type type, BigDecimal amount, BigDecimal balanceAfter, String note) {
        this.type = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.note = note;
        this.timestamp = LocalDateTime.now();
    }

    public Type getType() { return type; }
    public BigDecimal getAmount() { return amount; }
    public BigDecimal getBalanceAfter() { return balanceAfter; }
    public String getNote() { return note; }
    public LocalDateTime getTimestamp() { return timestamp; }

    /** Credits are shown with "+", debits with "-". */
    private String signedAmount() {
        boolean credit = type == Type.DEPOSIT || type == Type.TRANSFER_IN;
        return (credit ? "+" : "-") + String.format("%,.2f", amount);
    }

    @Override
    public String toString() {
        return String.format("%s | %-12s | %12s | bal %12s | %s",
                timestamp.format(FORMAT), type, signedAmount(),
                String.format("%,.2f", balanceAfter), note);
    }
}
