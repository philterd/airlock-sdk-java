/*******************************************************************************
 * Copyright 2023 Philterd, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package com.mtnfog.test.philter;

import ai.philterd.airlock.AirlockClient;
import ai.philterd.airlock.model.StatusResponse;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.nio.charset.Charset;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Ignore
public class AirlockClientTest {

    private static final Logger LOGGER = LogManager.getLogger(AirlockClientTest.class);

    private static final String ENDPOINT = "https://10.0.2.227:8080/";

    @Test
    public void getPolicies() throws Exception {

        final AirlockClient client = new AirlockClient.AirlockClientBuilder()
                .withEndpoint(ENDPOINT)
                .withOkHttpClientBuilder(getUnsafeOkHttpClientBuilder())
                .build();

        final List<String> policyNames = client.getPolicies();

        Assert.assertTrue(policyNames != null);
        Assert.assertFalse(policyNames.isEmpty());

        for(final String name : policyNames) {
            LOGGER.info("Policy: {}",  name);
        }

    }

    @Test(expected = SSLHandshakeException.class)
    public void getPoliciesNoCertificate() throws Exception {

        final AirlockClient client = new AirlockClient.AirlockClientBuilder()
                .withEndpoint(ENDPOINT)
                .build();

        client.getPolicies();

    }

    @Test
    public void get() throws Exception {

        final AirlockClient client = new AirlockClient.AirlockClientBuilder()
                .withEndpoint(ENDPOINT)
                .withSslConfiguration("/tmp/client-test.jks", "changeit",
                        "/tmp/keystore-server.jks", "changeit")
                .build();

        final List<String> policyNames = client.getPolicies();

        Assert.assertTrue(policyNames != null);
        Assert.assertFalse(policyNames.isEmpty());

        for(final String name : policyNames) {
            LOGGER.info("Policy: {}",  name);
        }

    }

    @Test
    public void getByName() throws Exception {

        final AirlockClient client = new AirlockClient.AirlockClientBuilder()
                .withEndpoint(ENDPOINT)
                .withSslConfiguration("/tmp/client-test.jks", "changeit",
                        "/tmp/keystore-server.jks", "changeit")
                .build();

        final String filterProfile = client.Policy("default");

        Assert.assertTrue(filterProfile != null);
        Assert.assertTrue(filterProfile.length() > 0);

        LOGGER.info("Policy:\n{}", filterProfile);

    }

    @Test
    public void save() throws Exception {

        final AirlockClient client = new AirlockClient.AirlockClientBuilder()
                .withEndpoint(ENDPOINT)
                .withSslConfiguration("/tmp/client-test.jks", "changeit",
                        "/tmp/keystore-server.jks", "changeit")
                .build();

        final String json = IOUtils.toString(this.getClass().getResource("/default2.json"), Charset.defaultCharset());

        client.savePolicy(json);

    }

    @Test
    public void status() throws Exception {

        final AirlockClient client = new AirlockClient.AirlockClientBuilder()
                .withEndpoint(ENDPOINT)
                .withSslConfiguration("/tmp/client-test.jks", "changeit",
                        "/tmp/keystore-server.jks", "changeit")
                .withOkHttpClientBuilder(getUnsafeOkHttpClientBuilder())
                .build();

        final StatusResponse statusResponse = client.status();

        Assert.assertTrue(StringUtils.equals("Healthy", statusResponse.getStatus()));

    }

    // This is used to test against Philter running with a self-signed certificate.
    private OkHttpClient.Builder getUnsafeOkHttpClientBuilder() throws NoSuchAlgorithmException, KeyManagementException {

        final TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {

            @Override
            public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {
            }

            @Override
            public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {
            }

            @Override
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return new java.security.cert.X509Certificate[] {};
            }

        } };

        final SSLContext sslContext = SSLContext.getInstance("SSL");
        sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) trustAllCerts[0]);
        builder.connectTimeout(AirlockClient.DEFAULT_TIMEOUT_SEC, TimeUnit.SECONDS);
        builder.writeTimeout(AirlockClient.DEFAULT_TIMEOUT_SEC, TimeUnit.SECONDS);
        builder.readTimeout(AirlockClient.DEFAULT_TIMEOUT_SEC, TimeUnit.SECONDS);
        builder.connectionPool(new ConnectionPool(AirlockClient.DEFAULT_MAX_IDLE_CONNECTIONS, AirlockClient.DEFAULT_KEEP_ALIVE_DURATION_MS, TimeUnit.MILLISECONDS));
        builder.hostnameVerifier((hostname, session) -> true);

        return builder;

    }

}
