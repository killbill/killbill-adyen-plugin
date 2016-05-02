/*
 * Copyright 2014-2016 Groupon, Inc
 * Copyright 2014-2016 The Billing Project, LLC
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

package org.killbill.billing.plugin.adyen.client;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Service;
import javax.xml.ws.soap.SOAPBinding;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.logging.Slf4jLogger;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.apache.cxf.transports.http.configuration.ProxyServerType;
import org.killbill.adyen.payment.Payment;
import org.killbill.adyen.payment.PaymentPortType;
import org.killbill.billing.plugin.adyen.client.jaxws.HttpHeaderInterceptor;
import org.killbill.billing.plugin.adyen.client.jaxws.IgnoreUnexpectedElementsEventHandler;
import org.killbill.billing.plugin.adyen.client.jaxws.LoggingInInterceptor;
import org.killbill.billing.plugin.adyen.client.jaxws.LoggingOutInterceptor;

import com.google.common.base.Preconditions;

public class AdyenPaymentPortRegistry implements PaymentPortRegistry {

    private static final String PAYMENT_SERVICE_SUFFIX = "-paymentService";

    private final Map<String, Object> services = new ConcurrentHashMap<String, Object>();

    private final LoggingOutInterceptor loggingOutInterceptor;
    private final LoggingInInterceptor loggingInInterceptor;
    private final HttpHeaderInterceptor httpHeaderInterceptor;

    protected final AdyenConfigProperties config;

    public AdyenPaymentPortRegistry(final AdyenConfigProperties config,
                                    final LoggingInInterceptor loggingInInterceptor,
                                    final LoggingOutInterceptor loggingOutInterceptor,
                                    final HttpHeaderInterceptor httpHeaderInterceptor) {
        this.loggingInInterceptor = loggingInInterceptor;
        this.loggingOutInterceptor = loggingOutInterceptor;
        this.config = Preconditions.checkNotNull(config, "config");
        this.httpHeaderInterceptor = httpHeaderInterceptor;
    }

    @Override
    public void close() throws IOException {
        for (final Object service : services.values()) {
            try {
                // See ClientProxy.getClient
                ((ClientProxy) Proxy.getInvocationHandler(service)).close();
            } catch (final RuntimeException ignored) {
            }
        }
    }

    @Override
    public PaymentPortType getPaymentPort(final String countryIsoCode) {
        return createService(countryIsoCode,
                             PAYMENT_SERVICE_SUFFIX,
                             PaymentPortType.class,
                             Payment.SERVICE,
                             Payment.PaymentHttpPort,
                             config.getPaymentUrl(),
                             config.getPaymentConnectionTimeout(),
                             config.getPaymentReadTimeout());
    }

    protected <T> T createService(final String countryIsoCode, final String suffix,
                                  final Class<T> clazz,
                                  final QName serviceName,
                                  final QName portName,
                                  final String address,
                                  final String connectionTimeout,
                                  final String readTimeout) {
        if (!this.services.containsKey(countryIsoCode + suffix)) {
            synchronized (this) {
                if (!this.services.containsKey(countryIsoCode + suffix)) {
                    final T service = createService(countryIsoCode,
                                                    clazz,
                                                    serviceName,
                                                    portName,
                                                    address,
                                                    connectionTimeout,
                                                    readTimeout);
                    this.services.put(countryIsoCode + suffix, service);
                }
            }
        }
        return (T) this.services.get(countryIsoCode + suffix);
    }

    private <T> T createService(final String countryIsoCode,
                                final Class<T> clazz,
                                final QName service,
                                final QName portName,
                                final String address,
                                final String connectionTimeout,
                                final String readTimeout) {
        final String countryCode = AdyenConfigProperties.gbToUK(countryIsoCode);

        return createService(clazz,
                             service,
                             portName,
                             address,
                             config.getUserName(countryCode),
                             config.getPassword(countryCode),
                             connectionTimeout,
                             readTimeout);
    }

    private <T> T createService(final Class<T> clazz,
                                final QName service,
                                final QName portName,
                                final String address,
                                final String userName,
                                final String password,
                                final String connectionTimeout,
                                final String readTimeout) {
        Preconditions.checkNotNull(service, "service");
        Preconditions.checkNotNull(portName, "portName");
        Preconditions.checkNotNull(address, "address");
        Preconditions.checkNotNull(userName, "username");
        Preconditions.checkNotNull(password, "password");

        // Delegate logging to slf4j (see also https://github.com/killbill/killbill-platform/tree/master/osgi-bundles/libs/slf4j-osgi)
        LogUtils.setLoggerClass(Slf4jLogger.class);

        final Service result = Service.create(null, service);
        result.addPort(portName, SOAPBinding.SOAP11HTTP_BINDING, address);
        final T port = result.getPort(portName, clazz);
        final Client client = ClientProxy.getClient(port);
        client.getEndpoint().put("jaxb-validation-event-handler", new IgnoreUnexpectedElementsEventHandler());

        final HTTPConduit conduit = (HTTPConduit) client.getConduit();
        final HTTPClientPolicy clientPolicy = conduit.getClient();
        clientPolicy.setAllowChunking(config.getAllowChunking());
        if (config.getTrustAllCertificates()) {
            final TLSClientParameters tcp = new TLSClientParameters();
            tcp.setTrustManagers(new TrustManager[]{new TrustAllX509TrustManager()});
            conduit.setTlsClientParameters(tcp);
        }
        if (connectionTimeout != null) {
            clientPolicy.setConnectionTimeout(Long.valueOf(connectionTimeout));
        }
        if (readTimeout != null) {
            clientPolicy.setReceiveTimeout(Long.valueOf(readTimeout));
        }
        if (config.getProxyServer() != null) {
            clientPolicy.setProxyServer(config.getProxyServer());
        }
        if (config.getProxyPort() != null) {
            clientPolicy.setProxyServerPort(config.getProxyPort());
        }
        if (config.getProxyType() != null) {
            clientPolicy.setProxyServerType(ProxyServerType.fromValue(config.getProxyType().toUpperCase()));
        }

        ((BindingProvider) port).getRequestContext().put(BindingProvider.USERNAME_PROPERTY, userName);
        ((BindingProvider) port).getRequestContext().put(BindingProvider.PASSWORD_PROPERTY, password);

        final Endpoint endpoint = client.getEndpoint();
        endpoint.getInInterceptors().add(loggingInInterceptor);
        endpoint.getOutInterceptors().add(loggingOutInterceptor);
        endpoint.getOutInterceptors().add(httpHeaderInterceptor);

        return port;
    }

    private static final class TrustAllX509TrustManager implements X509TrustManager {

        private static final X509Certificate[] acceptedIssuers = {};

        public void checkClientTrusted(final X509Certificate[] chain, final String authType) {}

        public void checkServerTrusted(final X509Certificate[] chain, final String authType) {}

        public X509Certificate[] getAcceptedIssuers() {
            return acceptedIssuers;
        }
    }
}
