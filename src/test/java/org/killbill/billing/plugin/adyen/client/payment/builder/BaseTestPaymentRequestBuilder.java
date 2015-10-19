package org.killbill.billing.plugin.adyen.client.payment.builder;


import org.killbill.billing.plugin.adyen.client.model.RecurringType;
import org.testng.annotations.DataProvider;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public abstract class BaseTestPaymentRequestBuilder {

    protected static final String DP_RECURRING_TYPES = "RecurringTypes";

    @DataProvider(name = DP_RECURRING_TYPES)
    public Iterator<Object[]> recurringTypesdataProvider() {
        final List<Object[]> recurringTypes = new ArrayList<Object[]>(RecurringType.values().length);
        for (final RecurringType recurringType : RecurringType.values()) {
            recurringTypes.add(new Object[] {recurringType});
        }
        return recurringTypes.iterator();
    }

}
