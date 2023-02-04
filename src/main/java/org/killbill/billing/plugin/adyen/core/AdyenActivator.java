/*
 * Copyright 2021 Wovenware, Inc
 *
 * Wovenware licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
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
import org.killbill.billing.plugin.adyen.core.resources.AdyenCheckoutService;
import org.killbill.billing.plugin.adyen.core.resources.AdyenCheckoutServlet;
import org.killbill.billing.plugin.adyen.core.resources.AdyenHealthcheckServlet;
import org.killbill.billing.plugin.adyen.core.resources.AdyenNotificationServlet;
import org.killbill.billing.plugin.adyen.dao.AdyenDao;
import org.killbill.billing.plugin.api.notification.PluginConfigurationEventHandler;
import org.killbill.billing.plugin.core.config.PluginEnvironmentConfig;
import org.killbill.billing.plugin.core.resources.jooby.PluginApp;
import org.killbill.billing.plugin.core.resources.jooby.PluginAppBuilder;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AdyenActivator extends KillbillActivatorBase {

  private static final Logger logger = LoggerFactory.getLogger(AdyenActivator.class);

  public static final String PLUGIN_NAME = "adyen-plugin";

  private AdyenConfigurationHandler adyenConfigurationHandler;

  @Override
  public void start(final BundleContext context) throws Exception {
    super.start(context);

    logger.info(" starting plugin {}", PLUGIN_NAME);
    final AdyenDao adyenDao = new AdyenDao(dataSource.getDataSource());

    final String region = PluginEnvironmentConfig.getRegion(configProperties.getProperties());

    // Register an event listener for plugin configuration (optional)
    logger.info("Registering an event listener for plugin configuration");
    adyenConfigurationHandler = new AdyenConfigurationHandler(region, PLUGIN_NAME, killbillAPI);
    final AdyenConfigProperties globalConfiguration =
        adyenConfigurationHandler.createConfigurable(configProperties.getProperties());
    adyenConfigurationHandler.setDefaultConfigurable(globalConfiguration);

    // As an example, this plugin registers a PaymentPluginApi (this could be changed to any other
    // plugin api)
    logger.info("Registering an APIs");
    final PaymentPluginApi paymentPluginApi =
        new AdyenPaymentPluginApi(
            adyenConfigurationHandler, killbillAPI, configProperties, clock.getClock(), adyenDao);
    registerPaymentPluginApi(context, paymentPluginApi);

    // Expose a healthcheck (optional), so other plugins can check on the plugin status
    logger.info("Registering healthcheck");
    final Healthcheck healthcheck = new AdyenHealthcheck();
    registerHealthcheck(context, healthcheck);
    final AdyenCheckoutService checkoutService =
        new AdyenCheckoutService(killbillAPI, adyenConfigurationHandler);
    // Register a servlet (optional)
    final PluginApp pluginApp =
        new PluginAppBuilder(PLUGIN_NAME, killbillAPI, dataSource, super.clock, configProperties)
            .withRouteClass(AdyenHealthcheckServlet.class)
            .withRouteClass(AdyenNotificationServlet.class)
            .withRouteClass(AdyenCheckoutServlet.class)
            .withService(healthcheck)
            .withService(clock)
            .withService(checkoutService)
            .withService(paymentPluginApi)
            .build();
    final HttpServlet httpServlet = PluginApp.createServlet(pluginApp);

    registerServlet(context, httpServlet);

    registerHandlers();
  }

  private void registerHandlers() {
    final PluginConfigurationEventHandler configHandler =
        new PluginConfigurationEventHandler(adyenConfigurationHandler);
    dispatcher.registerEventHandlers(configHandler);
  }

  private void registerServlet(final BundleContext context, final Servlet servlet) {
    final Hashtable<String, String> props = new Hashtable<>();
    props.put(OSGIPluginProperties.PLUGIN_NAME_PROP, PLUGIN_NAME);
    registrar.registerService(context, Servlet.class, servlet, props);
  }

  private void registerPaymentPluginApi(final BundleContext context, final PaymentPluginApi api) {
    final Hashtable<String, String> props = new Hashtable<>();
    props.put(OSGIPluginProperties.PLUGIN_NAME_PROP, PLUGIN_NAME);
    registrar.registerService(context, PaymentPluginApi.class, api, props);
  }

  private void registerHealthcheck(final BundleContext context, final Healthcheck healthcheck) {
    final Hashtable<String, String> props = new Hashtable<>();
    props.put(OSGIPluginProperties.PLUGIN_NAME_PROP, PLUGIN_NAME);
    registrar.registerService(context, Healthcheck.class, healthcheck, props);
  }
}
