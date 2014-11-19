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

package org.killbill.billing.plugin.adyen.dao;

import org.jooq.Result;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.killbill.adyen.payment.PaymentResult;
import org.killbill.billing.plugin.adyen.dao.gen.Tables;
import org.killbill.billing.plugin.adyen.dao.gen.tables.records.AdyenPluginGatewayResponsesRecord;

import javax.sql.DataSource;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import static org.killbill.billing.plugin.adyen.dao.gen.tables.AdyenPluginGatewayResponses.ADYEN_PLUGIN_GATEWAY_RESPONSES;

public final class AdyenDao {

    private final DataSource dataSource;
    private final SQLDialect dialect;

    public AdyenDao(final DataSource dataSource, final SQLDialect dialect) {
        this.dataSource = dataSource;
        this.dialect = dialect;
    }

    public void insertPaymentResult(final UUID kbAccountId, final UUID kbPaymentId, final String transactionType, final PaymentResult result) throws SQLException {
        execute(dataSource.getConnection(), new WithConnectionCallback<Void>() {
            @Override
            public Void withConnection(Connection conn) throws SQLException {
                DSL.using(conn, dialect).insertInto(Tables.ADYEN_PLUGIN_GATEWAY_RESPONSES,
                        ADYEN_PLUGIN_GATEWAY_RESPONSES.KB_ACCOUNT_ID,
                        ADYEN_PLUGIN_GATEWAY_RESPONSES.KB_PAYMENT_ID,
                        ADYEN_PLUGIN_GATEWAY_RESPONSES.TRANSACTION_TYPE,
                        ADYEN_PLUGIN_GATEWAY_RESPONSES.AUTH_CODE,
                        ADYEN_PLUGIN_GATEWAY_RESPONSES.DCC_AMOUNT,
                        ADYEN_PLUGIN_GATEWAY_RESPONSES.DCC_CURRENCY,
                        ADYEN_PLUGIN_GATEWAY_RESPONSES.DCC_SIGNATURE,
                        ADYEN_PLUGIN_GATEWAY_RESPONSES.ISSUER_URL,
                        ADYEN_PLUGIN_GATEWAY_RESPONSES.MD,
                        ADYEN_PLUGIN_GATEWAY_RESPONSES.PA_REQUEST,
                        ADYEN_PLUGIN_GATEWAY_RESPONSES.PSP_REFERENCE,
                        ADYEN_PLUGIN_GATEWAY_RESPONSES.REFUSAL_REASON,
                        ADYEN_PLUGIN_GATEWAY_RESPONSES.RESULT_CODE)
                        .values(kbAccountId.toString(),
                                kbPaymentId.toString(),
                                transactionType,
                                result.getAuthCode(),
                                (result.getDccAmount() == null || result.getDccAmount().getValue() == null) ? null : BigDecimal.valueOf(result.getDccAmount().getValue()),
                                (result.getDccAmount() == null) ? null : result.getDccAmount().getCurrency(),
                                result.getDccSignature(),
                                result.getIssuerUrl(),
                                result.getMd(),
                                result.getPaRequest(),
                                result.getPspReference(),
                                result.getRefusalReason(),
                                result.getResultCode())
                        .execute();
                return null;
            }
        });
    }

    private String urlDecode(final String input) {
        if (input == null) {
            return null;
        }
        try {
            return URLDecoder.decode(input, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // STEPH may need more thinking about that case
            throw new RuntimeException(e);
        }
    }

    public List<AdyenPluginGatewayResponsesRecord> getEntriesForPaymentId(final UUID kbPaymentId) throws SQLException {
        return execute(dataSource.getConnection(), new WithConnectionCallback<List<AdyenPluginGatewayResponsesRecord>>() {
            @Override
            public List<AdyenPluginGatewayResponsesRecord> withConnection(Connection conn) throws SQLException {
                final Result<AdyenPluginGatewayResponsesRecord> records = DSL.using(conn, dialect).selectFrom(Tables.ADYEN_PLUGIN_GATEWAY_RESPONSES)
                        .where(ADYEN_PLUGIN_GATEWAY_RESPONSES.KB_PAYMENT_ID.equal(kbPaymentId.toString()))
                        .fetch();
                return records;
            }
        });
    }

    private interface WithConnectionCallback<T> {
        public T withConnection(final Connection conn) throws SQLException;
    }

    private <T> T execute(final Connection conn, WithConnectionCallback<T> callback) throws SQLException {
        try {
            return callback.withConnection(conn);
        } finally {
            conn.close();
        }
    }

}
