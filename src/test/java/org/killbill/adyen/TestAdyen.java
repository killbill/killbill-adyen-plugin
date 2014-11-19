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

package org.killbill.adyen;

import com.google.common.io.Resources;
import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;
import org.jooq.SQLDialect;
import org.killbill.adyen.common.Amount;
import org.killbill.adyen.common.BrowserInfo;
import org.killbill.adyen.payment.*;
import org.killbill.billing.plugin.adyen.client.AdyenConfigProperties;
import org.killbill.billing.plugin.adyen.client.AdyenPaymentPortRegistry;
import org.killbill.billing.plugin.adyen.client.AdyenRequestSender;
import org.killbill.billing.plugin.adyen.client.PaymentPortRegistry;
import org.killbill.billing.plugin.adyen.core.AdyenConfig;
import org.killbill.billing.plugin.adyen.dao.AdyenDao;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;
import org.skife.config.ConfigurationObjectFactory;

import javax.sql.DataSource;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.Properties;
import java.util.UUID;

public class TestAdyen {

    private final AdyenConfigProperties properties;
    private final PaymentPortRegistry portRegistry;
    private final AdyenRequestSender sender;

    private final AdyenDao dao;

    public TestAdyen(AdyenConfigProperties properties, PaymentPortRegistry portRegistry, AdyenRequestSender sender) throws SQLException {
        this.properties = properties;
        this.portRegistry = portRegistry;
        this.sender = sender;

        DataSource mysqlDataSource = getMysqlDataSource();
        dao = new AdyenDao(mysqlDataSource, SQLDialect.MYSQL);
    }

    public void authorize() throws ServiceException, SQLException {
        Amount amount = new Amount();
        amount.setCurrency("EUR");
        amount.setValue(1234L);

        Card card = new Card();
        card.setHolderName("Jean Pierre");
        //card.setNumber("4111 1111 1111 1111");
        card.setCvc("737");
        card.setExpiryMonth("06");
        card.setExpiryYear("2016");


        PaymentRequest request = new PaymentRequest();
        request.setMerchantAccount("MyCityDealDE");
        request.setReference(UUID.randomUUID().toString());
        request.setAmount(amount);

        request.setCard(card);
        //request.setRecurring(recurring);
        PaymentResult result = sender.authorize("DE", request);

        dao.insertPaymentResult(UUID.randomUUID(), UUID.randomUUID(), "AUTHORIZE", result);
        System.out.println("auth code  = " + result.getAuthCode());

    }


    public void authorizeWith3DS() throws ServiceException, SQLException {
        Amount amount = new Amount();
        amount.setCurrency("EUR");
        amount.setValue(1234L);

        Card card = new Card();
        card.setHolderName("Jean Pierre");
        card.setNumber("4212 3456 7890 1237");
        card.setCvc("737");
        card.setExpiryMonth("06");
        card.setExpiryYear("2016");


        PaymentRequest request3DS = new PaymentRequest();
        request3DS.setMerchantAccount("MyCityDealDE");
        request3DS.setReference(UUID.randomUUID().toString());
        request3DS.setAmount(amount);

        BrowserInfo browserInfo = new BrowserInfo();
        browserInfo.setAcceptHeader("text/html");
        browserInfo.setUserAgent("Mozilla/5.0 (X11; U; Linux i686; en-US;rv:1.9) Gecko/2008052912 Firefox/3.0");

        request3DS.setBrowserInfo(browserInfo);
        /*
        request.setShopperEmail("foo@gamil.com");
        request.setShopperReference(UUID.randomUUID().toString());
        request.setShopperIP("192.168.1.101");
        request.setShopperStatement("Nothing to declare");
        */
        request3DS.setCard(card);
        //request.setRecurring(recurring);
        //PaymentResult result = sender.authorize("DE", request3DS);

        UUID kbAccountId = UUID.randomUUID();
        UUID kbPaymentId = UUID.randomUUID();
        //dao.insertPaymentResult(kbAccountId, kbPaymentId, "AUTHORIZE", result);
        //System.out.println("3DS  = " + result.getResultCode());


        PaymentRequest3D paymentRequest3D = new PaymentRequest3D();
        paymentRequest3D.setMerchantAccount("MyCityDealDE");
        paymentRequest3D.setBrowserInfo(browserInfo);
        paymentRequest3D.setMd("jl+6o4XXWRPpB49xGqgKIWp7B3jGLRdqiQg3rn5HGhvw3NNgQNW5IgsUVRYnUeZ+jMQIydTlBHN2yLK+ZgcUXaIlMVGqddOS0U5ZzbwB7xT8Lm5hW9yKs9UsVvhmqjtmxUO2iXfkNpVI9Fdl3ZPd8TDAsFBXTQ9Y0NICAecMeZDElSSRKHhWpm/s42eS0+lf7/CPoLHFWNqxfIbosePSVAFJ4YQO7fsO9o5weYIpVHGohdvbcyh7cTk6kYzz9SRCXKV2Fcj5MCEhF0H2LKJudMg5oituzlT/vCfcjJzbw55b84HpEF/6smf2dASHFZPUGvjPKLFsjdxAgc53EndWqLC3kDay4+rT4mA5fMXPtpRx7waSvfX3s2wyqb4TRqQs");
        paymentRequest3D.setPaResponse("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");

        PaymentResult result3D = sender.authorize3D("DE", paymentRequest3D);
        dao.insertPaymentResult(kbAccountId, kbPaymentId, "AUTHORIZE_3DS", result3D);

        System.out.println("auth code  = " + result3D.getAuthCode());

    }


    public static void main(String[] args) throws Exception {
        System.getProperties().setProperty("org.apache.cxf.Logger", "org.apache.cxf.common.logging.Log4jLogger");

        Properties properties = loadPropertiesFromFileOrSystemProperties();

        final AdyenConfig config = new ConfigurationObjectFactory(properties).build(AdyenConfig.class);
        AdyenConfigProperties configProperties = new AdyenConfigProperties(config);
        PaymentPortRegistry portRegistry = new AdyenPaymentPortRegistry(configProperties, new LogService() {
            @Override
            public void log(int level, String message) {
                System.out.println(message);
            }

            @Override
            public void log(int level, String message, Throwable exception) {

            }

            @Override
            public void log(ServiceReference sr, int level, String message) {
            }

            @Override
            public void log(ServiceReference sr, int level, String message, Throwable exception) {
            }
        });
        AdyenRequestSender adyenSender = new AdyenRequestSender(portRegistry);
        TestAdyen test = new TestAdyen(configProperties, portRegistry, adyenSender);
        test.authorizeWith3DS();
    }

    private static Properties loadPropertiesFromFileOrSystemProperties() throws IOException {
        final URL propertiesFileLocation = Resources.getResource("config.properties");
        final Properties properties = new Properties();
        properties.load(propertiesFileLocation.openStream());
        return properties;
    }

    private DataSource getMysqlDataSource() {
        MysqlDataSource ds = new MysqlDataSource();
        ds.setServerName("localhost");
        ds.setPortNumber(3306);
        ds.setDatabaseName("killbill");
        ds.setUser("root");
        ds.setPassword("root");
        return ds;
    }

}
