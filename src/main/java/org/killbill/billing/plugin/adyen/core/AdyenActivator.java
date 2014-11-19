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

import org.jooq.SQLDialect;
import org.killbill.billing.osgi.api.OSGIPluginProperties;
import org.killbill.billing.payment.plugin.api.PaymentPluginApi;
import org.killbill.billing.plugin.adyen.api.AdyenPaymentPluginApi;
import org.killbill.billing.plugin.adyen.client.AdyenConfigProperties;
import org.killbill.billing.plugin.adyen.client.AdyenPaymentPortRegistry;
import org.killbill.billing.plugin.adyen.client.AdyenRequestSender;
import org.killbill.billing.plugin.adyen.client.PaymentPortRegistry;
import org.killbill.billing.plugin.adyen.dao.AdyenDao;
import org.killbill.clock.Clock;
import org.killbill.clock.DefaultClock;
import org.killbill.killbill.osgi.libs.killbill.KillbillActivatorBase;
import org.killbill.killbill.osgi.libs.killbill.OSGIKillbillEventDispatcher.OSGIKillbillEventHandler;
import org.killbill.killbill.osgi.libs.killbill.OSGIKillbillLogService;
import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;
import org.skife.config.ConfigurationObjectFactory;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Properties;

public class AdyenActivator extends KillbillActivatorBase {

    public static final String PLUGIN_NAME = "killbill-adyen";

    @Override
    public void start(final BundleContext context) throws Exception {
        super.start(context);

        // For safety, we want to enter with context class loader set to the Felix bundle and not the one from the web app.
        // This is not stricly required in that initialization but if we were to do fancy things-- like initializing apache cxf
        // that would break without it.
        Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());

        logService.log(LogService.LOG_INFO, "Entering AdyenActivator start");


        // Register a servlet (optional)
        final AdyenServlet adyenServlet = new AdyenServlet(logService);
        registerServlet(context, adyenServlet);

        final AdyenDao dao = new AdyenDao(dataSource.getDataSource(), SQLDialect.MYSQL);

        final Clock clock = new DefaultClock();

        final AdyenConfig config = readAdyenConfigFromProperties(configProperties.getProperties());

        final AdyenConfigProperties configProperties = new AdyenConfigProperties(config);
        final AdyenRequestSender adyenClient = initializeAdyenClient(configProperties, logService);
        final AdyenPaymentPluginApi pluginApi = new AdyenPaymentPluginApi(killbillAPI, dao, clock, logService, adyenClient, configProperties.getMerchantAccount("DE"));
        registerPaymentPluginApi(context, pluginApi);
        logService.log(LogService.LOG_INFO, "Exiting AdyenActivator start");
    }

    @Override
    public void stop(final BundleContext context) throws Exception {
        Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
        logService.log(LogService.LOG_INFO, "AdyenActivator stopping");
        super.stop(context);
        // Do additional work on shutdown (optional)
    }

    @Override
    public OSGIKillbillEventHandler getOSGIKillbillEventHandler() {
        return null;
    }


    private AdyenConfig readAdyenConfigFromProperties(final Properties properties) {
        return new ConfigurationObjectFactory(properties).build(AdyenConfig.class);
    }

    private AdyenRequestSender initializeAdyenClient(final AdyenConfigProperties configProperties, OSGIKillbillLogService logService) throws Exception {
        final PaymentPortRegistry portRegistry = new AdyenPaymentPortRegistry(configProperties, logService);
        final AdyenRequestSender adyenClient = new AdyenRequestSender(portRegistry);
        return adyenClient;
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
