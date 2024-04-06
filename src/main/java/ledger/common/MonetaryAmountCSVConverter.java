package ledger.common;

import com.opencsv.bean.AbstractBeanField;

import javax.money.MonetaryAmount;

public class MonetaryAmountCSVConverter extends AbstractBeanField<MonetaryAmount, String> {

    @Override
    protected MonetaryAmount convert(String value) {
        return MonetaryUtil.monetaryAmount(Double.parseDouble(value));
    }
}
