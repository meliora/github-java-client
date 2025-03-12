/*-
 * -\-\-
 * github-api
 * --
 * Copyright (C) 2016 - 2024 Spotify AB
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

package com.spotify.github.v3.clients;

import com.fasterxml.jackson.core.type.TypeReference;
import com.spotify.github.v3.User;
import com.spotify.github.v3.checks.Installation;
import com.spotify.github.v3.user.requests.SuspensionReason;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class UserClient {

  public static final int NO_CONTENT = 204;
  private final GitHubClient github;
  private final String owner;

  private static final String SUSPEND_USER_TEMPLATE = "/users/%s/suspended";
  private static final String USERS_URI_USERNAME_TEMPLATE = "/users/%s";
  private static final String GET_INSTALLATIONS_WITH_USER_ACCESS_TOKEN = "/user/installations";

  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final TypeReference<List<Installation>> INSTALLATION_LIST_TYPE_REFERENCE =
          new TypeReference<>() {};

  UserClient(final GitHubClient github, final String owner) {
    this.github = github;
    this.owner = owner;
  }

  static UserClient create(final GitHubClient github, final String owner) {
    return new UserClient(github, owner);
  }

  public GithubAppClient createGithubAppClient() {
    return new GithubAppClient(this.github, this.owner);
  }

  /**
   * Suspend a user.
   *
   * @param username username of the user to suspend
   * @return a CompletableFuture that indicates success or failure
   */
  public CompletableFuture<Boolean> suspendUser(
      final String username, final SuspensionReason reason) {
    final String path = String.format(SUSPEND_USER_TEMPLATE, username);
    return github
        .put(path, github.json().toJsonUnchecked(reason))
        .thenApply(resp -> resp.code() == NO_CONTENT);
  }

  /**
   * Unsuspend a user.
   *
   * @param username username of the user to unsuspend
   * @return a CompletableFuture that indicates success or failure
   */
  public CompletableFuture<Boolean> unSuspendUser(
      final String username, final SuspensionReason reason) {
    final String path = String.format(SUSPEND_USER_TEMPLATE, username);
    return github
        .delete(path, github.json().toJsonUnchecked(reason))
        .thenApply(resp -> resp.code() == NO_CONTENT);
  }

  /**
   * Get a specific user.
   *
   * @param username user name
   * @return an user
   */
  public CompletableFuture<User> getUser(final String username) {
    final String path = String.format(USERS_URI_USERNAME_TEMPLATE, username);
    log.info("Fetching user from " + path);
    return github.request(path, User.class);
  }

  /**
   * List app installations accessible to the user access token.
   *
   * The client this is called must be set up with user access token.
   *
   * @return a list of Installation
   */
  public CompletableFuture<List<Installation>> getInstallationsWithUserAccessToken() {
    return github.request(GET_INSTALLATIONS_WITH_USER_ACCESS_TOKEN, INSTALLATION_LIST_TYPE_REFERENCE);
  }

}
