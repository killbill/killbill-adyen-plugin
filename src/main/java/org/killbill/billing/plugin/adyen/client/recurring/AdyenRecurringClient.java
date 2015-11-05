/*
 * Copyright 2015 Groupon, Inc
 *
 * Groupon licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package org.killbill.billing.plugin.adyen.client.recurring;

import java.util.List;

import org.killbill.adyen.payment.Recurring;
import org.killbill.adyen.recurring.ArrayOfRecurringDetail;
import org.killbill.adyen.recurring.DisableRequest;
import org.killbill.adyen.recurring.RecurringDetail;
import org.killbill.adyen.recurring.RecurringDetailsRequest;
import org.killbill.adyen.recurring.RecurringPortType;
import org.killbill.adyen.recurring.ServiceException;
import org.killbill.billing.plugin.adyen.client.AdyenConfigProperties;

import com.google.common.collect.ImmutableList;

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
        final ArrayOfRecurringDetail details = recurringPortType.listRecurringDetails(request).getDetails();
        return details == null ? ImmutableList.<RecurringDetail>of() : details.getRecurringDetail();
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
