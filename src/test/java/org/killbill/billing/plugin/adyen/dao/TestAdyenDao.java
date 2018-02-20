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

package org.killbill.billing.plugin.adyen.dao;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.xml.datatype.XMLGregorianCalendar;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.killbill.adyen.common.Amount;
import org.killbill.adyen.notification.ArrayOfString;
import org.killbill.adyen.notification.NotificationRequestItem;
import org.killbill.billing.catalog.api.Currency;
import org.killbill.billing.payment.api.TransactionType;
import org.killbill.billing.plugin.adyen.TestWithEmbeddedDBBase;
import org.killbill.billing.plugin.adyen.api.AdyenPaymentPluginApi;
import org.killbill.billing.plugin.adyen.client.model.NotificationItem;
import org.killbill.billing.plugin.adyen.client.model.PaymentServiceProviderResult;
import org.killbill.billing.plugin.adyen.client.model.PurchaseResult;
import org.killbill.billing.plugin.adyen.dao.gen.tables.records.AdyenNotificationsRecord;
import org.killbill.billing.plugin.adyen.dao.gen.tables.records.AdyenResponsesRecord;
import org.killbill.clock.DefaultClock;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;

public class TestAdyenDao extends TestWithEmbeddedDBBase {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Test(groups = "slow")
    public void testInsertPaymentResult() throws SQLException, IOException {
        final PurchaseResult purchaseResult = new PurchaseResult(PaymentServiceProviderResult.AUTHORISED,
                                                                 UUID.randomUUID().toString(),
                                                                 UUID.randomUUID().toString(),
                                                                 UUID.randomUUID().toString(),
                                                                 UUID.randomUUID().toString(),
                                                                 UUID.randomUUID().toString(),
                                                                 UUID.randomUUID().toString(),
                                                                 ImmutableMap.<String, String>of(UUID.randomUUID().toString(), UUID.randomUUID().toString(),
                                                                                                 AdyenPaymentPluginApi.PROPERTY_DCC_AMOUNT_VALUE, "10",
                                                                                                 AdyenPaymentPluginApi.PROPERTY_DCC_AMOUNT_CURRENCY, "EUR"),
                                                                 ImmutableMap.<String, String>of("cvcResult", "3 Not checked",
                                                                                                 "refusalReasonRaw", "AUTHORISED"));
        final UUID kbAccountId = UUID.randomUUID();
        final UUID kbPaymentId = UUID.randomUUID();
        final UUID kbPaymentTransactionId = UUID.randomUUID();
        final TransactionType transactionType = TransactionType.AUTHORIZE;
        final BigDecimal amount = BigDecimal.TEN;
        final Currency currency = Currency.EUR;
        final DateTime dateTime = DefaultClock.truncateMs(new DateTime(DateTimeZone.UTC));
        final UUID kbTenantId = UUID.randomUUID();
        final Map<String, String> expectedAdditionalData = ImmutableMap.<String, String>builder()
                                                                       .putAll(purchaseResult.getAdditionalData())
                                                                       .putAll(purchaseResult.getFormParameter())
                                                                       .build();
        dao.addResponse(kbAccountId, kbPaymentId, kbPaymentTransactionId, transactionType, amount, currency, purchaseResult, dateTime, kbTenantId);

        final List<AdyenResponsesRecord> result = dao.getResponses(kbPaymentId, kbTenantId);
        Assert.assertEquals(result.size(), 1);
        final AdyenResponsesRecord record = result.get(0);
        Assert.assertNotNull(record.getRecordId());
        Assert.assertEquals(record.getKbAccountId(), kbAccountId.toString());
        Assert.assertEquals(record.getKbPaymentId(), kbPaymentId.toString());
        Assert.assertEquals(record.getKbPaymentTransactionId(), kbPaymentTransactionId.toString());
        Assert.assertEquals(record.getTransactionType(), transactionType.toString());
        Assert.assertEquals(record.getAmount().compareTo(amount), 0);
        Assert.assertEquals(record.getCurrency(), currency.toString());
        Assert.assertEquals(new DateTime(record.getCreatedDate(), DateTimeZone.UTC).compareTo(dateTime), 0);
        Assert.assertEquals(record.getKbTenantId(), kbTenantId.toString());
        Assert.assertEquals(record.getDccAmount().compareTo(BigDecimal.TEN), 0);
        Assert.assertEquals(record.getDccCurrency(), "EUR");
        Assert.assertEquals(objectMapper.readValue(record.getAdditionalData(), Map.class), expectedAdditionalData);
        Assert.assertEquals(record.getPspResult(), PaymentServiceProviderResult.AUTHORISED.toString());
    }

    @Test(groups = "slow")
    public void testInsertNotification() throws SQLException, IOException {
        final NotificationRequestItem notificationRequestItem = new NotificationRequestItem();
        final Amount amount = new Amount();
        amount.setValue(1200L);
        amount.setCurrency("EUR");
        notificationRequestItem.setAmount(amount);
        notificationRequestItem.setEventCode(UUID.randomUUID().toString());
        final XMLGregorianCalendar calendar = Mockito.mock(XMLGregorianCalendar.class);
        Mockito.when(calendar.toGregorianCalendar()).thenReturn(new GregorianCalendar());
        notificationRequestItem.setEventDate(calendar);
        notificationRequestItem.setMerchantAccountCode(UUID.randomUUID().toString());
        notificationRequestItem.setMerchantReference(UUID.randomUUID().toString());
        final ArrayOfString operations = new ArrayOfString();
        operations.getString().add(UUID.randomUUID().toString());
        notificationRequestItem.setOperations(operations);
        notificationRequestItem.setOriginalReference(UUID.randomUUID().toString());
        notificationRequestItem.setPaymentMethod(UUID.randomUUID().toString());
        notificationRequestItem.setPspReference(UUID.randomUUID().toString());
        notificationRequestItem.setReason(UUID.randomUUID().toString());
        notificationRequestItem.setSuccess(true);
        final NotificationItem notificationItem = new NotificationItem(notificationRequestItem);

        final UUID kbAccountId = UUID.randomUUID();
        final UUID kbPaymentId = UUID.randomUUID();
        final UUID kbPaymentTransactionId = UUID.randomUUID();
        final TransactionType transactionType = TransactionType.AUTHORIZE;
        final DateTime dateTime = DefaultClock.truncateMs(new DateTime(DateTimeZone.UTC));
        final UUID kbTenantId = UUID.randomUUID();
        dao.addNotification(kbAccountId, kbPaymentId, kbPaymentTransactionId, transactionType, notificationItem, dateTime, kbTenantId);

        final AdyenNotificationsRecord record = dao.getNotification(notificationRequestItem.getPspReference());
        Assert.assertNotNull(record.getRecordId());
        Assert.assertEquals(record.getKbAccountId(), kbAccountId.toString());
        Assert.assertEquals(record.getKbPaymentId(), kbPaymentId.toString());
        Assert.assertEquals(record.getKbPaymentTransactionId(), kbPaymentTransactionId.toString());
        Assert.assertEquals(record.getTransactionType(), transactionType.toString());
        Assert.assertEquals(record.getAmount().compareTo(new BigDecimal("12")), 0);
        Assert.assertEquals(record.getCurrency(), "EUR");
        Assert.assertEquals(record.getEventCode(), notificationItem.getEventCode());
        Assert.assertEquals(record.getMerchantAccountCode(), notificationItem.getMerchantAccountCode());
        Assert.assertEquals(record.getMerchantReference(), notificationItem.getMerchantReference());
        Assert.assertEquals(record.getOperations(), operations.getString().get(0));
        Assert.assertEquals(record.getOriginalReference(), notificationItem.getOriginalReference());
        Assert.assertEquals(record.getPaymentMethod(), notificationItem.getPaymentMethod());
        Assert.assertEquals(record.getPspReference(), notificationItem.getPspReference());
        Assert.assertEquals(record.getReason(), notificationItem.getReason());
        Assert.assertTrue(record.getSuccess() == '1');
        Assert.assertEquals(new DateTime(record.getCreatedDate(), DateTimeZone.UTC).compareTo(dateTime), 0);
        Assert.assertEquals(record.getKbTenantId(), kbTenantId.toString());
    }
}
