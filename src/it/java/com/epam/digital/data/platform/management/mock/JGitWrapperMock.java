/*
 * Copyright 2022 EPAM Systems.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.epam.digital.data.platform.management.mock;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

import com.epam.digital.data.platform.management.config.GerritPropertiesConfig;
import com.epam.digital.data.platform.management.dto.TestFormDetailsShort;
import com.epam.digital.data.platform.management.model.dto.ChangeInfoDto;
import com.epam.digital.data.platform.management.service.impl.JGitWrapper;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.CommitCommand;
import org.eclipse.jgit.api.FetchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.api.PullCommand;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.RemoteAddCommand;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.StatusCommand;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectLoader.SmallObject;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class JGitWrapperMock {

  private static final String FORM_CONTENT = "{\n" +
      "  \"type\": \"form\",\n" +
      "  \"components\": [\n" +
      "   ],\n" +
      "  \"title\": \"title\",\n" +
      "  \"path\": \"add-fizfactors1\",\n" +
      "  \"name\": \"add-fizfactors1\",\n" +
      "  \"display\": \"form\",\n" +
      "  \"submissionAccess\": []\n" +
      "}";

  private final GerritPropertiesConfig gerritPropertiesConfig;

  @Getter
  private final JGitWrapper jGitWrapper = Mockito.mock(JGitWrapper.class);
  @Getter
  private Git git;
  @Getter
  private Repository repository;
  private CheckoutCommand checkoutCommand;
  private RevTree revTree;
  private TreeWalk treeWalk;
  private TreeWalk dirWalk;
  private ObjectLoader loader;

  @SneakyThrows
  public void init() {
    final var cloneCommand = createCloneCommand();
    Mockito.when(jGitWrapper.cloneRepository()).thenReturn(cloneCommand);

    git = Mockito.mock(Git.class);
    repository = Mockito.mock(Repository.class);
    revTree = Mockito.mock(RevTree.class);
    treeWalk = Mockito.mock(TreeWalk.class);
    dirWalk = Mockito.mock(TreeWalk.class);
    loader = Mockito.mock(ObjectLoader.class);

    ObjectId objectId = Mockito.mock(ObjectId.class);
    Mockito.when(git.getRepository()).thenReturn(repository);
    Mockito.when(jGitWrapper.open(any())).thenReturn(git);
    Mockito.when(jGitWrapper.getRevTree(repository)).thenReturn(revTree);
    Mockito.when(treeWalk.getObjectId(0)).thenReturn(objectId);

    Mockito.when(repository.open(any())).thenReturn(loader);
    Mockito.when(loader.getCachedBytes()).thenReturn(new byte[]{});
  }

  @SneakyThrows
  public Git mockCloneCommand(String versionName) {
    final var generalCloneCommand = jGitWrapper.cloneRepository();

    final var directory = new File(gerritPropertiesConfig.getRepositoryDirectory(), versionName);
    final var callResult = Mockito.mock(Git.class);

    final var directoryCloneCommand = createCloneCommand();
    Mockito.when(generalCloneCommand.setDirectory(directory)).thenReturn(directoryCloneCommand);
    Mockito.doAnswer(invocationOnMock -> {
      log.info("Called clone command for {} version", versionName);
      Assertions.assertThat(directory.mkdirs()).isTrue();
      Assertions.assertThat(new File(directory, "forms").mkdir()).isTrue();
      Assertions.assertThat(new File(directory, "bpmn").mkdir()).isTrue();
      return callResult;
    }).when(directoryCloneCommand).call();
    return callResult;
  }

  @SneakyThrows
  public void mockCommitCommand() {
    CommitCommand commitCommand = Mockito.mock(CommitCommand.class);
    RevCommit revCommit = Mockito.mock(RevCommit.class);
    Mockito.when(git.commit()).thenReturn(commitCommand);
    Mockito.when(commitCommand.setMessage(anyString())).thenReturn(commitCommand);
    Mockito.when(commitCommand.setAmend(true)).thenReturn(commitCommand);
    Mockito.when(commitCommand.call()).thenReturn(revCommit);
  }

  @SneakyThrows
  public void mockPullCommand() {
    PullCommand pullCommand = Mockito.mock(PullCommand.class);
    var credentialsProvider = new UsernamePasswordCredentialsProvider(
        gerritPropertiesConfig.getUser(), gerritPropertiesConfig.getPassword());
    Mockito.when(pullCommand.setCredentialsProvider(Mockito.refEq(credentialsProvider)))
        .thenReturn(pullCommand);
    Mockito.when(pullCommand.setRebase(true)).thenReturn(pullCommand);
    Mockito.when(git.pull()).thenReturn(pullCommand);
  }

  @SneakyThrows
  public Git mockOpenGit(String versionName) {
    final var directory = new File(gerritPropertiesConfig.getRepositoryDirectory(), versionName);

    final var git = Mockito.mock(Git.class);
    Mockito.when(jGitWrapper.open(directory)).thenReturn(git);

    final var repository = Mockito.mock(Repository.class);
    Mockito.when(git.getRepository()).thenReturn(repository);

    return git;
  }

  @SneakyThrows
  public FetchCommand mockFetchCommand(@NonNull final Git git,
      @NonNull final ChangeInfoDto changeInfoDto) {
    final var fetchCommand = createFetchCommand();
    Mockito.when(fetchCommand.setRefSpecs(changeInfoDto.getRefs())).thenReturn(fetchCommand);
    Mockito.when(git.fetch()).thenReturn(fetchCommand);
    return fetchCommand;
  }

  @SneakyThrows
  public void mockFetchCommand(ChangeInfoDto changeInfoDto) {
    FetchCommand fetchCommand = Mockito.mock(FetchCommand.class);
    Mockito.when(fetchCommand.setCredentialsProvider(any())).thenReturn(fetchCommand);
    Mockito.when(fetchCommand.setRefSpecs(changeInfoDto.getRefs()))
        .thenReturn(fetchCommand);
    Mockito.when(git.fetch()).thenReturn(fetchCommand);
  }

  @SneakyThrows
  public void mockLogCommand() {
    LogCommand logCommand = Mockito.mock(LogCommand.class);
    Mockito.when(git.log()).thenReturn(logCommand);
    var revCommit = Mockito.mock(RevCommit.class);
    var revCommit2 = Mockito.mock(RevCommit.class);
    Mockito.when(logCommand.addPath(anyString())).thenReturn(logCommand);
    Mockito.when(logCommand.call()).thenReturn(List.of(revCommit, revCommit2));
  }

  @SneakyThrows
  public void mockAddCommand() {
    AddCommand addCommand = Mockito.mock(AddCommand.class);
    Mockito.when(git.add()).thenReturn(addCommand);
    Mockito.when(addCommand.addFilepattern(anyString())).thenReturn(addCommand);
  }

  @SneakyThrows
  public void mockRemoteAddCommand() {
    RemoteAddCommand remoteAddCommand = Mockito.mock(RemoteAddCommand.class);
    Mockito.when(git.remoteAdd()).thenReturn(remoteAddCommand);
    Mockito.when(remoteAddCommand.setUri(any(URIish.class))).thenReturn(remoteAddCommand);
  }

  @SneakyThrows
  public void mockStatusCommand() {
    StatusCommand statusCommand = Mockito.mock(StatusCommand.class);
    Status status = Mockito.mock(Status.class);
    Mockito.when(git.status()).thenReturn(statusCommand);
    Mockito.when(statusCommand.call()).thenReturn(status);
  }

  public void mockPushCommand() {
    PushCommand pushCommand = Mockito.mock(PushCommand.class);
    Mockito.when(git.push()).thenReturn(pushCommand);
  }

  @SneakyThrows
  public CheckoutCommand mockCheckoutCommand(@NonNull final Git git) {
    final var checkoutCommand = createCheckoutCommand();
    Mockito.when(git.checkout()).thenReturn(checkoutCommand);
    return checkoutCommand;
  }

  @SneakyThrows
  public void mockCheckoutCommand() {
    checkoutCommand = Mockito.mock(CheckoutCommand.class);
    Mockito.when(checkoutCommand.setName("refs/heads/" + gerritPropertiesConfig.getHeadBranch()))
        .thenReturn(checkoutCommand);
    Mockito.when(checkoutCommand.setName("FETCH_HEAD")).thenReturn(checkoutCommand);
    Mockito.when(git.checkout()).thenReturn(checkoutCommand);
  }

  @SneakyThrows
  public void mockGetFileContent(@NonNull final Git git, @NonNull final String path,
      @NonNull final String content) {
    final var repository = git.getRepository();

    final var treeWalk = Mockito.mock(TreeWalk.class);
    Mockito.when(jGitWrapper.getTreeWalk(eq(repository))).thenReturn(treeWalk);

    Mockito.doAnswer(invocation -> {
      Mockito.when(treeWalk.next()).thenReturn(true).thenReturn(false);

      final var objectId = Mockito.mock(ObjectId.class);
      Mockito.when(treeWalk.getObjectId(0)).thenReturn(objectId).thenReturn(null);

      final var loader = Mockito.mock(ObjectLoader.class);
      Mockito.when(repository.open(objectId)).thenReturn(loader).thenReturn(null);

      final var bytes = content.getBytes(StandardCharsets.UTF_8);
      Mockito.when(loader.getCachedBytes()).thenReturn(bytes);
      Mockito.when(loader.getBytes()).thenReturn(bytes).thenReturn(null);
      return null;
    }).when(treeWalk).setFilter(Mockito.refEq(PathFilter.create(path)));
  }

  @SneakyThrows
  public void mockGetBusinessProcess(String content) {
    Mockito.when(jGitWrapper.getTreeWalk(eq(repository))).thenReturn(treeWalk);
    Mockito.when(treeWalk.next()).thenReturn(true);
    Mockito.when(loader.getBytes()).thenReturn(content.getBytes(StandardCharsets.UTF_8));
  }

  @SneakyThrows
  public void mockGetForm(TestFormDetailsShort formDetails) {
    Mockito.when(jGitWrapper.getTreeWalk(eq(repository))).thenReturn(treeWalk);
    Mockito.when(treeWalk.next()).thenReturn(true);
    Mockito.when(loader.getBytes())
        .thenReturn(formDetails.getContent().getBytes(StandardCharsets.UTF_8));
  }

  @SneakyThrows
  public void mockGetSettings(String settings, String globalVars) {
    Mockito.when(jGitWrapper.getTreeWalk(eq(repository))).thenReturn(treeWalk);
    Mockito.when(treeWalk.next()).thenReturn(true);
    Mockito.when(loader.getBytes())
        .thenReturn(globalVars.getBytes(StandardCharsets.UTF_8),
            settings.getBytes(StandardCharsets.UTF_8));
  }

  @SneakyThrows
  public void mockGetFormsList(List<TestFormDetailsShort> list) {
    Mockito.when(jGitWrapper.getTreeWalk(eq(repository), anyString(), eq(revTree)))
        .thenReturn(treeWalk);
    Mockito.when(jGitWrapper.getTreeWalk(repository)).thenReturn(dirWalk);
    Mockito.when(dirWalk.next()).thenAnswer(
        new Answer<Boolean>() {
          Iterator<TestFormDetailsShort> iterator = list.iterator();

          @Override
          public Boolean answer(InvocationOnMock invocation) {
            if (iterator.hasNext()) {
              var next = iterator.next();
              Mockito.when(dirWalk.getPathString()).thenReturn("forms/" + next.getName());
              Mockito.when(loader.getBytes())
                  .thenReturn(next.getContent().getBytes(StandardCharsets.UTF_8));
              return true;
            } else {
              iterator = list.iterator();
              return false;
            }
          }
        });
  }

  @SneakyThrows
  public void mockGetBusinessProcessList(Map<String, String> processContentMap) {
    Mockito.when(jGitWrapper.getTreeWalk(eq(repository), anyString(), eq(revTree)))
        .thenReturn(treeWalk);
    Mockito.when(jGitWrapper.getTreeWalk(repository)).thenReturn(dirWalk);

    Mockito.when(dirWalk.next()).thenAnswer(
        new Answer<Boolean>() {
          Iterator<Map.Entry<String, String>> iterator = processContentMap.entrySet().iterator();

          @Override
          public Boolean answer(InvocationOnMock invocation) {
            if (iterator.hasNext()) {
              var next = iterator.next();
              Mockito.when(dirWalk.getPathString()).thenReturn("/bpmn/" + next.getKey());
              Mockito.when(loader.getBytes())
                  .thenReturn(next.getValue().getBytes(StandardCharsets.UTF_8));
              return true;
            } else {
              iterator = processContentMap.entrySet().iterator();
              return false;
            }
          }
        });
  }

  @SneakyThrows
  public void mockGetFileInPath() {
    var directory = new File(gerritPropertiesConfig.getRepositoryDirectory(),
        gerritPropertiesConfig.getHeadBranch());
    var git = Mockito.mock(Git.class);
    Mockito.when(jGitWrapper.open(directory)).thenReturn(git);
    var repository = Mockito.mock(Repository.class);
    Mockito.when(git.getRepository()).thenReturn(repository);
    var revTree = Mockito.mock(RevTree.class);
    Mockito.when(jGitWrapper.getRevTree(repository)).thenReturn(revTree);
    var treeWalk = Mockito.mock(TreeWalk.class);
    Mockito.when(jGitWrapper.getTreeWalk(repository, "forms", revTree)).thenReturn(treeWalk);
    Mockito.when(jGitWrapper.getTreeWalk(repository)).thenReturn(treeWalk);
    Mockito.when(treeWalk.next()).thenReturn(true).thenReturn(false)
        .thenReturn(true).thenReturn(false);
    Mockito.when(treeWalk.getPathString()).thenReturn("someFile");
    var logCommand = Mockito.mock(LogCommand.class);
    Mockito.when(git.log()).thenReturn(logCommand);
    var revCommit = Mockito.mock(RevCommit.class);
    var revCommit2 = Mockito.mock(RevCommit.class);
    Mockito.when(logCommand.call()).thenReturn(List.of(revCommit, revCommit2));
    SmallObject smallObject = new SmallObject(1, FORM_CONTENT.getBytes(StandardCharsets.UTF_8));
    Mockito.when(repository.open(any())).thenReturn(smallObject);
  }

  public void resetAll() {
    Stream.of(jGitWrapper, git, repository, checkoutCommand, revTree, treeWalk,
            dirWalk, loader)
        .filter(Objects::nonNull)
        .forEach(Mockito::reset);
  }

  private String getRepoUri() {
    final var url = gerritPropertiesConfig.getUrl();
    final var repo = gerritPropertiesConfig.getRepository();
    return String.format("%s/%s", url, repo);
  }

  private CredentialsProvider getCredentialsProvider() {
    final var user = gerritPropertiesConfig.getUser();
    final var password = gerritPropertiesConfig.getPassword();
    return new UsernamePasswordCredentialsProvider(user, password);
  }

  private CloneCommand createCloneCommand() {
    final var repoURI = getRepoUri();
    final var credentialsProvider = getCredentialsProvider();

    final var cloneCommand = Mockito.mock(CloneCommand.class);
    Mockito.lenient().when(cloneCommand.setURI(repoURI)).thenReturn(cloneCommand);
    Mockito.lenient().when(cloneCommand.setCredentialsProvider(Mockito.refEq(credentialsProvider)))
        .thenReturn(cloneCommand);
    Mockito.lenient().when(cloneCommand.setCloneAllBranches(true)).thenReturn(cloneCommand);
    return cloneCommand;
  }

  private FetchCommand createFetchCommand() {
    final var credentialsProvider = getCredentialsProvider();

    final var fetchCommand = Mockito.mock(FetchCommand.class);
    Mockito.lenient().when(fetchCommand.setCredentialsProvider(Mockito.refEq(credentialsProvider)))
        .thenReturn(fetchCommand);
    return fetchCommand;
  }

  private CheckoutCommand createCheckoutCommand() {
    final var checkoutCommand = Mockito.mock(CheckoutCommand.class);
    Mockito.lenient().when(checkoutCommand.setName(Constants.FETCH_HEAD))
        .thenReturn(checkoutCommand);
    return checkoutCommand;
  }
}

