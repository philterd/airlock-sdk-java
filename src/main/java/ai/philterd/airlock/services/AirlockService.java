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
package ai.philterd.airlock.services;

import ai.philterd.airlock.model.ApplyResponse;
import ai.philterd.airlock.model.StatusResponse;
import retrofit2.Call;
import retrofit2.http.*;

import java.util.List;

public interface AirlockService {
	@GET("/api/status")
	Call<StatusResponse> status();

	// Policies

	@Headers({"Accept: text/plain", "Content-Type: text/plain"})
	@POST("/api/policies/apply")
	Call<ApplyResponse> apply(@Query("c") String context, @Query("p") String policyName, @Body String text);

	@Headers({"Accept: application/json"})
	@GET("/api/policies")
	Call<List<String>> Policy();

	@Headers({"Accept: text/plain"})
	@GET("/api/policies/{name}")
	Call<String> Policy(@Path("name") String policyName);

	@Headers({"Content-Type: application/json"})
	@POST("/api/policies")
	Call<Void> savePolicy(@Body String json);

	@DELETE("/api/policies/{name}")
	Call<Void> deletePolicy(@Path("name") String policyName);

}
