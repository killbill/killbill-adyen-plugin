package org.killbill.billing.plugin.adyen.recurring;

import org.killbill.adyen.payment.Recurring;
import org.killbill.adyen.recurring.DisableRequest;
import org.killbill.adyen.recurring.RecurringDetail;
import org.killbill.adyen.recurring.RecurringDetailsRequest;
import org.killbill.adyen.recurring.RecurringPortType;
import org.killbill.adyen.recurring.ServiceException;
import org.killbill.billing.plugin.adyen.client.AdyenConfigProperties;

import java.util.List;

public class AdyenRecurringClient {

    private final RecurringPortRegistry recurringPortRegistry;

    public AdyenRecurringClient(final AdyenConfigProperties config) {
        this.recurringPortRegistry = new AdyenRecurringPortRegistry(config);
    }

    public List<RecurringDetail> getRecurringDetailList(final String countryCode,
                                                        final String shopperRef,
                                                        final String merchantAccount,
                                                        final String contract) throws ServiceException {
        final RecurringPortType recurringPortType = recurringPortRegistry.getRecurringPort(countryCode);
        final RecurringDetailsRequest request = new RecurringDetailsRequest();
        final Recurring recurring = new Recurring();
        request.setShopperReference(shopperRef);
        request.setMerchantAccount(merchantAccount);
        recurring.setContract(contract);
        request.setRecurring(recurring);
        return recurringPortType.listRecurringDetails(request).getDetails().getRecurringDetail();
    }

    public void revokeRecurringDetails(final String countryCode,
                                       final String shopperRef,
                                       final String merchantAccount) throws ServiceException {
        final RecurringPortType recurringPortType = recurringPortRegistry.getRecurringPort(countryCode);
        final DisableRequest request = new DisableRequest();
        request.setShopperReference(shopperRef);
        request.setMerchantAccount(merchantAccount);
        request.setContract("RECURRING,ONECLICK");
        recurringPortType.disable(request);
    }

}
