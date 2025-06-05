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

package com.spotify.github.v3.issues.requests;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.spotify.github.GithubStyle;
import com.spotify.github.Parameters;

import java.time.ZonedDateTime;
import java.util.Optional;

import org.immutables.value.Value;

/** Issue retrieval parameters resource */
@Value.Immutable
@GithubStyle
@JsonSerialize(as = ImmutableIssueParameters.class)
@JsonDeserialize(as = ImmutableIssueParameters.class)
public interface IssueParameters extends Parameters {
    /**
     * If an integer is passed, it should refer to a milestone by its number field. If the string * is passed,
     * issues with any milestone are accepted. If the string none is passed, issues without milestones are returned.
     */
    Optional<String> milestone();

    /**
     * Can be one of: open, closed, all
     * Default: open
     */
    Optional<String> state();

    /**
     * Can be the name of a user. Pass in none for issues with no assigned user, and * for issues assigned to any user.
     */
    Optional<String> assignee();

    /**
     * Can be the name of an issue type. If the string * is passed, issues with any type are accepted.
     * If the string none is passed, issues without type are returned.
     */
    Optional<String> type();

    /**
     * The user that created the issue.
     */
    Optional<String> creator();

    /**
     * A user that's mentioned in the issue.
     */
    Optional<String> mentioned();

    /**
     * A list of comma separated label names. Example: bug,ui,@high
     */
    Optional<String> labels();

    /**
     * What to sort results by.
     * Default: created
     * Can be one of: created, updated, comments
     */
    Optional<String> sort();

    /**
     * The direction to sort the results by.
     * Default: desc
     * Can be one of: asc, desc
     */
    Optional<String> direction();

    /**
     * Only show results that were last updated after the given time.
     * This is a timestamp in ISO 8601 format: YYYY-MM-DDTHH:MM:SSZ.
     */
    Optional<ZonedDateTime> since();
    
    /**
     * Results per page (max 100)
     */
    @SuppressWarnings("checkstyle:methodname")
    Optional<Integer> per_page();

    /**
     * Page number of the results to fetch.
     */
    Optional<Integer> page();
}
