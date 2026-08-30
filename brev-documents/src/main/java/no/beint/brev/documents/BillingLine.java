package no.beint.brev.documents;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.Optional;

/** One invoice or credit-note line. The net amount is explicit so ledger rounding is preserved. */
public record BillingLine(
        String id,
        String itemName,
        Quantity quantity,
        UnitPrice unitPrice,
        VatCategory vatCategory,
        Money netAmount,
        Optional<Money> priceAllowance,
        Optional<String> itemDescription) {

    public BillingLine {
        BillingValues.nonBlank(id, "line ID", 64);
        BillingValues.nonBlank(itemName, "item name", 200);
        Objects.requireNonNull(quantity, "quantity");
        Objects.requireNonNull(unitPrice, "unitPrice");
        Objects.requireNonNull(vatCategory, "vatCategory");
        Objects.requireNonNull(netAmount, "netAmount");
        Objects.requireNonNull(priceAllowance, "priceAllowance");
        Objects.requireNonNull(itemDescription, "itemDescription");
        itemDescription = itemDescription.flatMap(value -> BillingValues.optionalText(value, "item description", 500));
        if (!unitPrice.currency().equals(netAmount.currency())) {
            throw new IllegalArgumentException("line " + id + " net amount currency does not match unit price");
        }
        priceAllowance.ifPresent(allowance -> {
            allowance.requireSameCurrency(netAmount);
            if (allowance.amount().signum() < 0) {
                throw new IllegalArgumentException("price allowance must not be negative");
            }
        });
    }

    public BillingLine(
            String id,
            String itemName,
            Quantity quantity,
            UnitPrice unitPrice,
            VatCategory vatCategory) {
        this(
                id,
                itemName,
                quantity,
                unitPrice,
                vatCategory,
                derivedNet(unitPrice, quantity),
                Optional.empty(),
                Optional.empty());
    }

    public BillingLine(
            String id,
            String itemName,
            Quantity quantity,
            UnitPrice unitPrice,
            VatCategory vatCategory,
            Money netAmount,
            Optional<Money> priceAllowance) {
        this(id, itemName, quantity, unitPrice, vatCategory, netAmount, priceAllowance, Optional.empty());
    }

    public BillingLine(
            String id,
            String itemName,
            Quantity quantity,
            UnitPrice unitPrice,
            VatCategory vatCategory,
            Money netAmount) {
        this(id, itemName, quantity, unitPrice, vatCategory, netAmount, Optional.empty(), Optional.empty());
    }

    public BillingLine withPriceAllowance(Money allowance) {
        return new BillingLine(
                id, itemName, quantity, unitPrice, vatCategory, netAmount, Optional.of(allowance), itemDescription);
    }

    public BillingLine withItemDescription(String description) {
        return new BillingLine(
                id, itemName, quantity, unitPrice, vatCategory, netAmount, priceAllowance, Optional.of(description));
    }

    private static Money derivedNet(UnitPrice unitPrice, Quantity quantity) {
        BigDecimal net = unitPrice.amount()
                .multiply(quantity.value())
                .divide(unitPrice.baseQuantity(), 2, RoundingMode.HALF_UP);
        return new Money(unitPrice.currency(), net);
    }
}
