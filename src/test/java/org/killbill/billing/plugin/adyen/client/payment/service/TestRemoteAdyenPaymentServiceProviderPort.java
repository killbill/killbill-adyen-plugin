/*
 * Copyright 2014 Groupon, Inc
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

package org.killbill.billing.plugin.adyen.client.payment.service;

import java.util.Currency;
import java.util.UUID;

import org.killbill.billing.plugin.adyen.TestRemoteBase;
import org.killbill.billing.plugin.adyen.client.model.OrderData;
import org.killbill.billing.plugin.adyen.client.model.PaymentData;
import org.killbill.billing.plugin.adyen.client.model.PaymentModificationResponse;
import org.killbill.billing.plugin.adyen.client.model.PaymentProvider;
import org.killbill.billing.plugin.adyen.client.model.PaymentType;
import org.killbill.billing.plugin.adyen.client.model.PurchaseResult;
import org.killbill.billing.plugin.adyen.client.model.SplitSettlementData;
import org.killbill.billing.plugin.adyen.client.model.UserData;
import org.killbill.billing.plugin.adyen.client.model.paymentinfo.CreditCard;
import org.testng.Assert;
import org.testng.annotations.Test;

import static org.testng.Assert.assertFalse;

public class TestRemoteAdyenPaymentServiceProviderPort extends TestRemoteBase {

    @Test(groups = "slow")
    public void testAuthorizeAndMultiplePartialCaptures() throws Exception {
        final long authAmount = 10L;
        final PaymentData<CreditCard> paymentData = new PaymentData<CreditCard>();
        paymentData.setPaymentTxnInternalRef(UUID.randomUUID().toString());
        paymentData.setPaymentInfo(getCreditCard());
        final OrderData orderData = new OrderData();
        final UserData userData = new UserData();
        final String termUrl = null;
        final SplitSettlementData splitSettlementData = null;

        final PurchaseResult authorizeResult = adyenPaymentServiceProviderPort.authorise(authAmount, paymentData, orderData, userData, termUrl, splitSettlementData);
        // Adyen's unique reference that is associated with the payment
        Assert.assertNotNull(authorizeResult.getPspReference());
        // Result of the payment. The possible values are Authorised, Refused, Error or Received (as with a Dutch Direct Debit)
        Assert.assertEquals(authorizeResult.getResultCode(), "Authorised");
        // The authorisation code if the payment was successful. Blank otherwise
        Assert.assertNotNull(authorizeResult.getAuthCode());
        // Adyen's mapped refusal reason, populated if the payment was refused
        Assert.assertNull(authorizeResult.getReason());

        final long captureAmount = 5L;
        final PaymentProvider paymentProvider = paymentData.getPaymentInfo().getPaymentProvider();
        final String pspReference = authorizeResult.getPspReference();

        // First capture
        final PaymentModificationResponse capture1Result = adyenPaymentServiceProviderPort.capture(captureAmount, paymentProvider, pspReference, splitSettlementData);
        Assert.assertNotNull(capture1Result.getPspReference());
        Assert.assertEquals(capture1Result.getResponse(), "[capture-received]");

        // Second capture
        final PaymentModificationResponse capture2Result = adyenPaymentServiceProviderPort.capture(captureAmount, paymentProvider, pspReference, splitSettlementData);
        Assert.assertNotNull(capture2Result.getPspReference());
        Assert.assertEquals(capture2Result.getResponse(), "[capture-received]");
    }

    @Test(groups = "slow")
    public void testAuthorizeAndVoid() throws Exception {
        final long authAmount = 10L;
        final PaymentData<CreditCard> paymentData = new PaymentData<CreditCard>();
        paymentData.setPaymentTxnInternalRef(UUID.randomUUID().toString());
        paymentData.setPaymentInfo(getCreditCard());
        final OrderData orderData = new OrderData();
        final UserData userData = new UserData();
        final String termUrl = null;
        final SplitSettlementData splitSettlementData = null;

        final PurchaseResult authorizeResult = adyenPaymentServiceProviderPort.authorise(authAmount, paymentData, orderData, userData, termUrl, splitSettlementData);
        Assert.assertNotNull(authorizeResult.getPspReference());
        Assert.assertEquals(authorizeResult.getResultCode(), "Authorised");
        Assert.assertNotNull(authorizeResult.getAuthCode());
        Assert.assertNull(authorizeResult.getReason());

        final PaymentProvider paymentProvider = paymentData.getPaymentInfo().getPaymentProvider();
        final String pspReference = authorizeResult.getPspReference();

        final PaymentModificationResponse voidResult = adyenPaymentServiceProviderPort.cancel(paymentProvider, pspReference, splitSettlementData);
        Assert.assertNotNull(voidResult.getPspReference());
        Assert.assertEquals(voidResult.getResponse(), "[cancel-received]");
    }

    @Test(groups = "slow")
    public void testAuthorizeCaptureAndRefund() throws Exception {
        final long authAmount = 10L;
        final PaymentData<CreditCard> paymentData = new PaymentData<CreditCard>();
        paymentData.setPaymentTxnInternalRef(UUID.randomUUID().toString());
        paymentData.setPaymentInfo(getCreditCard());
        final OrderData orderData = new OrderData();
        final UserData userData = new UserData();
        final String termUrl = null;
        final SplitSettlementData splitSettlementData = null;

        final PurchaseResult authorizeResult = adyenPaymentServiceProviderPort.authorise(authAmount, paymentData, orderData, userData, termUrl, splitSettlementData);
        Assert.assertNotNull(authorizeResult.getPspReference());
        Assert.assertEquals(authorizeResult.getResultCode(), "Authorised");
        Assert.assertNotNull(authorizeResult.getAuthCode());
        Assert.assertNull(authorizeResult.getReason());

        final long captureAmount = authAmount;
        final PaymentProvider paymentProvider = paymentData.getPaymentInfo().getPaymentProvider();
        final String capturePspReference = authorizeResult.getPspReference();

        final PaymentModificationResponse captureResult = adyenPaymentServiceProviderPort.capture(captureAmount, paymentProvider, capturePspReference, splitSettlementData);
        Assert.assertNotNull(captureResult.getPspReference());
        Assert.assertEquals(captureResult.getResponse(), "[capture-received]");

        final long refundAmount = captureAmount;
        final String refundPspReference = captureResult.getPspReference();

        final PaymentModificationResponse refundResult = adyenPaymentServiceProviderPort.refund(refundAmount, paymentProvider, refundPspReference, splitSettlementData);
        Assert.assertNotNull(refundResult.getPspReference());
        Assert.assertEquals(refundResult.getResponse(), "[refund-received]");
    }

    @Test(groups = "slow")
    public void testAuthorizeAndBadVoid() throws Exception {
        final long authAmount = 10L;
        final PaymentData<CreditCard> paymentData = new PaymentData<CreditCard>();
        paymentData.setPaymentTxnInternalRef(UUID.randomUUID().toString());
        paymentData.setPaymentInfo(getCreditCard());
        final OrderData orderData = new OrderData();
        final UserData userData = new UserData();
        final String termUrl = null;
        final SplitSettlementData splitSettlementData = null;

        final PurchaseResult authorizeResult = adyenPaymentServiceProviderPort.authorise(authAmount, paymentData, orderData, userData, termUrl, splitSettlementData);
        Assert.assertNotNull(authorizeResult.getPspReference());
        Assert.assertEquals(authorizeResult.getResultCode(), "Authorised");
        Assert.assertNotNull(authorizeResult.getAuthCode());
        Assert.assertNull(authorizeResult.getReason());

        final PaymentProvider paymentProvider = paymentData.getPaymentInfo().getPaymentProvider();
        final String pspReference = UUID.randomUUID().toString();

        final PaymentModificationResponse voidResult = adyenPaymentServiceProviderPort.cancel(paymentProvider, pspReference, splitSettlementData);
        assertFalse(voidResult.isSuccess());
    }

    private CreditCard getCreditCard() {
        final PaymentProvider paymentProvider = new PaymentProvider(adyenConfigProperties);
        paymentProvider.setCurrency(Currency.getInstance(DEFAULT_CURRENCY.name()));
        paymentProvider.setCountryIsoCode(DEFAULT_COUNTRY);
        paymentProvider.setPaymentType(PaymentType.CREDITCARD);

        final CreditCard paymentInfo = new CreditCard(paymentProvider);
        paymentInfo.setCcHolderName("Dupont");
        paymentInfo.setCcNumber(CC_NUMBER);
        paymentInfo.setValidUntilMonth(CC_EXPIRATION_MONTH);
        paymentInfo.setValidUntilYear(CC_EXPIRATION_YEAR);
        paymentInfo.setCcSecCode(CC_VERIFICATION_VALUE);

        return paymentInfo;
    }
}
