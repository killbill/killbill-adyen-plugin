/*
 * Copyright 2014-2016 Groupon, Inc
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

import java.util.Hashtable;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;

import org.killbill.billing.osgi.api.Healthcheck;
import org.killbill.billing.osgi.api.OSGIPluginProperties;
import org.killbill.billing.osgi.libs.killbill.KillbillActivatorBase;
import org.killbill.billing.payment.plugin.api.PaymentPluginApi;
import org.killbill.billing.plugin.adyen.api.AdyenPaymentPluginApi;
import org.killbill.billing.plugin.adyen.client.AdyenConfigProperties;
import org.killbill.billing.plugin.adyen.client.payment.service.AdyenPaymentServiceProviderHostedPaymentPagePort;
import org.killbill.billing.plugin.adyen.client.payment.service.AdyenPaymentServiceProviderPort;
import org.killbill.billing.plugin.adyen.client.recurring.AdyenRecurringClient;
import org.killbill.billing.plugin.adyen.core.resources.AdyenHealthcheckServlet;
import org.killbill.billing.plugin.adyen.core.resources.AdyenServlet;
import org.killbill.billing.plugin.adyen.dao.AdyenDao;
import org.killbill.billing.plugin.api.notification.PluginConfigurationEventHandler;
import org.killbill.billing.plugin.core.config.PluginEnvironmentConfig;
import org.killbill.billing.plugin.core.resources.jooby.PluginApp;
import org.killbill.billing.plugin.core.resources.jooby.PluginAppBuilder;
import org.killbill.clock.Clock;
import org.killbill.clock.DefaultClock;
import org.osgi.framework.BundleContext;

public class AdyenActivator extends KillbillActivatorBase {

    public static final String PLUGIN_NAME = "killbill-adyen";

    private AdyenConfigurationHandler adyenConfigurationHandler;
    private AdyenConfigPropertiesConfigurationHandler adyenConfigPropertiesConfigurationHandler;
    private AdyenHostedPaymentPageConfigurationHandler adyenHostedPaymentPageConfigurationHandler;
    private AdyenRecurringConfigurationHandler adyenRecurringConfigurationHandler;

    @Override
    public void start(final BundleContext context) throws Exception {
        super.start(context);

        final Clock clock = new DefaultClock();
        final AdyenDao dao = new AdyenDao(dataSource.getDataSource());

        final String region = PluginEnvironmentConfig.getRegion(configProperties.getProperties());
        adyenConfigurationHandler = new AdyenConfigurationHandler(PLUGIN_NAME, killbillAPI, logService, region);
        adyenConfigPropertiesConfigurationHandler = new AdyenConfigPropertiesConfigurationHandler(PLUGIN_NAME, killbillAPI, logService, region);
        adyenHostedPaymentPageConfigurationHandler = new AdyenHostedPaymentPageConfigurationHandler(PLUGIN_NAME, killbillAPI, logService, region);
        adyenRecurringConfigurationHandler = new AdyenRecurringConfigurationHandler(PLUGIN_NAME, killbillAPI, logService, region);

        final AdyenPaymentServiceProviderPort globalAdyenClient = adyenConfigurationHandler.createConfigurable(configProperties.getProperties());
        adyenConfigurationHandler.setDefaultConfigurable(globalAdyenClient);

        final AdyenConfigProperties adyenConfigProperties = adyenConfigPropertiesConfigurationHandler.createConfigurable(configProperties.getProperties());
        adyenConfigPropertiesConfigurationHandler.setDefaultConfigurable(adyenConfigProperties);

        final AdyenPaymentServiceProviderHostedPaymentPagePort globalAdyenHppClient = adyenHostedPaymentPageConfigurationHandler.createConfigurable(configProperties.getProperties());
        adyenHostedPaymentPageConfigurationHandler.setDefaultConfigurable(globalAdyenHppClient);

        final AdyenRecurringClient globalAdyenRecurringClient = adyenRecurringConfigurationHandler.createConfigurable(configProperties.getProperties());
        adyenRecurringConfigurationHandler.setDefaultConfigurable(globalAdyenRecurringClient);

        // Expose the healthcheck, so other plugins can check on the Adyen status
        final AdyenHealthcheck adyenHealthcheck = new AdyenHealthcheck(adyenConfigPropertiesConfigurationHandler);
        registerHealthcheck(context, adyenHealthcheck);

        // Register the servlet
        final PluginApp pluginApp = new PluginAppBuilder(PLUGIN_NAME,
                                                         killbillAPI,
                                                         logService,
                                                         dataSource,
                                                         super.clock,
                                                         configProperties).withRouteClass(AdyenServlet.class)
                                                                          .withRouteClass(AdyenHealthcheckServlet.class)
                                                                          .withService(adyenHealthcheck)
                                                                          .build();
        final HttpServlet adyenServlet = PluginApp.createServlet(pluginApp);
        registerServlet(context, adyenServlet);

        // Register the payment plugin
        final AdyenPaymentPluginApi pluginApi = new AdyenPaymentPluginApi(adyenConfigurationHandler,
                                                                          adyenConfigPropertiesConfigurationHandler,
                                                                          adyenHostedPaymentPageConfigurationHandler,
                                                                          adyenRecurringConfigurationHandler,
                                                                          killbillAPI,
                                                                          configProperties,
                                                                          logService,
                                                                          clock,
                                                                          dao);
        registerPaymentPluginApi(context, pluginApi);
        registerHandlers();
    }

    public void registerHandlers() {
        final PluginConfigurationEventHandler handler = new PluginConfigurationEventHandler(adyenConfigPropertiesConfigurationHandler, adyenConfigurationHandler, adyenHostedPaymentPageConfigurationHandler, adyenRecurringConfigurationHandler);
        dispatcher.registerEventHandlers(handler);
    }

    private void registerServlet(final BundleContext context, final HttpServlet servlet) {
        final Hashtable<String, String> props = new Hashtable<String, String>();
        props.put(OSGIPluginProperties.PLUGIN_NAME_PROP, PLUGIN_NAME);
        registrar.registerService(context, Servlet.class, servlet, props);
    }

    private void registerPaymentPluginApi(final BundleContext context, final PaymentPluginApi api) {
        final Hashtable<String, String> props = new Hashtable<String, String>();
        props.put(OSGIPluginProperties.PLUGIN_NAME_PROP, PLUGIN_NAME);
        registrar.registerService(context, PaymentPluginApi.class, api, props);
    }

    private void registerHealthcheck(final BundleContext context, final AdyenHealthcheck healthcheck) {
        final Hashtable<String, String> props = new Hashtable<String, String>();
        props.put(OSGIPluginProperties.PLUGIN_NAME_PROP, PLUGIN_NAME);
        registrar.registerService(context, Healthcheck.class, healthcheck, props);
    }
}
