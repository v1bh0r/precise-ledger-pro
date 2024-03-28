package ledger.producer;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.javamoney.moneta.Money;

import javax.money.Monetary;
import javax.money.MonetaryAmountFactory;

@ApplicationScoped
public class MonetaryProducer {
    @Produces
    @ApplicationScoped // Define the scope as needed
    public MonetaryAmountFactory<Money> produceMonetaryAmountFactory() {
        // Returns the default MonetaryAmountFactory
        return Monetary.getAmountFactory(Money.class);
    }
}
