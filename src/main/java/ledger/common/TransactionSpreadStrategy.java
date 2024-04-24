package ledger.common;

import ledger.model.Balance;
import ledger.model.Direction;
import lombok.Getter;
import lombok.NonNull;

@Getter
public abstract class TransactionSpreadStrategy {
    @NonNull
    Direction direction;

    public TransactionSpreadStrategy(@NonNull Direction direction) {
        this.direction = direction;
    }

    public abstract Balance applyTo(Balance balance);
}
