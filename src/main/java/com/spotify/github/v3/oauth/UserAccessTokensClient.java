/*-
 * -\-\-
 * github-api
 * --
 * Copyright (C) 2016 - 2020 Spotify AB
 * --
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * -/-/-
 */

package com.spotify.github.v3.oauth;

import com.spotify.github.jackson.Json;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Capable of generating user access tokens with oauth codes.
 *
 * TODO refresh logic, in the mean time we get by with non-refreshed ones
 *
 * @author Marko Kanala, Meliora Ltd (marko.kanala@meliora.fi)
 */
public class UserAccessTokensClient {
    private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String GET_USER_ACCESS_TOKEN_URL = "login/oauth/access_token";

    private final OkHttpClient client;
    private URI baseUrl;

    String urlFor(final String path) {
        return baseUrl.toString().replaceAll("/+$", "") + "/" + path.replaceAll("^/+", "");
    }

    private UserAccessTokensClient(final OkHttpClient client, final URI baseUrl) {
        this.client = client;
        this.baseUrl = baseUrl;
    }

    /**
     * @return UserAccessTokensClient with custom client to URI
     */
    public static UserAccessTokensClient create(final OkHttpClient client, final URI baseUrl) {
        return new UserAccessTokensClient(client, baseUrl);
    }

    /**
     * @return default UserAccessTokensClient to URI
     */
    public static UserAccessTokensClient create(final URI baseUrl) {
        return new UserAccessTokensClient(new OkHttpClient(), baseUrl);
    }

    /**
     * @return default UserAccessTokensClient to github.com
     */
    public static UserAccessTokensClient create() {
        try {
            return new UserAccessTokensClient(new OkHttpClient(), new URI("https://github.com"));
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public UserAccessToken generateUserAccessToken(final String clientId, final String clientSecret, final String oAuthCode) throws Exception {
        log.info("generateUserAccessToken: {}", oAuthCode);

        final String url = urlFor(GET_USER_ACCESS_TOKEN_URL);
        final Request request =
                new Request.Builder()
                    .addHeader("Accept", "application/json")
                    .url(url)
                    .post(
                            new FormBody.Builder()
                            // Required. The client ID for your GitHub App. The client ID is different from the app ID. You can find the client ID on the settings page for your app.
                            .add("client_id", clientId)
                            // Required. The client secret for your GitHub App. You can generate a client secret on the settings page for your app.
                            .add("client_secret", clientSecret)
                            .add("code", oAuthCode)
                            .build()
                    )
                    .build();

        final Response response = client.newCall(request).execute();

        if (!response.isSuccessful()) {
            throw new Exception(
                    String.format(
                            "Got non-2xx status %s when getting an USER access token from GitHub: %s",
                            response.code(), response.message()));
        }

        if (response.body() == null) {
            throw new Exception(
                    String.format(
                            "Got empty response body when getting an USER access token from GitHub, HTTP status was: %s",
                            response.message()));
        }
        final String text = response.body().string();
        response.body().close();
        return Json.create().fromJson(text, UserAccessToken.class);
    }

}
