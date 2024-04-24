package ledger.common.ledgeractivity.transactionactivity;

import ledger.common.TransactionSpreadStrategy;
import ledger.common.ledgeractivity.transactionactivity.transactionspreadstrategy.ComputationalSpread;
import ledger.common.ledgeractivity.transactionactivity.transactionspreadstrategy.StaticSpread;
import ledger.model.Balance;
import ledger.model.Direction;
import ledger.model.GeneralLedgerActivity;

public class TransactionSpreadStrategyFactory {
    public static TransactionSpreadStrategy create(GeneralLedgerActivity generalLedgerActivity) {
        var direction = Direction.valueOf(generalLedgerActivity.getDirection());
        return switch (generalLedgerActivity.getTransactionStrategy()) {
            case "ComputationalSpread" ->
                    new ComputationalSpread(generalLedgerActivity.getAmount(), direction, generalLedgerActivity.getSpread());
            case "StaticSpread" ->
                    new StaticSpread(new Balance(generalLedgerActivity.getPrincipal(), generalLedgerActivity.getInterest(), generalLedgerActivity.getFee(), generalLedgerActivity.getExcess()), direction);
            default -> {
                throw new RuntimeException("LoanId: " + generalLedgerActivity.getLoanId() + " - Unknown transaction spread strategy: " + generalLedgerActivity.getTransactionStrategy());
            }
        };
    }
}
