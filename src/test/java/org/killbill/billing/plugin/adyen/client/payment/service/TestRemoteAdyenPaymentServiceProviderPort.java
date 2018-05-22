/*
 * Copyright 2014-2018 Groupon, Inc
 * Copyright 2014-2018 The Billing Project, LLC
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
import java.util.Map;
import java.util.UUID;

import org.killbill.billing.catalog.api.Currency;
import org.killbill.billing.plugin.adyen.TestRemoteBase;
import org.killbill.billing.plugin.adyen.client.model.PaymentData;
import org.killbill.billing.plugin.adyen.client.model.PaymentInfo;
import org.killbill.billing.plugin.adyen.client.model.PaymentModificationResponse;
import org.killbill.billing.plugin.adyen.client.model.PurchaseResult;
import org.killbill.billing.plugin.adyen.client.model.SplitSettlementData;
import org.killbill.billing.plugin.adyen.client.model.UserData;
import org.killbill.billing.plugin.adyen.client.model.paymentinfo.Card;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.google.common.base.Charsets;
import com.google.common.io.BaseEncoding;

import static org.testng.Assert.assertFalse;

public class TestRemoteAdyenPaymentServiceProviderPort extends TestRemoteBase {

    @Test(groups = "integration")
    public void testAuthorizeAndMultiplePartialCaptures() throws Exception {
        final PaymentData<Card> paymentData = new PaymentData<Card>(BigDecimal.TEN, DEFAULT_CURRENCY, UUID.randomUUID().toString(), getCreditCard());
        final UserData userData = new UserData();
        final SplitSettlementData splitSettlementData = null;
        final Map<String, String> additionalData = null;

        final PurchaseResult authorizeResult = adyenPaymentServiceProviderPort.authorise(merchantAccount, paymentData, userData, splitSettlementData, additionalData);
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
        final PaymentModificationResponse capture1Result = adyenPaymentServiceProviderPort.capture(merchantAccount, paymentData1, pspReference, splitSettlementData, additionalData);
        Assert.assertNotNull(capture1Result.getPspReference());
        Assert.assertEquals(capture1Result.getResponse(), "[capture-received]");

        // Second capture
        final PaymentData paymentData2 = new PaymentData<Card>(captureAmount, DEFAULT_CURRENCY, UUID.randomUUID().toString(), getCreditCard());
        final PaymentModificationResponse capture2Result = adyenPaymentServiceProviderPort.capture(merchantAccount, paymentData2, pspReference, splitSettlementData, additionalData);
        Assert.assertNotNull(capture2Result.getPspReference());
        Assert.assertEquals(capture2Result.getResponse(), "[capture-received]");
    }

    @Test(groups = "integration")
    public void testAuthorizeAndVoid() throws Exception {
        final PaymentData<Card> paymentData = new PaymentData<Card>(BigDecimal.TEN, DEFAULT_CURRENCY, UUID.randomUUID().toString(), getCreditCard());
        final UserData userData = new UserData();
        final SplitSettlementData splitSettlementData = null;
        final Map<String, String> additionalData = null;

        final PurchaseResult authorizeResult = adyenPaymentServiceProviderPort.authorise(merchantAccount, paymentData, userData, splitSettlementData, additionalData);
        Assert.assertNotNull(authorizeResult.getPspReference());
        Assert.assertEquals(authorizeResult.getResultCode(), "Authorised");
        Assert.assertNotNull(authorizeResult.getAuthCode());
        Assert.assertNull(authorizeResult.getReason());

        final String pspReference = authorizeResult.getPspReference();

        final PaymentData paymentData1 = new PaymentData<Card>(null, null, UUID.randomUUID().toString(), getCreditCard());
        final PaymentModificationResponse voidResult = adyenPaymentServiceProviderPort.cancel(merchantAccount, paymentData1, pspReference, splitSettlementData, additionalData);
        Assert.assertNotNull(voidResult.getPspReference());
        Assert.assertEquals(voidResult.getResponse(), "[cancel-received]");
    }

    @Test(groups = "integration")
    public void testAuthorizeCaptureAndRefund() throws Exception {
        final PaymentData<Card> paymentData = new PaymentData<Card>(BigDecimal.TEN, DEFAULT_CURRENCY, UUID.randomUUID().toString(), getCreditCard());
        final UserData userData = new UserData();
        final SplitSettlementData splitSettlementData = null;
        final Map<String, String> additionalData = null;

        final PurchaseResult authorizeResult = adyenPaymentServiceProviderPort.authorise(merchantAccount, paymentData, userData, splitSettlementData, additionalData);
        Assert.assertNotNull(authorizeResult.getPspReference());
        Assert.assertEquals(authorizeResult.getResultCode(), "Authorised");
        Assert.assertNotNull(authorizeResult.getAuthCode());
        Assert.assertNull(authorizeResult.getReason());

        //noinspection UnnecessaryLocalVariable
        final BigDecimal captureAmount = BigDecimal.TEN;
        final String capturePspReference = authorizeResult.getPspReference();

        final PaymentData paymentData1 = new PaymentData<Card>(captureAmount, DEFAULT_CURRENCY, UUID.randomUUID().toString(), getCreditCard());
        final PaymentModificationResponse captureResult = adyenPaymentServiceProviderPort.capture(merchantAccount, paymentData1, capturePspReference, splitSettlementData, additionalData);
        Assert.assertNotNull(captureResult.getPspReference());
        Assert.assertEquals(captureResult.getResponse(), "[capture-received]");

        //noinspection UnnecessaryLocalVariable
        final BigDecimal refundAmount = captureAmount;
        final String refundPspReference = captureResult.getPspReference();

        final PaymentData paymentData2 = new PaymentData<Card>(refundAmount, DEFAULT_CURRENCY, UUID.randomUUID().toString(), getCreditCard());
        final PaymentModificationResponse refundResult = adyenPaymentServiceProviderPort.refund(merchantAccount, paymentData2, refundPspReference, splitSettlementData, additionalData);
        Assert.assertNotNull(refundResult.getPspReference());
        Assert.assertEquals(refundResult.getResponse(), "[refund-received]");
    }

    @Test(groups = "integration")
    public void testAuthorizeAndBadVoid() throws Exception {
        final PaymentData<Card> paymentData = new PaymentData<Card>(BigDecimal.TEN, DEFAULT_CURRENCY, UUID.randomUUID().toString(), getCreditCard());
        final UserData userData = new UserData();
        final SplitSettlementData splitSettlementData = null;
        final Map<String, String> additionalData = null;

        final PurchaseResult authorizeResult = adyenPaymentServiceProviderPort.authorise(merchantAccount, paymentData, userData, splitSettlementData, additionalData);
        Assert.assertNotNull(authorizeResult.getPspReference());
        Assert.assertEquals(authorizeResult.getResultCode(), "Authorised");
        Assert.assertNotNull(authorizeResult.getAuthCode());
        Assert.assertNull(authorizeResult.getReason());

        final String pspReference = UUID.randomUUID().toString();

        final PaymentData paymentData1 = new PaymentData<Card>(null, null, UUID.randomUUID().toString(), getCreditCard());
        final PaymentModificationResponse voidResult = adyenPaymentServiceProviderPort.cancel(merchantAccount, paymentData1, pspReference, splitSettlementData, additionalData);
        assertFalse(voidResult.isTechnicallySuccessful());
    }

    // Disabled by default since Apple Pay isn't enabled automatically on the sandbox
    @Test(groups = "integration", enabled = false)
    public void testAutoCaptureWithApplePay() throws Exception {
        final Card creditCard = new Card();
        creditCard.setCountry(DEFAULT_COUNTRY);
        creditCard.setCaptureDelayHours(0);
        // https://github.com/Adyen/AdyenPay-iOS/wiki/Adyen-Apple-Pay-Test-Tokens
        final String rawToken = "{\"version\":\"Adyen_Test\",\"signature\":\"ZmFrZSBzaWduYXR1cmU=\",\"header\":{\"ephemeralPublicKey\":\"MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEWxfzrzPtm97IUgtBxqjYWIVVaQt80yLoRtAbLWzspJSkX9lRbacQ0ATlTMXUYkQuvhiOIAfqyZjtR0CPuX0ajA==\",\"publicKeyHash\":\"OrWgjRGkqEWjdkRdUrXfiLGD0he/zpEu512FJWrGYFo=\",\"transactionId\":\"1234567890ABCDEF\"},\"data\":\"ffiOJlsIrYkVSIumRqrJhdf2XOZKIzmBS2lQTxyiV+W0R0d3JCc6Dfb3Ysp2tz1NGmZSR02tWOgnhjZd4a0LxUf5ezv890BbElTpCM3RiKrC/YEYKluFtDhYJSa2WyYxLjSr6T22gn+iQ1Gik+41qbxhQKcqlz9WkXIkDCfXX81Cdc7AE4oSWjuhnpw/PFf4uGD2V7n8W5YpGybQOzcfbo53lqSA+nKjEvDgARwjelnsL1vCszOwLXEbWimW10YE32ZYriLPyi8TU7T5OkNNDM1b8obnC1EU8RA14H8lmvBlN7rywu8lxKWAA/w0D3zBefgTqonFpClyJOfqQ3KtWsQH2yTfzXnyx2yqfRaeUIgpdwrqvYNJkVsOY8P3e/QO8U8TO7bcd83vQ0vxXafTMbwSYO6+bScPednH3+Y6R40THRpTjSuXJd6P2C/o4OA1Bm+Y9+E6nWzDuUUr3oLUEzRziUkzmbKV/iGTAJDBGxD0QAIdzca0\"}";
        creditCard.setToken(BaseEncoding.base64().encode(rawToken.getBytes(Charsets.US_ASCII)));

        final PaymentData<Card> paymentData = new PaymentData<Card>(new BigDecimal("1"), DEFAULT_CURRENCY, UUID.randomUUID().toString(), creditCard);
        final UserData userData = new UserData();
        final SplitSettlementData splitSettlementData = null;
        final Map<String, String> additionalData = null;

        final PurchaseResult authorizeResult = adyenPaymentServiceProviderPort.authorise(merchantAccount, paymentData, userData, splitSettlementData, additionalData);
        Assert.assertNotNull(authorizeResult.getPspReference());
        Assert.assertEquals(authorizeResult.getResultCode(), "Authorised");
        Assert.assertNotNull(authorizeResult.getAuthCode());
        Assert.assertNull(authorizeResult.getReason());
    }

    // Disabled by default since Boleto isn't enabled automatically on the sandbox
    @Test(groups = "integration", enabled = false)
    public void testBoleto() throws Exception {
        final PaymentInfo boleto = new PaymentInfo();
        boleto.setCountry("BR");
        boleto.setSelectedBrand("boletobancario_santander");

        final PaymentData<PaymentInfo> paymentData = new PaymentData<PaymentInfo>(new BigDecimal("1"), Currency.BRL, UUID.randomUUID().toString(), boleto);
        final UserData userData = new UserData();
        userData.setFirstName("José");
        userData.setLastName("Silva");
        final SplitSettlementData splitSettlementData = null;
        final Map<String, String> additionalData = null;

        final PurchaseResult authorizeResult = adyenPaymentServiceProviderPort.authorise(merchantAccount, paymentData, userData, splitSettlementData, additionalData);
        Assert.assertNotNull(authorizeResult.getPspReference());
        Assert.assertEquals(authorizeResult.getResultCode(), "Received");
        Assert.assertNull(authorizeResult.getAuthCode());
        Assert.assertNull(authorizeResult.getReason());
        Assert.assertNotNull(authorizeResult.getAdditionalData().get("boletobancario.barCodeReference"));
        Assert.assertNotNull(authorizeResult.getAdditionalData().get("boletobancario.data"));
        Assert.assertNotNull(authorizeResult.getAdditionalData().get("boletobancario.dueDate"));
        Assert.assertNotNull(authorizeResult.getAdditionalData().get("boletobancario.url"));
        Assert.assertNotNull(authorizeResult.getAdditionalData().get("boletobancario.expirationDate"));
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
