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

package org.killbill.billing.plugin.adyen.core;

import java.util.Dictionary;
import java.util.Hashtable;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;

import org.killbill.billing.osgi.api.OSGIPluginProperties;
import org.killbill.billing.payment.plugin.api.PaymentPluginApi;
import org.killbill.billing.plugin.adyen.api.AdyenPaymentPluginApi;
import org.killbill.billing.plugin.adyen.client.AdyenConfigProperties;
import org.killbill.billing.plugin.adyen.client.AdyenPaymentPortRegistry;
import org.killbill.billing.plugin.adyen.client.PaymentPortRegistry;
import org.killbill.billing.plugin.adyen.client.jaxws.HttpHeaderInterceptor;
import org.killbill.billing.plugin.adyen.client.jaxws.LoggingInInterceptor;
import org.killbill.billing.plugin.adyen.client.jaxws.LoggingOutInterceptor;
import org.killbill.billing.plugin.adyen.client.payment.builder.AdyenRequestFactory;
import org.killbill.billing.plugin.adyen.client.payment.converter.PaymentInfoConverterManagement;
import org.killbill.billing.plugin.adyen.client.payment.converter.impl.PaymentInfoConverterService;
import org.killbill.billing.plugin.adyen.client.payment.service.AdyenPaymentRequestSender;
import org.killbill.billing.plugin.adyen.client.payment.service.AdyenPaymentServiceProviderHostedPaymentPagePort;
import org.killbill.billing.plugin.adyen.client.payment.service.AdyenPaymentServiceProviderPort;
import org.killbill.billing.plugin.adyen.client.payment.service.Signer;
import org.killbill.billing.plugin.adyen.dao.AdyenDao;
import org.killbill.clock.Clock;
import org.killbill.clock.DefaultClock;
import org.killbill.killbill.osgi.libs.killbill.KillbillActivatorBase;
import org.killbill.killbill.osgi.libs.killbill.OSGIKillbillEventDispatcher.OSGIKillbillEventHandler;
import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;

public class AdyenActivator extends KillbillActivatorBase {

    public static final String PLUGIN_NAME = "killbill-adyen";

    @Override
    public void start(final BundleContext context) throws Exception {
        super.start(context);

        // For safety, we want to enter with context class loader set to the Felix bundle and not the one from the web app.
        // This is not strictly required in that initialization but if we were to do fancy things (like initializing apache cxf),
        // that would break without it.
        Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());

        logService.log(LogService.LOG_INFO, "Entering AdyenActivator start");

        // Register the servlet
        final AdyenServlet adyenServlet = new AdyenServlet();
        registerServlet(context, adyenServlet);

        // Register the payment plugin
        final Clock clock = new DefaultClock();
        final AdyenDao dao = new AdyenDao(dataSource.getDataSource());
        final AdyenConfigProperties adyenConfigProperties = new AdyenConfigProperties(configProperties.getProperties());
        final AdyenPaymentServiceProviderPort adyenClient = initializeAdyenClient(adyenConfigProperties);
        final AdyenPaymentServiceProviderHostedPaymentPagePort adyenHppClient = initializeHppAdyenClient(adyenConfigProperties);
        final AdyenPaymentPluginApi pluginApi = new AdyenPaymentPluginApi(adyenConfigProperties, adyenClient, adyenHppClient, killbillAPI, configProperties, logService, clock, dao);
        registerPaymentPluginApi(context, pluginApi);

        logService.log(LogService.LOG_INFO, "Exiting AdyenActivator start");
    }

    @Override
    public void stop(final BundleContext context) throws Exception {
        Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
        logService.log(LogService.LOG_INFO, "AdyenActivator stopping");
        super.stop(context);
    }

    @Override
    public OSGIKillbillEventHandler getOSGIKillbillEventHandler() {
        return null;
    }

    private AdyenPaymentServiceProviderPort initializeAdyenClient(final AdyenConfigProperties adyenConfigProperties) throws Exception {
        final PaymentInfoConverterManagement paymentInfoConverterManagement = new PaymentInfoConverterService();

        final Signer signer = new Signer(adyenConfigProperties);
        final AdyenRequestFactory adyenRequestFactory = new AdyenRequestFactory(paymentInfoConverterManagement, adyenConfigProperties, signer);

        final LoggingInInterceptor loggingInInterceptor = new LoggingInInterceptor();
        final LoggingOutInterceptor loggingOutInterceptor = new LoggingOutInterceptor();
        final HttpHeaderInterceptor httpHeaderInterceptor = new HttpHeaderInterceptor();
        final PaymentPortRegistry adyenPaymentPortRegistry = new AdyenPaymentPortRegistry(adyenConfigProperties, loggingInInterceptor, loggingOutInterceptor, httpHeaderInterceptor);
        final AdyenPaymentRequestSender adyenPaymentRequestSender = new AdyenPaymentRequestSender(adyenPaymentPortRegistry);

        return new AdyenPaymentServiceProviderPort(paymentInfoConverterManagement, adyenRequestFactory, adyenPaymentRequestSender);
    }

    private AdyenPaymentServiceProviderHostedPaymentPagePort initializeHppAdyenClient(final AdyenConfigProperties adyenConfigProperties) {
        final PaymentInfoConverterManagement paymentInfoConverterManagement = new PaymentInfoConverterService();

        final Signer signer = new Signer(adyenConfigProperties);
        final AdyenRequestFactory adyenRequestFactory = new AdyenRequestFactory(paymentInfoConverterManagement, adyenConfigProperties, signer);

        return new AdyenPaymentServiceProviderHostedPaymentPagePort(adyenConfigProperties, adyenRequestFactory);
    }

    private void registerServlet(final BundleContext context, final HttpServlet servlet) {
        final Hashtable<String, String> props = new Hashtable<String, String>();
        props.put(OSGIPluginProperties.PLUGIN_NAME_PROP, PLUGIN_NAME);
        registrar.registerService(context, Servlet.class, servlet, props);
    }

    private void registerPaymentPluginApi(final BundleContext context, final PaymentPluginApi api) {
        final Dictionary props = new Hashtable();
        props.put(OSGIPluginProperties.PLUGIN_NAME_PROP, PLUGIN_NAME);
        registrar.registerService(context, PaymentPluginApi.class, api, props);
    }
}
