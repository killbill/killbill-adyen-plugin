/*
 * Copyright 2014-2015 Groupon, Inc
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

import org.killbill.billing.osgi.api.OSGIPluginProperties;
import org.killbill.billing.payment.plugin.api.PaymentPluginApi;
import org.killbill.billing.plugin.adyen.api.AdyenPaymentPluginApi;
import org.killbill.billing.plugin.adyen.client.payment.service.AdyenPaymentServiceProviderHostedPaymentPagePort;
import org.killbill.billing.plugin.adyen.client.payment.service.AdyenPaymentServiceProviderPort;
import org.killbill.billing.plugin.adyen.dao.AdyenDao;
import org.killbill.billing.plugin.api.notification.PluginConfigurationEventHandler;
import org.killbill.clock.Clock;
import org.killbill.clock.DefaultClock;
import org.killbill.killbill.osgi.libs.killbill.KillbillActivatorBase;
import org.killbill.killbill.osgi.libs.killbill.OSGIKillbillEventDispatcher.OSGIKillbillEventHandler;
import org.osgi.framework.BundleContext;

public class AdyenActivator extends KillbillActivatorBase {

    public static final String PLUGIN_NAME = "killbill-adyen";

    private AdyenConfigurationHandler adyenConfigurationHandler;
    private AdyenHostedPaymentPageConfigurationHandler adyenHostedPaymentPageConfigurationHandler;

    @Override
    public void start(final BundleContext context) throws Exception {
        super.start(context);

        final Clock clock = new DefaultClock();
        final AdyenDao dao = new AdyenDao(dataSource.getDataSource());

        // Register the servlet
        final AdyenServlet adyenServlet = new AdyenServlet();
        registerServlet(context, adyenServlet);

        final AdyenPaymentServiceProviderPort globalAdyenClient = adyenConfigurationHandler.createConfigurable(configProperties.getProperties());
        adyenConfigurationHandler.setDefaultConfigurable(globalAdyenClient);

        final AdyenPaymentServiceProviderHostedPaymentPagePort globalAdyenHppClient = adyenHostedPaymentPageConfigurationHandler.createConfigurable(configProperties.getProperties());
        adyenHostedPaymentPageConfigurationHandler.setDefaultConfigurable(globalAdyenHppClient);

        // Register the payment plugin
        final AdyenPaymentPluginApi pluginApi = new AdyenPaymentPluginApi(adyenConfigurationHandler, adyenHostedPaymentPageConfigurationHandler, killbillAPI, configProperties, logService, clock, dao, killbillMetrics);
        registerPaymentPluginApi(context, pluginApi);
    }

    @Override
    public OSGIKillbillEventHandler getOSGIKillbillEventHandler() {
        adyenConfigurationHandler = new AdyenConfigurationHandler(PLUGIN_NAME, killbillAPI, logService);
        adyenHostedPaymentPageConfigurationHandler = new AdyenHostedPaymentPageConfigurationHandler(PLUGIN_NAME, killbillAPI, logService);
        return new PluginConfigurationEventHandler(adyenConfigurationHandler, adyenHostedPaymentPageConfigurationHandler);
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
}
