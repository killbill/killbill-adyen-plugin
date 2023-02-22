/*
 * Copyright 2020-2023 Equinix, Inc
 * Copyright 2014-2023 The Billing Project, LLC
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

package org.killbill.billing.plugin.adyen;

import com.google.common.collect.ImmutableList;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.killbill.billing.osgi.api.Healthcheck;
import org.killbill.billing.payment.api.Payment;
import org.killbill.billing.payment.api.PaymentApiException;
import org.killbill.billing.payment.api.PaymentMethodPlugin;
import org.killbill.billing.payment.api.PaymentTransaction;
import org.killbill.billing.payment.api.TransactionType;
import org.killbill.billing.payment.plugin.api.PaymentMethodInfoPlugin;
import org.killbill.billing.payment.plugin.api.PaymentPluginApiException;
import org.killbill.billing.payment.plugin.api.PaymentPluginStatus;
import org.killbill.billing.payment.plugin.api.PaymentTransactionInfoPlugin;
import org.killbill.billing.plugin.TestUtils;
import org.killbill.billing.plugin.adyen.api.AdyenPaymentMethodInfoPlugin;
import org.killbill.billing.plugin.adyen.api.AdyenPaymentMethodPlugin;
import org.killbill.billing.plugin.adyen.core.AdyenHealthcheck;
import org.killbill.billing.plugin.adyen.dao.gen.tables.records.AdyenPaymentMethodsRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TestAdyenPaymentPluginApi extends TestBase {

  private static final Logger logger = LoggerFactory.getLogger(TestAdyenPaymentPluginApi.class);
  private String defaultToken = "F8C833979563DDE0008FC110580B69F5";
  private String ccOneTime = "CC_ONE_TIME";
  private String testing = "Testing";
  private String ccRecurring = "CC_RECURRING";

  @Test(groups = "integration")
  public void testCreatePaymentMethod() throws PaymentPluginApiException {
    logger.info("testCreatePaymentMethod");
    final UUID kbAccountId = account.getId();
    Assert.assertEquals(syncPaymentMethods(kbAccountId).size(), 0);
    final UUID kbPaymentMethodId = UUID.randomUUID();
    AdyenPaymentMethodPlugin AdyenPaymentMethodPlugin =
        new AdyenPaymentMethodPlugin(
            kbPaymentMethodId, kbPaymentMethodId.toString(), true, ImmutableList.of());

    adyenPaymentPluginApi.addPaymentMethod(
        kbAccountId,
        kbPaymentMethodId,
        AdyenPaymentMethodPlugin,
        true,
        ImmutableList.of(),
        context);

    Assert.assertEquals(syncPaymentMethods(kbAccountId).size(), 1);
  }

  @Test(groups = "integration")
  public void testDeletePaymentMethod() throws PaymentPluginApiException {
    final UUID kbAccountId = account.getId();

    Assert.assertEquals(syncPaymentMethods(kbAccountId).size(), 0);
    final UUID kbPaymentMethodId = UUID.randomUUID();
    AdyenPaymentMethodPlugin AdyenPaymentMethodPlugin =
        new AdyenPaymentMethodPlugin(
            kbPaymentMethodId, kbPaymentMethodId.toString(), true, ImmutableList.of());

    adyenPaymentPluginApi.addPaymentMethod(
        kbAccountId,
        kbPaymentMethodId,
        AdyenPaymentMethodPlugin,
        true,
        ImmutableList.of(),
        context);

    Assert.assertEquals(syncPaymentMethods(kbAccountId).size(), 1);
    adyenPaymentPluginApi.deletePaymentMethod(kbAccountId, kbPaymentMethodId, null, context);
    Assert.assertEquals(syncPaymentMethods(kbAccountId).size(), 0);
  }

  @Test(groups = "integration")
  public void testSuccessfulAuthVoid() throws PaymentPluginApiException, PaymentApiException {
    final UUID kbAccountId = account.getId();

    Assert.assertEquals(syncPaymentMethods(kbAccountId).size(), 0);
    final UUID kbPaymentMethodId = UUID.randomUUID();
    AdyenPaymentMethodPlugin AdyenPaymentMethodPlugin =
        new AdyenPaymentMethodPlugin(
            kbPaymentMethodId, kbPaymentMethodId.toString(), true, ImmutableList.of());

    adyenPaymentPluginApi.addPaymentMethod(
        kbAccountId,
        kbPaymentMethodId,
        AdyenPaymentMethodPlugin,
        true,
        ImmutableList.of(),
        context);

    final Payment payment =
        TestUtils.buildPayment(kbAccountId, kbPaymentMethodId, account.getCurrency(), killbillApi);
    final PaymentTransaction authorizationTransaction =
        TestUtils.buildPaymentTransaction(
            payment, TransactionType.AUTHORIZE, BigDecimal.TEN, payment.getCurrency());
    final PaymentTransaction voidTransaction =
        TestUtils.buildPaymentTransaction(
            payment, TransactionType.VOID, BigDecimal.TEN, payment.getCurrency());

    final PaymentTransactionInfoPlugin authorizationInfoPlugin =
        adyenPaymentPluginApi.authorizePayment(
            kbAccountId,
            payment.getId(),
            authorizationTransaction.getId(),
            kbPaymentMethodId,
            authorizationTransaction.getAmount(),
            authorizationTransaction.getCurrency(),
            ImmutableList.of(),
            context);
    TestUtils.updatePaymentTransaction(authorizationTransaction, authorizationInfoPlugin);
    Assert.assertEquals(authorizationInfoPlugin.getStatus(), PaymentPluginStatus.CANCELED);

    final PaymentTransactionInfoPlugin voidInfoPlugin =
        adyenPaymentPluginApi.voidPayment(
            kbAccountId,
            payment.getId(),
            voidTransaction.getId(),
            kbPaymentMethodId,
            ImmutableList.of(),
            context);
    Assert.assertEquals(voidInfoPlugin.getStatus(), PaymentPluginStatus.CANCELED);
  }

  @Test(groups = "integration")
  public void testSuccessfulCapture() throws PaymentPluginApiException, PaymentApiException {
    final UUID kbAccountId = account.getId();

    Assert.assertEquals(syncPaymentMethods(kbAccountId).size(), 0);
    final UUID kbPaymentMethodId = UUID.randomUUID();
    AdyenPaymentMethodPlugin AdyenPaymentMethodPlugin =
        new AdyenPaymentMethodPlugin(
            kbPaymentMethodId, kbPaymentMethodId.toString(), true, ImmutableList.of());

    adyenPaymentPluginApi.addPaymentMethod(
        kbAccountId,
        kbPaymentMethodId,
        AdyenPaymentMethodPlugin,
        true,
        ImmutableList.of(),
        context);

    final Payment payment =
        TestUtils.buildPayment(kbAccountId, kbPaymentMethodId, account.getCurrency(), killbillApi);
    final PaymentTransaction captureTransaction =
        TestUtils.buildPaymentTransaction(
            payment, TransactionType.AUTHORIZE, BigDecimal.TEN, payment.getCurrency());

    final PaymentTransactionInfoPlugin captureInfoPlugin =
        adyenPaymentPluginApi.capturePayment(
            kbAccountId,
            payment.getId(),
            captureTransaction.getId(),
            kbPaymentMethodId,
            captureTransaction.getAmount(),
            captureTransaction.getCurrency(),
            ImmutableList.of(),
            context);
    TestUtils.updatePaymentTransaction(captureTransaction, captureInfoPlugin);
    Assert.assertEquals(captureInfoPlugin.getStatus(), PaymentPluginStatus.CANCELED);
  }

  @Test(groups = "integration")
  public void testSuccessfulCredit() throws PaymentPluginApiException, PaymentApiException {
    final UUID kbAccountId = account.getId();

    Assert.assertEquals(syncPaymentMethods(kbAccountId).size(), 0);
    final UUID kbPaymentMethodId = UUID.randomUUID();
    AdyenPaymentMethodPlugin AdyenPaymentMethodPlugin =
        new AdyenPaymentMethodPlugin(
            kbPaymentMethodId, kbPaymentMethodId.toString(), true, ImmutableList.of());

    adyenPaymentPluginApi.addPaymentMethod(
        kbAccountId,
        kbPaymentMethodId,
        AdyenPaymentMethodPlugin,
        true,
        ImmutableList.of(),
        context);

    final Payment payment =
        TestUtils.buildPayment(kbAccountId, kbPaymentMethodId, account.getCurrency(), killbillApi);
    final PaymentTransaction creditTransaction =
        TestUtils.buildPaymentTransaction(
            payment, TransactionType.AUTHORIZE, BigDecimal.TEN, payment.getCurrency());

    final PaymentTransactionInfoPlugin creditInfoPlugin =
        adyenPaymentPluginApi.creditPayment(
            kbAccountId,
            payment.getId(),
            creditTransaction.getId(),
            kbPaymentMethodId,
            creditTransaction.getAmount(),
            creditTransaction.getCurrency(),
            ImmutableList.of(),
            context);
    TestUtils.updatePaymentTransaction(creditTransaction, creditInfoPlugin);
    Assert.assertEquals(creditInfoPlugin.getStatus(), PaymentPluginStatus.CANCELED);
  }

  @Test(groups = "integration")
  public void testGetPaymentMethodDetail() throws PaymentPluginApiException {
    final UUID kbAccountId = account.getId();

    Assert.assertEquals(syncPaymentMethods(kbAccountId).size(), 0);
    final UUID kbPaymentMethodId = UUID.randomUUID();
    AdyenPaymentMethodPlugin AdyenPaymentMethodPlugin =
        new AdyenPaymentMethodPlugin(
            kbPaymentMethodId, kbPaymentMethodId.toString(), true, ImmutableList.of());

    adyenPaymentPluginApi.addPaymentMethod(
        kbAccountId,
        kbPaymentMethodId,
        AdyenPaymentMethodPlugin,
        true,
        ImmutableList.of(),
        context);

    Assert.assertEquals(syncPaymentMethods(kbAccountId).size(), 1);

    PaymentMethodPlugin test =
        adyenPaymentPluginApi.getPaymentMethodDetail(kbAccountId, kbPaymentMethodId, null, context);
    Assert.assertEquals(test.getKbPaymentMethodId(), AdyenPaymentMethodPlugin.getKbPaymentMethodId());
  }

  @Test(groups = "integration")
  public void testHealthcheck() {
    final Healthcheck healthcheck = new AdyenHealthcheck();
    Assert.assertTrue(healthcheck.getHealthStatus(null, null).isHealthy());
  }

  @Test(groups = "integration")
  public void testInfoPlugin() throws PaymentPluginApiException, PaymentApiException {
    AdyenPaymentMethodsRecord methodRecord = new AdyenPaymentMethodsRecord();
    methodRecord.setKbAccountId(account.getId().toString());
    methodRecord.setIsDefault((short) 1);

    methodRecord.setKbPaymentMethodId(UUID.randomUUID().toString());
    methodRecord.setKbTenantId(context.getTenantId().toString());

    methodRecord.setCreatedDate(LocalDateTime.now());
    Assert.assertEquals(AdyenPaymentMethodInfoPlugin.build(methodRecord).getAccountId(), account.getId());
  }

  private List<PaymentMethodInfoPlugin> syncPaymentMethods(UUID kbAccountId)
      throws PaymentPluginApiException {
    return adyenPaymentPluginApi.getPaymentMethods(kbAccountId, true, ImmutableList.of(), context);
  }

}
