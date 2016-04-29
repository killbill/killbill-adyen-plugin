/*
 * Copyright 2014-2016 Groupon, Inc
 * Copyright 2014-2016 The Billing Project, LLC
 *
 * The Billing Project licenses this file to you under the Apache License, version 2.0
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

import java.math.BigDecimal;
import java.util.UUID;

import org.killbill.billing.plugin.adyen.TestRemoteBase;
import org.killbill.billing.plugin.adyen.client.model.PaymentData;
import org.killbill.billing.plugin.adyen.client.model.PaymentModificationResponse;
import org.killbill.billing.plugin.adyen.client.model.PurchaseResult;
import org.killbill.billing.plugin.adyen.client.model.SplitSettlementData;
import org.killbill.billing.plugin.adyen.client.model.UserData;
import org.killbill.billing.plugin.adyen.client.model.paymentinfo.Card;
import org.testng.Assert;
import org.testng.annotations.Test;

import static org.testng.Assert.assertFalse;

public class TestRemoteAdyenPaymentServiceProviderPort extends TestRemoteBase {

    @Test(groups = "slow")
    public void testAuthorizeAndMultiplePartialCaptures() throws Exception {
        final PaymentData<Card> paymentData = new PaymentData<Card>(BigDecimal.TEN, DEFAULT_CURRENCY, UUID.randomUUID().toString(), getCreditCard());
        final UserData userData = new UserData();
        final SplitSettlementData splitSettlementData = null;

        final PurchaseResult authorizeResult = adyenPaymentServiceProviderPort.authorise(paymentData, userData, splitSettlementData);
        // Adyen's unique reference that is associated with the payment
        Assert.assertNotNull(authorizeResult.getPspReference());
        // Result of the payment. The possible values are Authorised, Refused, Error or Received (as with a Dutch Direct Debit)
        Assert.assertEquals(authorizeResult.getResultCode(), "Authorised");
        // The authorisation code if the payment was successful. Blank otherwise
        Assert.assertNotNull(authorizeResult.getAuthCode());
        // Adyen's mapped refusal reason, populated if the payment was refused
        Assert.assertNull(authorizeResult.getReason());

        final BigDecimal captureAmount = new BigDecimal("5");
        final String pspReference = authorizeResult.getPspReference();

        // First capture
        final PaymentData paymentData1 = new PaymentData<Card>(captureAmount, DEFAULT_CURRENCY, UUID.randomUUID().toString(), getCreditCard());
        final PaymentModificationResponse capture1Result = adyenPaymentServiceProviderPort.capture(paymentData1, pspReference, splitSettlementData);
        Assert.assertNotNull(capture1Result.getPspReference());
        Assert.assertEquals(capture1Result.getResponse(), "[capture-received]");

        // Second capture
        final PaymentData paymentData2 = new PaymentData<Card>(captureAmount, DEFAULT_CURRENCY, UUID.randomUUID().toString(), getCreditCard());
        final PaymentModificationResponse capture2Result = adyenPaymentServiceProviderPort.capture(paymentData2, pspReference, splitSettlementData);
        Assert.assertNotNull(capture2Result.getPspReference());
        Assert.assertEquals(capture2Result.getResponse(), "[capture-received]");
    }

    @Test(groups = "slow")
    public void testAuthorizeAndVoid() throws Exception {
        final PaymentData<Card> paymentData = new PaymentData<Card>(BigDecimal.TEN, DEFAULT_CURRENCY, UUID.randomUUID().toString(), getCreditCard());
        final UserData userData = new UserData();
        final SplitSettlementData splitSettlementData = null;

        final PurchaseResult authorizeResult = adyenPaymentServiceProviderPort.authorise(paymentData, userData, splitSettlementData);
        Assert.assertNotNull(authorizeResult.getPspReference());
        Assert.assertEquals(authorizeResult.getResultCode(), "Authorised");
        Assert.assertNotNull(authorizeResult.getAuthCode());
        Assert.assertNull(authorizeResult.getReason());

        final String pspReference = authorizeResult.getPspReference();

        final PaymentData paymentData1 = new PaymentData<Card>(null, null, UUID.randomUUID().toString(), getCreditCard());
        final PaymentModificationResponse voidResult = adyenPaymentServiceProviderPort.cancel(paymentData1, pspReference, splitSettlementData);
        Assert.assertNotNull(voidResult.getPspReference());
        Assert.assertEquals(voidResult.getResponse(), "[cancel-received]");
    }

    @Test(groups = "slow")
    public void testAuthorizeCaptureAndRefund() throws Exception {
        final PaymentData<Card> paymentData = new PaymentData<Card>(BigDecimal.TEN, DEFAULT_CURRENCY, UUID.randomUUID().toString(), getCreditCard());
        final UserData userData = new UserData();
        final SplitSettlementData splitSettlementData = null;

        final PurchaseResult authorizeResult = adyenPaymentServiceProviderPort.authorise(paymentData, userData, splitSettlementData);
        Assert.assertNotNull(authorizeResult.getPspReference());
        Assert.assertEquals(authorizeResult.getResultCode(), "Authorised");
        Assert.assertNotNull(authorizeResult.getAuthCode());
        Assert.assertNull(authorizeResult.getReason());

        //noinspection UnnecessaryLocalVariable
        final BigDecimal captureAmount = BigDecimal.TEN;
        final String capturePspReference = authorizeResult.getPspReference();

        final PaymentData paymentData1 = new PaymentData<Card>(captureAmount, DEFAULT_CURRENCY, UUID.randomUUID().toString(), getCreditCard());
        final PaymentModificationResponse captureResult = adyenPaymentServiceProviderPort.capture(paymentData1, capturePspReference, splitSettlementData);
        Assert.assertNotNull(captureResult.getPspReference());
        Assert.assertEquals(captureResult.getResponse(), "[capture-received]");

        //noinspection UnnecessaryLocalVariable
        final BigDecimal refundAmount = captureAmount;
        final String refundPspReference = captureResult.getPspReference();

        final PaymentData paymentData2 = new PaymentData<Card>(refundAmount, DEFAULT_CURRENCY, UUID.randomUUID().toString(), getCreditCard());
        final PaymentModificationResponse refundResult = adyenPaymentServiceProviderPort.refund(paymentData2, refundPspReference, splitSettlementData);
        Assert.assertNotNull(refundResult.getPspReference());
        Assert.assertEquals(refundResult.getResponse(), "[refund-received]");
    }

    @Test(groups = "slow")
    public void testAuthorizeAndBadVoid() throws Exception {
        final PaymentData<Card> paymentData = new PaymentData<Card>(BigDecimal.TEN, DEFAULT_CURRENCY, UUID.randomUUID().toString(), getCreditCard());
        final UserData userData = new UserData();
        final SplitSettlementData splitSettlementData = null;

        final PurchaseResult authorizeResult = adyenPaymentServiceProviderPort.authorise(paymentData, userData, splitSettlementData);
        Assert.assertNotNull(authorizeResult.getPspReference());
        Assert.assertEquals(authorizeResult.getResultCode(), "Authorised");
        Assert.assertNotNull(authorizeResult.getAuthCode());
        Assert.assertNull(authorizeResult.getReason());

        final String pspReference = UUID.randomUUID().toString();

        final PaymentData paymentData1 = new PaymentData<Card>(null, null, UUID.randomUUID().toString(), getCreditCard());
        final PaymentModificationResponse voidResult = adyenPaymentServiceProviderPort.cancel(paymentData1, pspReference, splitSettlementData);
        assertFalse(voidResult.isTechnicallySuccessful());
    }

    private Card getCreditCard() {
        final Card paymentInfo = new Card();
        paymentInfo.setHolderName("Dupont");
        paymentInfo.setNumber(CC_NUMBER);
        paymentInfo.setExpiryMonth(CC_EXPIRATION_MONTH);
        paymentInfo.setExpiryYear(CC_EXPIRATION_YEAR);
        paymentInfo.setCvc(CC_VERIFICATION_VALUE);

        paymentInfo.setCountry(DEFAULT_COUNTRY);

        return paymentInfo;
    }
}
