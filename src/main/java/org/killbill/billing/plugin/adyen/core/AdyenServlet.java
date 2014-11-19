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

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import com.google.common.io.InputSupplier;
import org.osgi.service.log.LogService;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

public class AdyenServlet extends HttpServlet {

    private final LogService logService;

    public AdyenServlet(final LogService logService) {
        this.logService = logService;
    }

    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
        // Find me on http://127.0.0.1:8080/plugins/killbill-adyen
        logService.log(LogService.LOG_INFO, "It works!");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        final InputStream input = req.getInputStream();
        String formUrlEncoded = CharStreams.toString(CharStreams.newReaderSupplier(new InputSupplier<InputStream>() {
            public InputStream getInput() throws IOException {
                return input;
            }
        }, Charsets.UTF_8));

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

        StringBuffer tmp = new StringBuffer("TermUrl parameters:\n\n");
        for (String key: parameters.keySet()) {
            tmp.append(key);
            tmp.append(": ");
            tmp.append(parameters.get(key));
            tmp.append("\n\n");
        }

        logService.log(LogService.LOG_INFO, tmp.toString());
    }
}
