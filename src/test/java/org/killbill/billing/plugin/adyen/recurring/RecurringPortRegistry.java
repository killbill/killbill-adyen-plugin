package org.killbill.billing.plugin.adyen.recurring;

import org.killbill.adyen.recurring.RecurringPortType;

public interface RecurringPortRegistry {

    RecurringPortType getRecurringPort(String countryIsoCode);

}
