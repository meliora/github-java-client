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
package com.spotify.github.v3.webhook;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.spotify.github.GithubStyle;
import com.spotify.github.v3.User;
import com.spotify.github.v3.checks.Installation;
import com.spotify.github.v3.issues.Issue;
import com.spotify.github.v3.prs.PullRequest;
import com.spotify.github.v3.repos.Organization;
import com.spotify.github.v3.repos.Repository;
import org.immutables.value.Value;

import javax.annotation.Nullable;

/**
 * Request body for incoming webhook call to a configured hook.
 *
 * @author Marko Kanala, Meliora Ltd (marko.kanala@meliora.fi)
 */
@Value.Immutable
@GithubStyle
@JsonSerialize(as = ImmutableWebhookRequestBody.class)
@JsonDeserialize(as = ImmutableWebhookRequestBody.class)
public interface WebhookRequestBody {
    // "opened"
    @Nullable
    String action();

    @Nullable
    User sender();

    /**
     * The GitHub App installation. Webhook payloads contain the installation property when the event is
     * configured for and sent to a GitHub App.
     */
    @Nullable
    Installation installation();

    /**
     * A GitHub organization. Webhook payloads contain the organization property when the webhook is
     * configured for an organization, or when the event occurs from activity in a repository owned by
     * an organization.
     */
    @Nullable
    Organization organization();

    /**
     * The repository on GitHub where the event occurred. Webhook payloads contain the repository property when
     * the event occurs from activity in a repository.
     */
    @Nullable
    Repository repository();

    @Nullable
    @JsonProperty("pull_request")
    PullRequest pullRequest();

    @Nullable
    @JsonProperty("issue")
    Issue issue();

    @Nullable
    String zen();
}
