package no.beint.brev.documents;

import java.util.Optional;

/** A purchase-order / sales-order reference (BT-13 / BT-14). */
public record OrderReference(String id, Optional<String> salesOrderId) {
    public OrderReference {
        BillingValues.nonBlank(id, "order reference", 100);
        salesOrderId = salesOrderId.flatMap(value -> BillingValues.optionalText(value, "sales order ID", 100));
    }

    public OrderReference(String id) {
        this(id, Optional.empty());
    }

    public static OrderReference salesOrder(String salesOrderId) {
        return new OrderReference("NA", Optional.of(salesOrderId));
    }
}
