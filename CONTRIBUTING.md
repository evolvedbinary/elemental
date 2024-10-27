# Contributing to Elemental
We welcome everyone to contribute to Elemental. We will consider each individual contribution separately on its own merits.
We strongly suggest that you join the [Elemental Slack Workspace](https://join.slack.com/t/elemental-xyz/shared_invite/zt-3290ginoh-lWocaoR3UMw7jghfrt~kFA), so that you can collaborate with the Elemental community. It is often valuable to discuss a potential contribution before undertaking any work.

We follow a *Hub and Spoke* like development model; therefore you should fork our Elemental repository, work on branches within your own fork, and then send pull requests for your branches to our GitHub repository.

## Branch Naming
Elemental uses a simple Git branching model for development.

The names of each branch should reflect their purpose, the following branches may be of interest:
* `main` - the main line of development for the next version.
* `gold` - reflects the `tag` of the last released version.

There are also branches that enable us to backport hot-fixes and features to older major versions, so that we might release small updates occasionally.
* `develop-6.x.x` - development of the 6.x.x version line of Elemental.

When contributing to Elemental you should branch our `main` (or for backports the `develop-6.x.x`) branch. Your branch should be named in one of two ways:

* `feature/<name-of-my-feature>`
    This naming convention should be used when contributing new features. For example `feature/xquery31-sliding-window`
* `hotfix/<name-of-my-fix>`
    This naming convention should be used when contributing bug fixes. For example `hotfix/memory-leak-xquery-context`

Additionally, if you are back-porting a feature or bug fix to a previous version of Elemental, you should prefix your branch name with a `V.x.x/` where `V` is the major version number, for example: `6.x.x/feature/xquery31-sliding-window`.

## Code Formatting
All new Java code is expected to be formatted inline with the [IntelliJ Default Style for Java code](https://www.jetbrains.com/help/idea/configuring-code-style.html#configure-code-style-schemes). 

## Commit Messages
Commits by developers *should* follow the Git [Commit Guidelines](https://git-scm.com/book/en/v2/Distributed-Git-Contributing-to-a-Project#_commit_guidelines). In addition, the summary line of the commit message *must* be prefixed with a label from our controlled list that helps us to better understand the commit and also to generate Change Logs.

### Commit Labels
Our controlled list of commit labels that should be prefixed to each commit summary is:

* `[feature]`
    This should be used when a commit adds a new feature.
* `[bugfix]`
    This should be used when a commit addresses a bug or issue.
* `[refactor]`
    This should be used when a commit is simply refactoring existing code.
* `[optimize]`
    This should be used when a commit is refactoring existing code to provide a performance and/or memory optimization.
* `[ignore]`
    This should be used when code is cleaned up by automated means, e.g. reformatting.
* `[doc]`
    This should be used for documentation.
* `[test]`
    This should be used when a commit solely contains changes to existing tests or adds further tests.
* `[ci]`
    This should be used when a commit solely makes changes to CI configuration.

Finally, any commit that addresses a GitHub issue, should have an additional line in its commit after the summary and before any fuller explanation that takes this form:
```
Closes https://github.com/evolvedbinary/elemental/issues/<github-issue-number>
```

### Commit Message Example
For example, here is a correctly formatted commit message:

```
[bugfix] Fix relative paths in EXPath classpath.txt files.

Closes https://github.com/eevolvedbinary/elemental/issues/4901
We now store the path of Jar files in each EXPath Package's `classpath.txt` file relative to the package's `content/` sub-folder.
```

## Pull Requests and Code Review
Pull requests will be reviewed and tested before they're merged. 
Worth restating, is the one "golden rule", even within the Core Team, **no developer should merge their own pull request**. This simple-but-important rule ensures that at least two people have considered the change.

Some things to keep in mind when trying to craft an acceptable Pull Request are:
-   **Atomic changes.** We prefer changes that have a single purpose to be grouped into their own individual commits. Each commit should have a single purpose.
-   **Only change what you need to.** If you must reformat code, keep it in a separate commit to any syntax or functionality changes.
-   **Test.** If you fix something prove it, write a test that illustrates the issue and validate the test. If you add a new feature it also requires tests, so that we can understand its intent and try to avoid regressions in future as much as possible.
-   **Run the full test suite.** We don't accept code that causes regressions. This will also be checked in CI.


## Security Issues
***If you find a security vulnerability, do NOT open an issue.***
See the [Security Policy](SECURITY.md).

## Versions and Releases
Elemental follows a Semantic Versioning scheme, this is further documented in the [Versioning Scheme and Release Process](VERSIONING_AND_RELEASING.md) document.

### Porting during Release Candidate development phase
When developing one or more stable release lines and/or a release-candidate in parallel, this may require commits to be both back- and forward-ported until the release-candidate has become the next stable release.

In these circumstances pull request(s) for the same purpose may be opened multiple times against different `develop-`* branches.

#### Backport
Assuming the stable is `6.x.x` and the RC is `7.x.x`:
1. Create a second branch `6.x.x/feature/<name-of-my-feature>` based off of our `develop-6.x.x` branch
2. [Cherry Pick](https://git-scm.com/docs/git-cherry-pick) your commits from `feature/<name-of-my-feature>` into `6.x.x/feature/<name-of-my-feature>`
3. Open a second PR from `6.x.x/feature/<name-of-my-feature>` against `develop-6.x.x` mentioning the original PR in the commit message

### Forward-port
Works just as backport but with your `feature/<name-of-my-feature>` branch based off of our `main` branch

## Syncing a Fork
Your fork will eventually become out of sync with the upstream repo as others contribute. To pull upstream changes into your fork, you have two options:

1.  [Merging](https://help.github.com/articles/syncing-a-fork).
2.  Rebasing.

Rebasing leads to a cleaner revision history which is much easier to follow and is our preferred approach. However, `git rebase` is a very sharp tool and must be used with care. For those new to rebase, we would suggest having a backup of your local (and possibly remote) git repos before continuing. Read on to learn how to sync using rebase.

#### Rebase Example
Let's say that you have a fork of Elemental's Git repo, and you have been working in your feature branch called `feature/my-feature` for some time, you are happy with how your work is progressing, but you want to sync so that your changes are based on the latest and greatest changes from Elemental. The way to do this using `git rebase` is as follows:

1.  If you have any un-committed changes you need to stash them using: `git stash save "changes before rebase"`.

2.  If you have not added Elemental's GitHub as an upstream remote, you need to do so once by running `git remote add upstream https://github.com/evolvedbinary/elemental.git`. You can view your existing remotes, by running `git remote -v`.

3.  You need to fetch the latest changes from Elemental's GitHub: `git fetch upstream`. This will not yet change your local branches in any way.

4.  You should first sync your `main` branch with Elemental's `main` branch. As you always work in feature branches, this should a simple fast-forward by running: `git checkout main` and then `git rebase upstream/main`.
    1.  If all goes well in (4) then you can push your `main` branch to your remote server (e.g. GitHub) with `git push origin main`.

5.  You can then replay your work in your feature branch `feature/my-feature` atop the latest changes from the `main` branch by running: `git checkout feature/my-feature` and then `git rebase main`.
    1.  Should you encounter any conflicts during (5) you can resolve them using `git mergetool` and then `git rebase --continue`.
    2.  If all goes well in (5), and take care to check your history is correct with `git log`, then you can force push your `feature/my-feature` branch to your remote server (e.g. GitHub) with `git push -f origin feature/my-feature`. *NOTE* the reason you need to use the `-f` to force the push is because the commit ids of your revisions will have changed after the rebase.

Note that it is worth syncing your branches that you are working on relatively frequently to prevent any large rebase processes which could lead to you having to resolve many conflicting changes where your branch has diverged over a long period of time.
