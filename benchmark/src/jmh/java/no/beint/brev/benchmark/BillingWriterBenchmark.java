package no.beint.brev.benchmark;

import no.beint.brev.CountryCode;
import no.beint.brev.CurrencyCode;
import no.beint.brev.EndpointId;
import no.beint.brev.SchemeId;
import no.beint.brev.UnitCode;
import no.beint.brev.documents.BillingDocument;
import no.beint.brev.documents.BillingLine;
import no.beint.brev.documents.Documents;
import no.beint.brev.documents.Party;
import no.beint.brev.documents.PaymentInstruction;
import no.beint.brev.documents.PostalAddress;
import no.beint.brev.documents.Quantity;
import no.beint.brev.documents.UnitPrice;
import no.beint.brev.documents.VatCategory;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 3)
@Measurement(iterations = 5)
@Fork(2)
public class BillingWriterBenchmark {
    private static final CurrencyCode NOK = new CurrencyCode("NOK");
    private final BillingDocument invoice = createInvoice();

    @Benchmark
    public byte[] writeInvoice() {
        return Documents.toByteArray(invoice);
    }

    private static BillingDocument createInvoice() {
        PostalAddress address = new PostalAddress("Testveien 1", "Oslo", "0150", new CountryCode("NO"));
        Party seller = Party.withVat(
                new EndpointId(SchemeId.NORWEGIAN_ORGANIZATION, "913341464"),
                "Seller AS",
                "913341464",
                "NO913341464MVA",
                address);
        Party buyer = Party.withoutVat(
                new EndpointId(SchemeId.NORWEGIAN_ORGANIZATION, "987654321"),
                "Buyer AS",
                "987654321",
                address);
        return BillingDocument.invoice()
                .id("BENCH-1")
                .issueDate(LocalDate.of(2026, 8, 17))
                .dueDate(LocalDate.of(2026, 9, 1))
                .currency(NOK)
                .buyerReference("benchmark")
                .seller(seller)
                .buyer(buyer)
                .payment(new PaymentInstruction("NO9386011117947", "1234567890123456789012345"))
                .line(new BillingLine(
                        "1",
                        "Consulting",
                        new Quantity(new BigDecimal("10"), UnitCode.HOUR),
                        new UnitPrice(NOK, new BigDecimal("1250")),
                        VatCategory.standard(new BigDecimal("25"))))
                .build();
    }
}
