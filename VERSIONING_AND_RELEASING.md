# Elemental Versioning Scheme and Release Process

![Elemental logo](https://www.elemental.xyz/images/elemental-logo-horizontal-light.png)

## Overview
This document describes the Versioning Scheme and Release Process for Elemental. These two topics are tightly connected, and so both are covered in this one document.

*   The Versioning Scheme describes how the source code and releases are named. Version numbers unambiguously inform users and developers about the significance of the release and order relative to past and future versions.

*   The Release Process describes how the Release Manager (the person who orchestrates a release) should take a `snapshot (tag)` of source code, apply the Versioning Scheme, assemble it, and publish the resulting products. The goal is to have a clear procedure for altering the version number to mark transitions in phases of development leading up to each release, and to ensure that releases are consistently sourced from a specific point in the project repository's history.

### Motivation

This approach was chosen to try and facilitate more rapid releases, with the goal of getting new features and bug fixes out to the community without sacrificing quality or stability. Critical to the success of this effort is achieving a common understanding about version numbers and managing version changes during releases.
The versioning scheme uses the popular Semantic Versioning scheme, in which each number here reflects major, minor, and patch versions. This single version-related property will bring clarity and semantic precision to releases. The Semantic Versioning scheme allows the team to label development versions as snapshots or release candidates, and to release these and nightly builds with clear version numbers.
The new versioning scheme ensures the names of new versions of delivered to the community are precise and reliable. Removing versioning ambiguities and clarifying release practices facilitates a rapid cycle of development and release.

## Versioning Scheme

We follow a widely-used, semantically precise versioning scheme called [Semantic Versioning](http://semver.org/) (specifically [version 2.0.0](https://github.com/mojombo/semver/tree/v2.0.0)) of this scheme. For a complete introduction to Semantic Versioning, please consult the documentation. Here, we summarize how the principles of Semantic Versioning are applied.

### Product Releases

For product releases (also called stable or final releases), a 3-component Semantic Versioning version number is used: "`MAJOR`**.**`MINOR`**.**`PATCH`". When a new version is released, its version number is incremented according to the following criteria:

1. `MAJOR` versions contain incompatible API changes, including changes to the on-disk format of the database;
2. `MINOR` versions add functionality or deprecate API functions, without breaking backward compatibility; and
3. `PATCH` versions contain only backwards-compatible bug fixes.

(Any public or protected methods of public or protected classes are considered API)

For example, the 8th major version of Elemental would have the Semantic Version number `8.0.0`. A new release following this including new features would be version `8.1.0`. A bugfix-only release following that would be version `8.1.1`.

### Pre-Releases

For pre-releases, such as [release candidates](https://en.wikipedia.org/wiki/Software_release_life_cycle#Release_candidate) or [snapshots](https://docs.oracle.com/middleware/1212/core/MAVEN/maven_version.htm#MAVEN401), a 4-component Semantic Versioning version number is used: "`MAJOR`**.**`MINOR`**.**`PATCH`**-**`PRERELEASE`. We follow Semantic Versioning's definitions for the `PRERELEASE` label scheme:

*   `PRERELEASE` is a series of dot separated identifiers, each identifier must use only the following ASCII characters `[0-9A-Za-z-]` and must not be empty.

*   The presence of `PRERELEASE` indicates that the version is pre-release and not yet considered stable. Product releases do not have `PRERELEASE`.

*   Given two versions in which `MAJOR`, `MINOR`, and `PATCH` are equal, the version with a `PRERELEASE` has lower precedence than one without it. The following rules hold true in terms of version number preference:

    *   `8.0.0` > `8.0.0-SNAPSHOT`
    *   `8.0.0` > `8.0.0-RC2`
    *   `8.0.0-RC2` > `8.0.0-RC1`
    *   `8.0.0-RC1` > `7.1.0`
    *   `8.0.0-SNAPSHOT` > `7.1.0`
    *   `7.2.0-SNAPSHOT` > `7.1.0`

We use only two clearly defined forms of `PRERELEASE` label:

*   `RCx` is used for release candidates. The `x` should be replaced with the iteration of the release candidate, for example `8.0.0-RC1` for the first release candidate of version 8, and `8.0.0-RC2` for the second release candidate of version 8. While not all releases are necessarily preceded by a release candidate (which are feature complete and considered ready for release), we may opt to issue one or more release candidates in order to gather feedback from testing by early adopters.

*   `SNAPSHOT` is used for point-in-time builds. These products are typically not published or distributed, but used only for local testing by developers or by the nightly-build system.

### Nightly Builds

A nightly build is similar to a snapshot, except it is automatically built from the latest source code and released once daily. To help distinguish between one day's nightly build and the next's, a 5-component Semantic Versioning version number is used for nightly builds' filenames: "`MAJOR`**.**`MINOR`**.**`PATCH`**-**`PRERELEASE`**+**`BUILD`. We follow Semantic Versioning's definitions for the `BUILD` label scheme:

*   `BUILD` is a series of dot separated identifiers, each identifier must use only ASCII alphanumerics and hyphen [0-9A-Za-z-] and must be empty. Build metadata SHOULD be ignored when determining version precedence.

*   The presence of `BUILD` indicates that the version is pre-release and not yet considered stable. Product releases do not have `BUILD`.

We add a further constraint and modify the precedence for the `BUILD` label:

*   The `BUILD` label is a UTC timezone timestamp, in the format `YYYYMMDDHHmmSS` (as would be given by the UNIX command `date +%Y%m%d%H%M%S`).

*   The precedence of the `BUILD` label, may be numerically compared by timestamp, e.g. `20240527142409 > 20240504000001`.

For example, the macOS disk image for the build from the SNAPSHOT pre-release version of 8.0.0 on May 7, 2024 at 21:37:22 UTC would be named:

    * elemental-8.0.0-SNAPSHOT+20240507213722.dmg

It is trivial for a developer to relate a timestamp back to a Git hash (by using the command `git rev-list -1 --before="$DATE" main`), should they need to do so.

### Where the version number is stored

The version number is stored in the `exist-parent/pom.xml` file, in a single property, `<version>`. The Semantic Versioning number `8.0.0-SNAPSHOT` would be stored as follows:
```xml
<version>8.0.0-SNAPSHOT</version>
```

That version number is also copied into the `META-INF/MANIFEST.MF` file of any Jar packages that are built, using the standard manifest attributes: `Specification-Version` and `Implementation-Version`.

## Release Process

This section details concrete steps for creating and publishing product releases. Each section here assumes you are starting with a clean Git checkout of the `main` branch from [https://github.com/evolvedbinary/elemental.git](https://github.com/evolvedbinary/elemental.git).

### Preparing a Product Release

Once development on a new stable version is complete, the following steps will prepare the version for release. For purposes of illustration, we will assume we are preparing the stable release of version 8.0.0.
You will require a system with:
* macOS
* JDK 8
* Maven 3.6.0+
* Python 3 with Pip
* Docker
* GnuPG
* A GPG key (for signing release artifacts)
* A Java KeyStore with key (for signing IzPack Installer)
* A valid Apple Developer Certificate (for signing Mac DMG)
* A GitHub account and Personal Access Token (https://github.com/settings/tokens) with permission to publish GitHub releases to the Elemental repository.

1. You will need login credentials for the Elemental organisation on:
    1. Sonatype Portal for Maven Central - https://central.sonatype.com/publishing/deployments
    2. DockerHub - https://cloud.docker.com/orgs/elemental/

    Your credentials for these should be stored securely in the `<servers>` section on your machine in your local `~/.m2/settings.xml` file, e.g.:
    ```xml
    <settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd">

        <servers>

            <!-- Sonatype Portal for Maven Central -->
            <server>
                <id>central-ossrh-staging</id>
                <username>your-username</username>
                <password>your-password</password>
            </server>

            <!-- Elemental DockerHub -->
            <server>
                <id>docker.io</id>
                <username>your-username</username>
                <password>your-password</password>
            </server>

            <!-- Elemental GitHub Release -->
            <server>
                <id>github</id>
                <privateKey>your-github-personal-access-token</privateKey>
            </server>
        </servers>
    </settings>
    ```

2. You will need your GPG Key, Java KeyStore, and Apple Notarization API credentials for signing the release artifacts in the `<activeProfiles`> section on your machine in your local `~/.m2/settings.xml` file, e.g.:
    ```xml
     <profiles>

         <profile>
             <id>elemental-release-signing</id>
             <properties>
                 <elemental.release.key>ABC1234</elemental.release.key>
                 <elemental.release.public-keyfile>${user.home}/.gnupg/pubring.gpg</elemental.release.public-keyfile>
                 <elemental.release.private-keyfile>${user.home}/.gnupg/secring.gpg</elemental.release.private-keyfile>
                 <elemental.release.key.passphrase>your-password</elemental.release.key.passphrase>

                 <elemental.release.keystore>${user.home}/your.store</elemental.release.keystore>
                 <elemental.release.keystore.pass>your-keystore-password</elemental.release.keystore.pass>
                 <elemental.release.keystore.key.alias>your-alias</elemental.release.keystore.key.alias>
                 <elemental.release.keystore.key.pass>your-key-password</elemental.release.keystore.key.pass>

                 <elemental.release.notarize.apple-id>your-apple-developer-email@your-dom.ain</elemental.release.notarize.apple-id>
                 <elemental.release.notarize.team-id>your-apple-developer-team-id</elemental.release.notarize.team-id>
                 <elemental.release.notarize.password>your-apple-notarize-api-password</elemental.release.notarize.password>

                 <mac.codesign.identity>signature-of-your-apple-developer-certificate</mac.codesign.identity>
             </properties>
         </profile>

     </profiles>


     <activeProfiles>

           <activeProfile>elemental-release-signing</activeProfile>

     </activeProfiles>
    ```

3.  Merge any outstanding PRs that have been reviewed and accepted for the milestone (e.g. `elemental-8.0.0`).

4.  Make sure that you have the HEAD of `origin/main` (or `upstream` if you are on a fork).

5.  Update both the change date to today's date in:
    1. `LICENSE` the `Change Date:` parameter
    2. `elemental-parent/pom.xml` the `<change-date>` element

6. Run `mvn license:format` for the new change date to be applied, check the git diff, then commit it with a message like: `[license] Update BSL Change Date for upcoming release of version 8.0.0`, push the commit to GitHub.

7. Prepare the release, if you wish you can do a dry-run first by specifying `-DdryRun=true`:
    ```bash
    $ mvn -Ddocker=true -Dmac-signing=true -P installer -Dizpack-signing=true -Darguments="-Ddocker=true -Dmac-signing=true -P installer -Dizpack-signing=true" release:prepare
    ```

    Maven will start the release process and prompt you for any information that it requires, for example:

    ```
    [INFO] --- maven-release-plugin:2.1:prepare (default-cli) @ elemental ---
    [INFO] Verifying that there are no local modifications...
    [INFO]   ignoring changes on: pom.xml.next, pom.xml.releaseBackup, pom.xml.tag, pom.xml.backup, pom.xml.branch, release.properties
    [INFO] Executing: /bin/sh -c cd /Users/aretter/code/evolvedbinary/elemental.maven && git status
    [INFO] Working directory: /Users/aretter/code/evolvedbinary/elemental.maven
    [INFO] Checking dependencies and plugins for snapshots ...
    What is the release version for "Elemental"? (xyz.elemental:elemental) 8.0.0: :
    What is SCM release tag or label for "Elemental"? (xyz.elemental:elemental) elemental-8.0.0: :
    What is the new development version for "Elemental"? (xyz.elemental:elemental) 8.1.0-SNAPSHOT: :
    ```

8. Once the prepare process completes you can perform the release. This will upload Maven Artifacts to Maven Central (staging), Docker images to Docker Hub, and Elemental distributions and installer to GitHub releases:
    ```bash
    $ mvn -Ddocker=true -Dmac-signing=true -P installer -Dizpack-signing=true -Djarsigner.skip=false -Darguments="-Ddocker=true -Dmac-signing=true -P installer -Dizpack-signing=true -Djarsigner.skip=false" release:perform
    ```

9. You now need to request the artifacts to be moved from the Portal OSSRH Staging API to the Maven Central staging area:
    ```bash
    $ curl -vv -X POST -H "Authorization: Bearer your-bearer-token-for-maven-central" https://ossrh-staging-api.central.sonatype.com/manual/upload/defaultRepository/xyz.elemental
    ```

10. Update the stable branch (`gold`) of Elemental to reflect the latest release:
    ```bash
    $ git push origin elemental-8.0.0:gold
    ```

#### Publishing/Promoting the Product Release
1. Check that the new versions are visible on [GitHub](https://github.com/evolvedbinary/elemental/releases).

2. Check that the new versions are visible on [DockerHub](https://hub.docker.com/r/evolvedbinary/elemental).

3. Login to https://central.sonatype.com/publishing/deployments and release the Maven artifacts to Maven central as described [here](https://central.sonatype.org/publish/publish-portal-guide/).

4. Update the Mac HomeBrew for Elemental, see: [Releasing to Homebrew](https://github.com/evolvedbinary/elemental/blob/main/VERSIONING_AND_RELEASING.md#releasing-to-homebrew).

5. Visit the GitHub releases page [https://github.com/evolvedbinary/elemental/releases](https://github.com/evolvedbinary/elemental/releases) and create a new release, enter the tag you previously created and link the release notes from the blog.

6. Send an email to the `Elemental` mailing list announcing the release with a title similar to `[ANN] Release of Elemental 8.0.0`, copy and paste the release notes from the blog into the email and reformat appropriately (see past emails).

7. Tweet about it using the `elemental` Twitter account.

8. Post it to the [LinkedIn Elemental group](https://www.linkedin.com/groups/10070373/)

9. Submit a news item to XML.com - [https://www.xml.com/news/submit-news-item/](https://www.xml.com/news/submit-news-item/).

10. Update the Wikipedia page with the new version details - [https://en.wikipedia.org/wiki/Elemental](https://en.wikipedia.org/wiki/Elemental).

11. Go to GitHub and move all issues and PRs which are still open for the release milestone to the next release milestone. Close the release milestone.


### Releasing to Homebrew
[Homebrew](http://brew.sh) is a popular command-line package manager for macOS. Once Homebrew is installed, applications like Elemental can be installed via a simple command. Elemental's presence on Homebrew is found in the Caskroom project, as a "cask", at [https://github.com/caskroom/homebrew-cask/blob/master/Casks/elemental.rb](https://github.com/caskroom/homebrew-cask/blob/master/Casks/elemental.rb).

**Terminology:** "Homebrew Cask" is the segment of Homebrew where pre-built binaries and GUI applications go, whereas the original "Homebrew" project is reserved for command-line utilities that can be built from source. Because the macOS version of Elemental is released as an app bundle with GUI components, it is handled as a Homebrew Cask.

When there is a new release, registering the new release with Homebrew can be easily accomplished using Homebrew's `brew bump-cask-pr` command. Full directions for this utility as well as procedures for more complex PRs can be found on [the Homebrew Cask CONTRIBUTING page](https://github.com/Homebrew/homebrew-cask/blob/master/CONTRIBUTING.md), but, a simple version bump is a one-line command. For example, to update Homebrew's version of Elemental to 8.0.0, use this command:

```bash
brew bump-cask-pr --version 8.0.0 elemental
```

This command will cause your local Homebrew installation to download the new version of Elemental, calculate the installer's new SHA-256 fingerprint value, and construct a pull request under your GitHub account, like [this one](https://github.com/Homebrew/homebrew-cask/pull/210264). Once the pull request is submitted, continuous integration tests will run, and a member of the Homebrew community will review the PR. At times there is a backlog on the CI servers, but once tests pass, the community review is typically completed in a matter of hours.
