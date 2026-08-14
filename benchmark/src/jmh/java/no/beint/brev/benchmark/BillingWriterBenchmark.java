package no.beint.brev.benchmark;

import no.beint.brev.CountryCode;
import no.beint.brev.CurrencyCode;
import no.beint.brev.EndpointId;
import no.beint.brev.SchemeId;
import no.beint.brev.UnitCode;
import no.beint.brev.billing.Invoice;
import no.beint.brev.billing.InvoiceLine;
import no.beint.brev.billing.Party;
import no.beint.brev.billing.PaymentInstruction;
import no.beint.brev.billing.PeppolBillingWriter;
import no.beint.brev.billing.PostalAddress;
import no.beint.brev.billing.Quantity;
import no.beint.brev.billing.TaxCategoryCode;
import no.beint.brev.billing.UnitPrice;
import no.beint.brev.billing.VatCategory;
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
import java.util.List;
import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 3)
@Measurement(iterations = 5)
@Fork(2)
public class BillingWriterBenchmark {
    private static final CurrencyCode NOK = new CurrencyCode("NOK");
    private final Invoice invoice = createInvoice();

    @Benchmark
    public byte[] writeInvoice() {
        return PeppolBillingWriter.toByteArray(invoice);
    }

    private static Invoice createInvoice() {
        SchemeId norwegianOrganization = new SchemeId("0192");
        PostalAddress address = new PostalAddress("Testveien 1", "Oslo", "0150", new CountryCode("NO"));
        Party seller = Party.withVat(
                new EndpointId(norwegianOrganization, "913341464"),
                "Seller AS",
                "913341464",
                "NO913341464MVA",
                address);
        Party buyer = Party.withoutVat(
                new EndpointId(norwegianOrganization, "987654321"),
                "Buyer AS",
                "987654321",
                address);
        InvoiceLine line = new InvoiceLine(
                "1",
                "Consulting",
                new Quantity(new BigDecimal("10"), new UnitCode("HUR")),
                new UnitPrice(NOK, new BigDecimal("1250")),
                new VatCategory(TaxCategoryCode.STANDARD_RATE, new BigDecimal("25")));
        return new Invoice(
                "BENCH-1",
                LocalDate.of(2026, 8, 17),
                LocalDate.of(2026, 9, 1),
                NOK,
                "benchmark",
                seller,
                buyer,
                new PaymentInstruction("NO9386011117947", "1234567890123456789012345"),
                List.of(line));
    }
}
