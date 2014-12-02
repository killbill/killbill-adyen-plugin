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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.killbill.billing.plugin.core.PluginServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AdyenServlet extends PluginServlet {

    private static final Logger logger = LoggerFactory.getLogger(AdyenServlet.class);

    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
    }

    @Override
    protected void doPost(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
        final String formUrlEncoded = getRequestData(req);

        final String[] keyValuePairs = formUrlEncoded.split("\\&");
        final Map<String, String> parameters = new HashMap<String, String>();
        for (final String keyValuePair : keyValuePairs) {
            if (keyValuePair != null && !keyValuePair.isEmpty()) {
                final String[] keyValue = keyValuePair.split("=");
                if (keyValue.length != 2) {
                    throw new RuntimeException("Invalid parameters :" + formUrlEncoded);
                }
                parameters.put(keyValue[0], keyValue[1]);
            }
        }

        final StringBuffer tmp = new StringBuffer("TermUrl parameters:\n\n");
        for (final String key : parameters.keySet()) {
            tmp.append(key);
            tmp.append(": ");
            tmp.append(parameters.get(key));
            tmp.append("\n\n");
        }

        logger.info(tmp.toString());
    }
}
