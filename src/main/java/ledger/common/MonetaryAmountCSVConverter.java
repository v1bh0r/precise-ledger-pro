package ledger.common;

import com.opencsv.bean.AbstractBeanField;

import javax.money.MonetaryAmount;

public class MonetaryAmountCSVConverter extends AbstractBeanField<MonetaryAmount, String> {

    @Override
    protected MonetaryAmount convert(String value) {
        if (value == null || value.isEmpty())
            return MonetaryUtil.zero();
        else {
            return MonetaryUtil.monetaryAmount(Double.parseDouble(value));
        }
    }
}
