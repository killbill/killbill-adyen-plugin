/*
 * Copyright 2020-2023 Equinix, Inc
 * Copyright 2014-2023 The Billing Project, LLC
 *
 * Ning licenses this file to you under the Apache License, version 2.0
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
package org.killbill.billing.plugin.adyen.client;

import com.adyen.Client;
import com.adyen.enums.Environment;
import com.adyen.model.Amount;
import com.adyen.model.checkout.CreateCheckoutSessionRequest;
import com.adyen.model.checkout.CreateCheckoutSessionRequest.RecurringProcessingModelEnum;
import com.adyen.model.checkout.CreateCheckoutSessionRequest.ShopperInteractionEnum;
import com.adyen.model.checkout.CreateCheckoutSessionResponse;
import com.adyen.model.checkout.CreatePaymentRefundRequest;
import com.adyen.model.checkout.CreatePaymentReversalRequest;
import com.adyen.model.checkout.PaymentRefundResource;
import com.adyen.model.checkout.PaymentReversalResource;
import com.adyen.model.checkout.PaymentsRequest;
import com.adyen.model.checkout.PaymentsResponse;
import com.adyen.service.Checkout;
import com.adyen.service.exception.ApiException;
import java.io.IOException;
import java.math.BigDecimal;
import org.killbill.billing.catalog.api.Currency;
import org.killbill.billing.plugin.adyen.core.AdyenConfigProperties;

public class AdyenSDKClientImpl implements AdyenSDKClient {

  private final AdyenConfigProperties adyenConfigProperties;
  private final Checkout checkout;

  public AdyenSDKClientImpl(AdyenConfigProperties adyenConfigProperties) {
    this.adyenConfigProperties = adyenConfigProperties;
    Client client =
        new Client(
            adyenConfigProperties.getApiKey(),
            Environment.valueOf(adyenConfigProperties.getEnviroment()));
    this.checkout = new Checkout(client);
  }

  @Override
  public CreateCheckoutSessionResponse checkoutsessions(
      Currency currency,
      BigDecimal kbAmount,
      String kbTransactionId,
      String kbAccountId,
      boolean isRecurrent)
      throws IOException, ApiException {

    Amount amount = new Amount().currency(currency.name()).value(convertToMinorUnit(kbAmount));
    CreateCheckoutSessionRequest checkoutSession = new CreateCheckoutSessionRequest();
    checkoutSession.merchantAccount(adyenConfigProperties.getMerchantAccount());
    checkoutSession.setChannel(CreateCheckoutSessionRequest.ChannelEnum.WEB);
    checkoutSession.setReference(kbTransactionId);
    checkoutSession.setReturnUrl(adyenConfigProperties.getReturnUrl());
    checkoutSession.setAmount(amount);
    checkoutSession.setCountryCode(adyenConfigProperties.getRegion());
    checkoutSession.setCaptureDelayHours(
        Integer.valueOf(adyenConfigProperties.getCaptureDelayHours()));
    checkoutSession.setShopperReference(kbAccountId);
    if (isRecurrent) {
      checkoutSession.setRecurringProcessingModel(RecurringProcessingModelEnum.CARDONFILE);
      checkoutSession.shopperInteraction(ShopperInteractionEnum.ECOMMERCE);
      checkoutSession.storePaymentMethod(true);
    }

    return checkout.sessions(checkoutSession);
  }

  @Override
  public PaymentReversalResource reversal(String transactionId, String paymentPspReference)
      throws IOException, ApiException {

    CreatePaymentReversalRequest paymentReversalRequest = new CreatePaymentReversalRequest();

    paymentReversalRequest.setMerchantAccount(adyenConfigProperties.getMerchantAccount());
    paymentReversalRequest.setReference(transactionId);

    return checkout.paymentsReversals(paymentPspReference, paymentReversalRequest);
  }

  public PaymentRefundResource refund(
      Currency currency, BigDecimal kbAmount, String transactionId, String paymentPspReference)
      throws IOException, ApiException {

    CreatePaymentRefundRequest paymentRefundRequest = new CreatePaymentRefundRequest();
    Amount amount = new Amount().currency(currency.name()).value(convertToMinorUnit(kbAmount));
    paymentRefundRequest.setAmount(amount);
    paymentRefundRequest.setMerchantAccount(adyenConfigProperties.getMerchantAccount());
    paymentRefundRequest.setReference(transactionId);
    return checkout.paymentsRefunds(paymentPspReference, paymentRefundRequest);
  }

  @Override
  public PaymentsResponse purchase(
      Currency currency,
      BigDecimal kbAmount,
      String transactionId,
      String kbAccountId,
      String recurringDetailReference)
      throws IOException, ApiException {
    PaymentsRequest paymentsRequest = new PaymentsRequest();
    Amount amount = new Amount().currency(currency.name()).value(convertToMinorUnit(kbAmount));
    paymentsRequest.setAmount(amount);
    paymentsRequest.setReference(transactionId);

    paymentsRequest.setShopperReference(kbAccountId);
    paymentsRequest.setReturnUrl(adyenConfigProperties.getReturnUrl());
    paymentsRequest.setMerchantAccount(adyenConfigProperties.getMerchantAccount());
    paymentsRequest.setShopperInteraction(PaymentsRequest.ShopperInteractionEnum.CONTAUTH);
    paymentsRequest.setRecurringProcessingModel(
        PaymentsRequest.RecurringProcessingModelEnum.CARD_ON_FILE);
    paymentsRequest.addOneClickData(recurringDetailReference, null);

    paymentsRequest.setCaptureDelayHours(
        Integer.valueOf(adyenConfigProperties.getCaptureDelayHours()));

    return checkout.payments(paymentsRequest);
  }

  private Long convertToMinorUnit(BigDecimal amount) {

    String minorUnit = amount.toString().replace(".", "");

    return Long.valueOf(minorUnit);
  }
}
