/*
 * Copyright 2015 Groupon, Inc
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

package org.killbill.billing.plugin.adyen.recurring;

import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.transport.http.HTTPConduit;
import org.killbill.adyen.recurring.Recurring;
import org.killbill.adyen.recurring.RecurringPortType;
import org.killbill.billing.plugin.adyen.client.AdyenConfigProperties;
import org.killbill.billing.plugin.adyen.client.jaxws.IgnoreUnexpectedElementsEventHandler;

import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Service;
import javax.xml.ws.soap.SOAPBinding;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.google.common.base.Preconditions.checkNotNull;

public class AdyenRecurringPortRegistry implements RecurringPortRegistry {

    private final static String RECURRING_SERVICE_SUFFIX = "-recurringService";
    private final Map<String, RecurringPortType> services;
    private final AdyenConfigProperties config;

    public AdyenRecurringPortRegistry(AdyenConfigProperties config) {
        this.config = config;
        this.services = new ConcurrentHashMap<String, RecurringPortType>();
    }

    @Override
    public RecurringPortType getRecurringPort(final String countryIsoCode) {


        if (!this.services.containsKey(countryIsoCode + RECURRING_SERVICE_SUFFIX)) {
            RecurringPortType service = createService(Recurring.SERVICE, Recurring.RecurringHttpPort, config.getRecurringUrl(),
                    config.getUserName(countryIsoCode), config.getPassword(countryIsoCode), config.getRecurringConnectionTimeout(), config.getRecurringReceiveTimeout());
            this.services.put(countryIsoCode + RECURRING_SERVICE_SUFFIX, service);
        }
        return this.services.get(countryIsoCode + RECURRING_SERVICE_SUFFIX);
    }


    private RecurringPortType createService(final QName service,
                                            final QName portName,
                                            final String address,
                                            final String userName,
                                            final String password,
                                            final String connectionTimeout,
                                            final String receiveTimeout) {
        checkNotNull(service, "service");
        checkNotNull(portName, "portName");
        checkNotNull(address, "address");
        checkNotNull(userName, "userName");
        checkNotNull(password, "password");
        // we don't use an wsdl as we don't want the hassle with retrieving it online(that may change, be unavailable)
        // or from the right location in all our different deployments
        Service result = Service.create(null, service);
        // configure the endPointAddress
        result.addPort(portName, SOAPBinding.SOAP11HTTP_BINDING, address);
        // configure username/pw
        RecurringPortType port = result.getPort(portName, RecurringPortType.class);
        Client client = ClientProxy.getClient(port);
        client.getEndpoint().put("jaxb-validation-event-handler", new IgnoreUnexpectedElementsEventHandler());
        if (connectionTimeout != null) {
            HTTPConduit conduit = (HTTPConduit) client.getConduit();
            conduit.getClient().setReceiveTimeout(Long.valueOf(connectionTimeout));
        }
        if (receiveTimeout != null) {
            HTTPConduit conduit = (HTTPConduit) client.getConduit();
            conduit.getClient().setReceiveTimeout(Long.valueOf(receiveTimeout));
        }
        ((BindingProvider) port).getRequestContext().put(BindingProvider.USERNAME_PROPERTY, userName);
        ((BindingProvider) port).getRequestContext().put(BindingProvider.PASSWORD_PROPERTY, password);

        return port;
    }

}
