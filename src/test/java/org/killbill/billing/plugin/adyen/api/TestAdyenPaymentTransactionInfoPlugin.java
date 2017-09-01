/*
 * Copyright 2016 Groupon, Inc
 * Copyright 2016 The Billing Project, LLC
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

package org.killbill.billing.plugin.adyen.api;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.UUID;

import org.jooq.types.UInteger;
import org.killbill.billing.catalog.api.Currency;
import org.killbill.billing.payment.api.TransactionType;
import org.killbill.billing.payment.plugin.api.PaymentTransactionInfoPlugin;
import org.killbill.billing.plugin.adyen.dao.gen.tables.records.AdyenResponsesRecord;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TestAdyenPaymentTransactionInfoPlugin {

    @Test(groups = "fast")
    public void testGatewayErrorCodeTruncation() throws Exception {
        final String errorCode = "configuration 905 Payment details are not supported";
        final AdyenResponsesRecord responsesRecord = new AdyenResponsesRecord(UInteger.valueOf(1),
                                                                              UUID.randomUUID().toString(),
                                                                              UUID.randomUUID().toString(),
                                                                              UUID.randomUUID().toString(),
                                                                              TransactionType.AUTHORIZE.toString(),
                                                                              BigDecimal.TEN,
                                                                              Currency.USD.toString(),
                                                                              null,
                                                                              UUID.randomUUID().toString(),
                                                                              UUID.randomUUID().toString(),
                                                                              errorCode,
                                                                              UUID.randomUUID().toString(),
                                                                              UUID.randomUUID().toString(),
                                                                              UUID.randomUUID().toString(),
                                                                              UUID.randomUUID().toString(),
                                                                              UUID.randomUUID().toString(),
                                                                              BigDecimal.ZERO,
                                                                              Currency.USD.toString(),
                                                                              UUID.randomUUID().toString(),
                                                                              UUID.randomUUID().toString(),
                                                                              UUID.randomUUID().toString(),
                                                                              UUID.randomUUID().toString(),
                                                                              null,
                                                                              new Timestamp(1242L),
                                                                              UUID.randomUUID().toString());
        final PaymentTransactionInfoPlugin paymentTransactionInfoPlugin = new AdyenPaymentTransactionInfoPlugin(responsesRecord);
        Assert.assertNull(paymentTransactionInfoPlugin.getGatewayErrorCode());
    }

    @Test(groups = "fast")
    public void testGatewayErrorCodeRawAcquirerReason() throws Exception {
        final AdyenResponsesRecord responsesRecord = new AdyenResponsesRecord(UInteger.valueOf(1),
                                                                              UUID.randomUUID().toString(),
                                                                              UUID.randomUUID().toString(),
                                                                              UUID.randomUUID().toString(),
                                                                              TransactionType.AUTHORIZE.toString(),
                                                                              BigDecimal.TEN,
                                                                              Currency.USD.toString(),
                                                                              null,
                                                                              UUID.randomUUID().toString(),
                                                                              UUID.randomUUID().toString(),
                                                                              UUID.randomUUID().toString(),
                                                                              UUID.randomUUID().toString(),
                                                                              UUID.randomUUID().toString(),
                                                                              UUID.randomUUID().toString(),
                                                                              UUID.randomUUID().toString(),
                                                                              UUID.randomUUID().toString(),
                                                                              BigDecimal.ZERO,
                                                                              Currency.USD.toString(),
                                                                              UUID.randomUUID().toString(),
                                                                              UUID.randomUUID().toString(),
                                                                              UUID.randomUUID().toString(),
                                                                              UUID.randomUUID().toString(),
                                                                              "{\"refusalReasonRaw\":\"05 : Do not honor\"}",
                                                                              new Timestamp(1242L),
                                                                              UUID.randomUUID().toString());
        final PaymentTransactionInfoPlugin paymentTransactionInfoPlugin = new AdyenPaymentTransactionInfoPlugin(responsesRecord);
        Assert.assertEquals(paymentTransactionInfoPlugin.getGatewayError(), "Do not honor");
        Assert.assertEquals(paymentTransactionInfoPlugin.getGatewayErrorCode(), "05");
    }

    @Test(groups = "fast")
    public void testNullGatewayErrorCode() throws Exception {
        final AdyenResponsesRecord responsesRecord = new AdyenResponsesRecord(UInteger.valueOf(1),
                                                                              UUID.randomUUID().toString(),
                                                                              UUID.randomUUID().toString(),
                                                                              UUID.randomUUID().toString(),
                                                                              TransactionType.AUTHORIZE.toString(),
                                                                              BigDecimal.TEN,
                                                                              Currency.USD.toString(),
                                                                              null,
                                                                              UUID.randomUUID().toString(),
                                                                              UUID.randomUUID().toString(),
                                                                              UUID.randomUUID().toString(),
                                                                              "Not enough balance",
                                                                              UUID.randomUUID().toString(),
                                                                              UUID.randomUUID().toString(),
                                                                              UUID.randomUUID().toString(),
                                                                              UUID.randomUUID().toString(),
                                                                              BigDecimal.ZERO,
                                                                              Currency.USD.toString(),
                                                                              UUID.randomUUID().toString(),
                                                                              UUID.randomUUID().toString(),
                                                                              UUID.randomUUID().toString(),
                                                                              UUID.randomUUID().toString(),
                                                                              "{\"refusalReasonRaw\":\"ill-formatted raw refusal reason\"}",
                                                                              new Timestamp(1242L),
                                                                              UUID.randomUUID().toString());
        final PaymentTransactionInfoPlugin paymentTransactionInfoPlugin = new AdyenPaymentTransactionInfoPlugin(responsesRecord);
        Assert.assertEquals(paymentTransactionInfoPlugin.getGatewayError(), "Not enough balance");
        Assert.assertNull(paymentTransactionInfoPlugin.getGatewayErrorCode());
    }
}
