package no.beint.brev.benchmark;

import no.beint.brev.documents.BillingDocument;
import no.beint.brev.documents.Documents;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import peppol.bis.invoice3.api.PeppolBillingApi;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 3)
@Measurement(iterations = 5)
@Fork(2)
public class BillingWriterBenchmark {
    private final BillingDocument brevInvoice = BillingBenchmarkFixture.createBrevInvoice();
    private final peppol.bis.invoice3.domain.Invoice digipostInvoice =
            BillingBenchmarkFixture.createDigipostInvoice();

    @Benchmark
    public byte[] brevSerialization() {
        return Documents.toByteArray(brevInvoice);
    }

    @Benchmark
    public byte[] digipostSerialization() throws IOException {
        return PeppolBillingApi.create(digipostInvoice).inputStream().readAllBytes();
    }
}
