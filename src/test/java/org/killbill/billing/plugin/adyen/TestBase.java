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

package org.killbill.billing.plugin.adyen;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import org.junit.After;
import org.junit.Before;
import org.killbill.billing.ObjectType;
import org.killbill.billing.account.api.Account;
import org.killbill.billing.account.api.AccountUserApi;
import org.killbill.billing.catalog.api.Currency;
import org.killbill.billing.invoice.api.Invoice;
import org.killbill.billing.invoice.api.InvoiceUserApi;
import org.killbill.billing.osgi.libs.killbill.OSGIConfigPropertiesService;
import org.killbill.billing.osgi.libs.killbill.OSGIKillbillAPI;
import org.killbill.billing.payment.api.PaymentApi;
import org.killbill.billing.payment.api.PaymentMethod;
import org.killbill.billing.plugin.TestUtils;
import org.killbill.billing.plugin.adyen.api.AdyenPaymentPluginApi;
import org.killbill.billing.plugin.adyen.core.AdyenActivator;
import org.killbill.billing.plugin.adyen.core.AdyenConfigProperties;
import org.killbill.billing.plugin.adyen.core.AdyenConfigurationHandler;
import org.killbill.billing.plugin.adyen.dao.AdyenDao;
import org.killbill.billing.util.api.CustomFieldUserApi;
import org.killbill.billing.util.callcontext.CallContext;
import org.killbill.billing.util.callcontext.TenantContext;
import org.killbill.billing.util.customfield.CustomField;
import org.killbill.clock.ClockMock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestBase {

  protected static final String PROPERTIES_FILE_NAME = "adyen.properties";
  public static final Currency DEFAULT_CURRENCY = Currency.USD;
  public static final String DEFAULT_COUNTRY = "US";

  protected ClockMock clock;
  protected CallContext context;
  protected Account account;
  protected AdyenPaymentPluginApi adyenPaymentPluginApi;
  protected OSGIKillbillAPI killbillApi;
  protected CustomFieldUserApi customFieldUserApi;
  protected InvoiceUserApi invoiceUserApi;
  protected AccountUserApi accountUserApi;
  protected PaymentApi paymentApi;
  protected PaymentMethod paymentMethod;
  protected AdyenConfigurationHandler adyenConfigPropertiesConfigurationHandler;
  protected AdyenDao dao;

  private static final Logger logger = LoggerFactory.getLogger(TestBase.class);

  @Before
  public void setUp() throws Exception {
    setUpBeforeSuite();

    logger.info("[setUp] initialization");
    EmbeddedDbHelper.instance().resetDB();
    dao = EmbeddedDbHelper.instance().getAdyenDao();

    clock = new ClockMock();
    context = Mockito.mock(CallContext.class);
    Mockito.when(context.getTenantId()).thenReturn(UUID.randomUUID());

    account = TestUtils.buildAccount(DEFAULT_CURRENCY, DEFAULT_COUNTRY);
    Mockito.when(account.getEmail()).thenReturn(UUID.randomUUID().toString() + "@example.com");
    killbillApi = TestUtils.buildOSGIKillbillAPI(account);
    customFieldUserApi = Mockito.mock(CustomFieldUserApi.class);
    Mockito.when(killbillApi.getCustomFieldUserApi()).thenReturn(customFieldUserApi);
    invoiceUserApi = Mockito.mock(InvoiceUserApi.class);
    Mockito.when(killbillApi.getInvoiceUserApi()).thenReturn(invoiceUserApi);
    accountUserApi = Mockito.mock(AccountUserApi.class);
    Mockito.when(killbillApi.getAccountUserApi()).thenReturn(accountUserApi);
    paymentApi = Mockito.mock(PaymentApi.class);
    Mockito.when(killbillApi.getPaymentApi()).thenReturn(paymentApi);
    paymentMethod = Mockito.mock(PaymentMethod.class);
    Mockito.when(
            paymentApi.getPaymentMethodById(
                Mockito.any(UUID.class),
                Mockito.any(Boolean.class),
                Mockito.any(Boolean.class),
                Mockito.anyList(),
                Mockito.any(TenantContext.class)))
        .thenReturn(paymentMethod);

    Invoice mockInvoice = TestUtils.buildInvoice(account);
    List<Invoice> mockInvoices = new ArrayList<>();

    mockInvoices.add(mockInvoice);

    Mockito.when(
            invoiceUserApi.getInvoiceByPayment(
                Mockito.any(UUID.class), Mockito.any(TenantContext.class)))
        .thenReturn(mockInvoice);
    Mockito.when(
            invoiceUserApi.getUnpaidInvoicesByAccountId(
                Mockito.any(UUID.class),
                Mockito.any(),
                Mockito.any(),
                Mockito.any(CallContext.class)))
        .thenReturn(mockInvoices);
    Mockito.when(
            customFieldUserApi.getCustomFieldsForObject(
                Mockito.any(UUID.class), Mockito.any(), Mockito.any(TenantContext.class)))
        .thenReturn(null);
    TestUtils.buildPaymentMethod(
        account.getId(), account.getPaymentMethodId(), AdyenActivator.PLUGIN_NAME, killbillApi);

    adyenConfigPropertiesConfigurationHandler =
        new AdyenConfigurationHandler(null, AdyenActivator.PLUGIN_NAME, killbillApi);

    final OSGIConfigPropertiesService configPropertiesService =
        Mockito.mock(OSGIConfigPropertiesService.class);
    adyenPaymentPluginApi =
        new AdyenPaymentPluginApi(
            adyenConfigPropertiesConfigurationHandler,
            killbillApi,
            configPropertiesService,
            clock,
            dao);

    TestUtils.updateOSGIKillbillAPI(killbillApi, adyenPaymentPluginApi);

    Mockito.doAnswer(
            new Answer<Object>() {
              @Override
              public Object answer(final InvocationOnMock invocation) throws Throwable {
                // A bit simplistic but good enough for now?
                Mockito.when(
                        customFieldUserApi.getCustomFieldsForAccountType(
                            Mockito.eq(account.getId()),
                            Mockito.eq(ObjectType.ACCOUNT),
                            Mockito.any(TenantContext.class)))
                    .thenReturn((List<CustomField>) invocation.getArguments()[0]);
                return null;
              }
            })
        .when(customFieldUserApi)
        .addCustomFields(Mockito.anyList(), Mockito.any(CallContext.class));
    setUpIntegration(PROPERTIES_FILE_NAME);
  }

  protected void setUpIntegration(String fileName) throws IOException {
    logger.info("[setUpIntegration] initialization");
    final Properties properties = TestUtils.loadProperties(fileName);
    final AdyenConfigProperties AdyenConfigProperties = new AdyenConfigProperties(properties, "");
    adyenConfigPropertiesConfigurationHandler.setDefaultConfigurable(AdyenConfigProperties);
  }

  private void setUpBeforeSuite() throws IOException, SQLException {
    logger.info("[setUpBeforeSuite] initialization");

    EmbeddedDbHelper.instance().startDb();
  }

  @After
  public void tearDownAfterSuite() throws IOException {
    EmbeddedDbHelper.instance().stopDB();
  }
}
