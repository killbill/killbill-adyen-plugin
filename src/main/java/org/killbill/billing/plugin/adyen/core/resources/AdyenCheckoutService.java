/*
 * Copyright 2021 Wovenware, Inc
 *
 * Wovenware licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package org.killbill.billing.plugin.adyen.core.resources;

import static org.killbill.billing.plugin.adyen.api.AdyenPaymentPluginApi.INTERNAL;
import static org.killbill.billing.plugin.adyen.api.AdyenPaymentPluginApi.SESSION_DATA;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.joda.time.DateTime;
import org.killbill.billing.account.api.Account;
import org.killbill.billing.account.api.AccountApiException;
import org.killbill.billing.osgi.libs.killbill.OSGIKillbillAPI;
import org.killbill.billing.payment.api.Payment;
import org.killbill.billing.payment.api.PaymentApiException;
import org.killbill.billing.payment.api.PluginProperty;
import org.killbill.billing.payment.plugin.api.PaymentPluginApiException;
import org.killbill.billing.payment.plugin.api.PaymentTransactionInfoPlugin;
import org.killbill.billing.plugin.adyen.core.AdyenConfigurationHandler;
import org.killbill.billing.util.callcontext.CallContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AdyenCheckoutService {
  private static final Logger logger = LoggerFactory.getLogger(AdyenCheckoutService.class);
  public static final String IS_CHECKOUT = "isCheckout";
  private OSGIKillbillAPI killbillAPI;

  private AdyenConfigurationHandler adyenConfigurationHandler;

  public AdyenCheckoutService(
      OSGIKillbillAPI killbillAPI, AdyenConfigurationHandler adyenConfigurationHandler) {
    this.killbillAPI = killbillAPI;
    this.adyenConfigurationHandler = adyenConfigurationHandler;
  }

  public Map<String, String> createSession(
      UUID kbAccountId, CallContext context, BigDecimal amount, UUID paymentMethodId, UUID tenantId)
      throws PaymentPluginApiException {

    killbillAPI
        .getSecurityApi()
        .login(
            adyenConfigurationHandler.getConfigurable(tenantId).getUsername(),
            adyenConfigurationHandler.getConfigurable(tenantId).getPassword());
    Account kbAccount = null;
    Payment payment = null;
    try {
      kbAccount = killbillAPI.getAccountUserApi().getAccountById(kbAccountId, context);

    } catch (AccountApiException e) {
      logger.error("Account Api {}", e.getMessage(), e);
      throw new PaymentPluginApiException(INTERNAL, e.getMessage());
    }
    List<PluginProperty> prop = new ArrayList<>();
    prop.add(new PluginProperty(IS_CHECKOUT, true, false));
    try {

      payment =
          killbillAPI
              .getPaymentApi()
              .createPurchase(
                  kbAccount,
                  paymentMethodId,
                  null,
                  amount,
                  kbAccount.getCurrency(),
                  DateTime.now(),
                  null,
                  null,
                  prop,
                  context);

    } catch (PaymentApiException e) {
      logger.error("Payment Api {}", e.getMessage(), e);
      throw new PaymentPluginApiException(INTERNAL, e.getMessage());
    }
    PaymentTransactionInfoPlugin paymentInfo =
        payment.getTransactions().get(payment.getTransactions().size() - 1).getPaymentInfoPlugin();

    Map<String, String> formFields = new HashMap<>();
    formFields.put("sessionId", paymentInfo.getFirstPaymentReferenceId());
    formFields.put(SESSION_DATA, paymentInfo.getProperties().get(0).getValue().toString());
    killbillAPI.getSecurityApi().logout();
    return formFields;
  }
}
