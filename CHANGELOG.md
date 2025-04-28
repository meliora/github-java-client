# Changelog

All notable changes to this fork will be documented in this file.

## 0.3.7-SNAPSHOT

- remove shading: makes OkHttpClient configurable
- fix Integer id() declarations "Numeric value out of range of int" JsonMappingExceptions
- REST: Extend IssueClient to include Issue related stuff
- impl pretty printing toString method for immutables

- add support for passing extra headers in GitHubPage
- use correct mediatype in issueclient to fetch body, body_html and body_text for issues and comments
- add User.email, User.name, Issue.state_reason, Issue.closed_by
- add body_html and body_text to Issue and Comment, make int id()'s Long in various places

- GitHubClient: add capability to call absolute urls with !json content, make it possible to override AUTHORIZATION header via extraHeaders
- add IssueClient.getIssueAttachment to fetch a absolutely (issue or comment) linked attachment file with possible personal access token override (adds AttachmentFile.java)

- move labels() from PullRequest to PullRequestItem

- Installation: annotate fields nullable as minimal variants are encountered
- impl RateLimitException to the client
- impl WebhookRequestBody to support incoming hooks

- impl oauth.UserAccessTokensClient for fetching UserAccessTokens with oauth codes (needed for app install flow)
- impl WrappedGithubPage to support paging when the array is wrapped in an attribute
- impl UserClient.getInstallationsWithUserAccessToken

- impl GraphQLClient: a crude implementation to run graphql queries against repository data with untyped response structures

- use pagination in PullRequestClient.list

- use int64 instead of int32 in issue & pull request numbers