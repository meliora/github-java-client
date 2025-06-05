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

package com.spotify.github.v3.clients;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.spotify.github.async.AsyncPage;
import com.spotify.github.v3.AttachmentFile;
import com.spotify.github.v3.ImmutableAttachmentFile;
import com.spotify.github.v3.comment.Comment;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.spotify.github.v3.issues.Issue;
import com.spotify.github.v3.issues.requests.IssueParameters;
import okhttp3.ResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.HttpHeaders;

import static com.spotify.github.v3.clients.GitHubClient.*;

/** Issue API client */
public class IssueClient {

  static final String COMMENTS_URI_NUMBER_TEMPLATE = "/repos/%s/%s/issues/%s/comments";
  static final String COMMENTS_URI_TEMPLATE = "/repos/%s/%s/issues/comments";
  static final String COMMENTS_URI_ID_TEMPLATE = "/repos/%s/%s/issues/comments/%s";

  static final String ISSUES_URI_NUMBER_TEMPLATE = "/repos/%s/%s/issues/%s";

  static final String ISSUES_URI_LIST_TEMPLATE = "/repos/%s/%s/issues";

  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final GitHubClient github;
  private final String owner;
  private final String repo;

  IssueClient(final GitHubClient github, final String owner, final String repo) {
    this.github = github;
    this.owner = owner;
    this.repo = repo;
  }

  static IssueClient create(final GitHubClient github, final String owner, final String repo) {
    return new IssueClient(github, owner, repo);
  }

  public String getOwner() {
    return owner;
  }

  public String getRepo() {
    return repo;
  }

  /**
   * List repository issues.
   *
   * @return issues
   */
  public Iterator<AsyncPage<Issue>> listIssues() {
    return listIssues(null);
  }

  /**
   * List repository issues.
   *
   * @param parameters request parameters
   * @return issues
   */
  public Iterator<AsyncPage<Issue>> listIssues(final IssueParameters parameters) {
    final String serial = parameters != null ? parameters.serialize() : null;
    final String path = String.format(ISSUES_URI_LIST_TEMPLATE, owner, repo) + (Strings.isNullOrEmpty(serial) ? "" : "?" + serial);
    log.info("Fetching issues from " + path);
    return new GithubPageIterator<>(new GithubPage<>(
            github, path, LIST_ISSUE_TYPE_REFERENCE, Collections.singletonMap(HttpHeaders.ACCEPT, "application/vnd.github.full+json"))
    );
  }

  /**
   * List repository comments.
   *
   * @return comments
   */
  public Iterator<AsyncPage<Comment>> listComments() {
    return listComments(String.format(COMMENTS_URI_TEMPLATE, owner, repo));
  }

  /**
   * List given issue number comments.
   *
   * @param number issue number
   * @return comments
   */
  public Iterator<AsyncPage<Comment>> listComments(final long number) {
    return listComments(String.format(COMMENTS_URI_NUMBER_TEMPLATE, owner, repo, number));
  }

  /**
   * Get a specific comment.
   *
   * @param id comment id
   * @return a comment
   */
  public CompletableFuture<Comment> getComment(final int id) {
    final String path = String.format(COMMENTS_URI_ID_TEMPLATE, owner, repo, id);
    log.info("Fetching issue comments from " + path);
    return github.request(path, Comment.class, Collections.singletonMap(HttpHeaders.ACCEPT, "application/vnd.github.full+json"));
  }

  /**
   * Create a comment for a given issue number.
   *
   * @param number issue number
   * @param body comment content
   * @return the Comment that was just created
   */
  public CompletableFuture<Comment> createComment(final long number, final String body) {
    final String path = String.format(COMMENTS_URI_NUMBER_TEMPLATE, owner, repo, number);
    final String requestBody = github.json().toJsonUnchecked(ImmutableMap.of("body", body));
    return github.post(path, requestBody, Comment.class);
  }

  /**
   * Edit a specific comment.
   *
   * @param id comment id
   * @param body new comment content
   */
  public CompletableFuture<Void> editComment(final int id, final String body) {
    final String path = String.format(COMMENTS_URI_ID_TEMPLATE, owner, repo, id);
    return github
        .patch(path, github.json().toJsonUnchecked(ImmutableMap.of("body", body)))
        .thenAccept(IGNORE_RESPONSE_CONSUMER);
  }

  /**
   * Delete a comment.
   *
   * @param id comment id
   */
  public CompletableFuture<Void> deleteComment(final int id) {
    return github
        .delete(String.format(COMMENTS_URI_ID_TEMPLATE, owner, repo, id))
        .thenAccept(IGNORE_RESPONSE_CONSUMER);
  }

  private Iterator<AsyncPage<Comment>> listComments(final String path) {
    return new GithubPageIterator<>(new GithubPage<>(
            github, path, LIST_COMMENT_TYPE_REFERENCE, Collections.singletonMap(HttpHeaders.ACCEPT, "application/vnd.github.full+json"))
    );
  }

  /**
   * Get a specific issue.
   *
   * @param number issue number
   * @return an issue
   */
  public CompletableFuture<Issue> getIssue(final long number) {
    final String path = String.format(ISSUES_URI_NUMBER_TEMPLATE, owner, repo, number);
    log.info("Fetching issue from " + path);
    /*
        application/vnd.github.full+json: Returns raw, text, and HTML representations. Response will include body, body_text, and body_html.
     */
    return github.request(path, Issue.class, Collections.singletonMap(HttpHeaders.ACCEPT, "application/vnd.github.full+json"));
  }

  /**
   * Fetches an attachment file linked to an issue.
   *
   * @param url absolute url for the file, most often parsed from the body_html content
   * @param personalAccessToken personal access token to use to fetch, always mandatory for atleast private repositories
   * @return attachment file
   */
  public CompletableFuture<AttachmentFile> getIssueAttachment(final String url, final String personalAccessToken) {
    log.info("Fetching attachment from " + url);
    if (!url.toLowerCase().startsWith("https://") && !url.toLowerCase().startsWith("http://")) {
      throw new IllegalArgumentException("must use absolute urls for issue attachments");
    }
    Map<String, String> headers = new HashMap<>();
    if (personalAccessToken != null) {
      headers.put(HttpHeaders.AUTHORIZATION, "token " + personalAccessToken);
    }
    headers.put(HttpHeaders.ACCEPT, "application/octet-stream");
    return github.request(url, headers).thenApply(response -> {
        try (ResponseBody body = response.body()) {
            if (body == null) {
              throw new IOException("No response body.");
            }
            return ImmutableAttachmentFile.builder()
                    .contentLength(body.contentLength())
                    .charset(body.contentType() != null ? body.contentType().charset() : null)
                    .contentType(body.contentType() != null ? body.contentType().toString() : "application/octet-stream")
                    .bytes(body.bytes())
                    .build();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed getting response body for: " + response, e);
        }
    });
  }

}
