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
package ai.philterd.airlock;

import ai.philterd.airlock.model.*;
import ai.philterd.airlock.model.exceptions.ClientException;
import ai.philterd.airlock.model.exceptions.ServiceUnavailableException;
import ai.philterd.airlock.model.exceptions.UnauthorizedException;
import ai.philterd.airlock.services.AirlockService;
import nl.altindag.sslcontext.SSLFactory;
import okhttp3.*;
import org.apache.commons.lang3.StringUtils;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Client class for Airlock's API. See https://www.philterd.ai.
 */
public class AirlockClient extends AbstractClient {

	public static final int DEFAULT_TIMEOUT_SEC = 30;
	public static final int DEFAULT_MAX_IDLE_CONNECTIONS = 20;
	public static final int DEFAULT_KEEP_ALIVE_DURATION_MS = 30 * 1000;

	private AirlockService service;

	public static class AirlockClientBuilder {

		private String endpoint;
		private OkHttpClient.Builder okHttpClientBuilder;
		private long timeout = DEFAULT_TIMEOUT_SEC;
		private int maxIdleConnections = DEFAULT_MAX_IDLE_CONNECTIONS;
		private int keepAliveDurationMs = DEFAULT_KEEP_ALIVE_DURATION_MS;
		private String keystore;
		private String keystorePassword;
		private String truststore;
		private String truststorePassword;

		public AirlockClientBuilder withEndpoint(String endpoint) {
			this.endpoint = endpoint;
			return this;
		}

		public AirlockClientBuilder withOkHttpClientBuilder(OkHttpClient.Builder okHttpClientBuilder) {
			this.okHttpClientBuilder = okHttpClientBuilder;
			return this;
		}

		public AirlockClientBuilder withTimeout(long timeout) {
			this.timeout = timeout;
			return this;
		}

		public AirlockClientBuilder withMaxIdleConnections(int maxIdleConnections) {
			this.maxIdleConnections = maxIdleConnections;
			return this;
		}

		public AirlockClientBuilder withKeepAliveDurationMs(int keepAliveDurationMs) {
			this.keepAliveDurationMs = keepAliveDurationMs;
			return this;
		}

		public AirlockClientBuilder withSslConfiguration(String keystore, String keystorePassword, String truststore, String truststorePassword) {
			this.keystore = keystore;
			this.keystorePassword = keystorePassword;
			this.truststore = truststore;
			this.truststorePassword = truststorePassword;
			return this;
		}

		public AirlockClient build() throws Exception {
			return new AirlockClient(endpoint, okHttpClientBuilder, timeout, maxIdleConnections, keepAliveDurationMs, keystore,
					keystorePassword, truststore, truststorePassword);
		}

	}

	private AirlockClient(String endpoint, OkHttpClient.Builder okHttpClientBuilder, long timeout, int maxIdleConnections, int keepAliveDurationMs,
						  String keystore, String keystorePassword, String truststore, String truststorePassword) {

		if(okHttpClientBuilder == null) {

			okHttpClientBuilder = new OkHttpClient.Builder()
					.connectTimeout(timeout, TimeUnit.SECONDS)
					.writeTimeout(timeout, TimeUnit.SECONDS)
					.readTimeout(timeout, TimeUnit.SECONDS)
					.connectionPool(new ConnectionPool(maxIdleConnections, keepAliveDurationMs, TimeUnit.MILLISECONDS));

		}

		if(StringUtils.isNotEmpty(keystore)) {
			configureSSL(okHttpClientBuilder, keystore, keystorePassword, truststore, truststorePassword);
		}

		final OkHttpClient okHttpClient = okHttpClientBuilder.build();

		final Retrofit.Builder builder = new Retrofit.Builder()
				.baseUrl(endpoint)
				.client(okHttpClient)
				.addConverterFactory(ScalarsConverterFactory.create())
				.addConverterFactory(GsonConverterFactory.create());

		final Retrofit retrofit = builder.build();

		service = retrofit.create(AirlockService.class);

	}

	private void configureSSL(final OkHttpClient.Builder okHttpClientBuilder, String keystore, String keystorePassword,
							 String truststore, String truststorePassword) {

		final SSLFactory sslFactory = SSLFactory.builder()
				.withIdentityMaterial(Paths.get(keystore), keystorePassword.toCharArray())
				.withTrustMaterial(Paths.get(truststore), truststorePassword.toCharArray())
				.build();

		okHttpClientBuilder.sslSocketFactory(sslFactory.getSslSocketFactory(), sslFactory.getTrustManager().get());

	}

	/**
	 * Send text to Philter to be filtered and get an explanation.
	 * @param context The context. Contexts can be used to group text based on some arbitrary property.
	 * @param documentId The document ID. Leave empty for Philter to assign a document ID to the request.
	 * @param policyName The name of the policy to apply to the text.
	 * @param text The text to be filtered.
	 * @return The filter {@link ApplyResponse}.
	 * @throws IOException Thrown if the request can not be completed.
	 */
	public ApplyResponse apply(String context, String documentId, String policyName, String text) throws IOException {

		final Response<ApplyResponse> response = service.apply(context, documentId, policyName, text).execute();

		if(response.isSuccessful()) {

			return response.body();

		} else {

			if(response.code() == 401) {

				throw new UnauthorizedException(UNAUTHORIZED);

			} else if(response.code() == 503) {

				throw new ServiceUnavailableException(SERVICE_UNAVAILABLE);

			} else {

				throw new ClientException("Unknown error: HTTP " + response.code());

			}

		}

	}

	/**
	 * Gets the status of Philter.
	 * @return A {@link StatusResponse} object.
	 * @throws IOException Thrown if the request can not be completed.
	 */
	public StatusResponse status() throws IOException {

		final Response<StatusResponse> response = service.status().execute();

		if(response.isSuccessful()) {

			return response.body();

		} else {

			if(response.code() == 503) {

				throw new ServiceUnavailableException(SERVICE_UNAVAILABLE);

			} else {

				throw new ClientException("Unknown error: HTTP " + response.code());

			}

		}

	}

	/**
	 * Gets a list of policy names.
	 * @return A list of policy names.
	 * @throws IOException Thrown if the call not be executed.
	 */
	public List<String> getPolicies() throws IOException {

		final Response<List<String>> response = service.policy().execute();

		if(response.isSuccessful()) {

			return response.body();

		} else {

			if(response.code() == 401) {

				throw new UnauthorizedException(UNAUTHORIZED);

			} else if(response.code() == 503) {

				throw new ServiceUnavailableException(SERVICE_UNAVAILABLE);

			} else {

				throw new ClientException("Unknown error: HTTP " + response.code());

			}

		}

	}

	/**
	 * Gets the content of a policy.
	 * @param policyName The name of the policy to get.
	 * @return The content of the policy.
	 * @throws IOException Thrown if the call not be executed.
	 */
	public String Policy(String policyName) throws IOException {

		final Response<String> response = service.policy(policyName).execute();

		if(response.isSuccessful()) {

			return response.body();

		} else {

			if(response.code() == 401) {

				throw new UnauthorizedException(UNAUTHORIZED);

			} else if(response.code() == 503) {

				throw new ServiceUnavailableException(SERVICE_UNAVAILABLE);

			} else {

				throw new ClientException("Unknown error: HTTP " + response.code());

			}

		}

	}

	/**
	 * Saves (or overwrites) the policy.
	 * @param json The body of the policy.
	 * @throws IOException Thrown if the call not be executed.
	 */
	public void savePolicy(String json) throws IOException {

		final Response<Void> response = service.savePolicy(json).execute();

		if(!response.isSuccessful()) {

			if(response.code() == 401) {

				throw new UnauthorizedException(UNAUTHORIZED);

			} else if(response.code() == 503) {

				throw new ServiceUnavailableException(SERVICE_UNAVAILABLE);

			} else {

				throw new ClientException("Unknown error: HTTP " + response.code());

			}

		}

	}

	/**
	 * Deletes a policy.
	 * @param policyName The name of the policy to delete.
	 * @throws IOException Thrown if the call not be executed.
	 */
	public void deletePolicy(String policyName) throws IOException {

		final Response<Void> response = service.deletePolicy(policyName).execute();

		if(!response.isSuccessful()) {

			if(response.code() == 401) {

				throw new UnauthorizedException(UNAUTHORIZED);

			} else if(response.code() == 503) {

				throw new ServiceUnavailableException(SERVICE_UNAVAILABLE);

			} else {

				throw new ClientException("Unknown error: HTTP " + response.code());

			}

		}

	}

}
