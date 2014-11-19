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

import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;
import org.jooq.SQLDialect;
import org.killbill.adyen.common.Amount;
import org.killbill.adyen.payment.PaymentResult;
import org.killbill.billing.plugin.adyen.dao.AdyenDao;
import org.killbill.billing.plugin.adyen.dao.gen.tables.records.AdyenPluginGatewayResponsesRecord;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public class TestJooq {

    UUID KB_ACCOUNT_ID = UUID.fromString("cb67faab-cb75-4981-a5e4-4654c480932d");
    UUID KB_PAYMENT_ID = UUID.fromString("dfcdd88c-dd3b-47a2-81f7-65867c18f836");

    private AdyenDao dao;

    @BeforeTest
    public void beforeTest() throws SQLException {
        DataSource mysqlDataSource = getMysqlDataSource();
        mysqlDataSource.getConnection().prepareStatement("delete from  adyen_plugin_gateway_responses where kb_account_id='" + KB_ACCOUNT_ID.toString() + "';").execute();
        dao = new AdyenDao(mysqlDataSource, SQLDialect.MYSQL);
    }

    @Test
    public void test() throws SQLException {
        PaymentResult paymentResult = new PaymentResult();
        String authCode = "2343274";
        paymentResult.setAuthCode(authCode);
        Amount amount = new Amount();
        amount.setValue(12L);
        amount.setCurrency("EUR");
        paymentResult.setDccAmount(amount);

        dao.insertPaymentResult(KB_ACCOUNT_ID,
                KB_PAYMENT_ID,
            "Authorised",
                paymentResult);
        final List<AdyenPluginGatewayResponsesRecord> result = dao.getEntriesForPaymentId(KB_PAYMENT_ID);
        Assert.assertEquals(result.size(), 1);
        Assert.assertEquals(result.get(0).getAuthCode(), authCode);
        Assert.assertEquals(result.get(0).getDccAmount().compareTo(new BigDecimal("12.0")), 0);
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
