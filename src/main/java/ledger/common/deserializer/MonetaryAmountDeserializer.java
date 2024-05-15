package ledger.common.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import javax.money.Monetary;
import javax.money.MonetaryAmount;
import java.io.IOException;

import static ledger.common.MonetaryUtil.DEFAULT_CURRENCY_CODE;

public class MonetaryAmountDeserializer extends JsonDeserializer<MonetaryAmount> {

    @Override
    public MonetaryAmount deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        return Monetary.getDefaultAmountFactory()
                .setCurrency(DEFAULT_CURRENCY_CODE) // replace with your currency
                .setNumber(jsonParser.getDecimalValue())
                .create();
    }
}