package ledger.service;

import jakarta.enterprise.context.ApplicationScoped;
import ledger.common.ledgeractivity.domain.InterestRate;

import javax.money.MonetaryAmount;
import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class DailyInterestCalculator {

    public MonetaryAmount calculateInterest(MonetaryAmount principal, List<InterestRate> interestRates, int daysInYear, LocalDateTime effectiveAt) {
        return calculateInterest(principal, getInterestRate(interestRates, effectiveAt), daysInYear);
    }

    public MonetaryAmount calculateInterest(MonetaryAmount principal, float interestRate, int daysInYear) {
        return principal.multiply(interestRate / daysInYear);
    }

    private float getInterestRate(List<InterestRate> interestRates, LocalDateTime effectiveAt) {
        return interestRates.stream()
                .filter(interestRate -> interestRate.getEffectiveAt().isBefore(effectiveAt))
                .findFirst()
                .map(InterestRate::getRate)
                .orElse(0f);
    }
}
