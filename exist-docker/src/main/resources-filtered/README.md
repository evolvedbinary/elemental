# ${project.name}
${project.description}

[![License](https://img.shields.io/badge/license-AGPL%203.1-orange.svg)](https://www.gnu.org/licenses/agpl-3.0.html)

This module holds the source files for building a minimal docker image of the [Elemental](https://www.elemental.xyz)
NoSQL Database. Images are automatically updated as part of the build-test life-cycle. 
These images are based on Google Cloud Platform's ["Distroless" Docker Images](https://github.com/GoogleCloudPlatform/distroless).


## Requirements
*   [Docker](https://www.docker.com): `18-stable`
### For building
*   [maven](https://maven.apache.org/): `^3.6.0`
*   [java](https://www.java.com/): `17`
*   [bats](https://github.com/bats-core/bats-core): `^1.1.0` (for testing)

## How to use
Pre-build images are available on [DockerHub](https://hub.docker.com/r/evolvedbinary/elemental/). 
There are two continuously updated channels:
*   `release` for the stable releases based on the [`gold` branch](https://github.com/evolvedbinary/elemental/tree/gold)
*   `latest` for the latest commit to the [`main` branch](https://github.com/evolvedbinary/elemental/tree/main).
*   `debug` for the latest debug (includes shell) build from the [`main` branch](https://github.com/evolvedbinary/elemental/tree/main).

To download the image run:
```bash
docker pull evolvedbinary/elemental:latest
```

Once the download is complete, you can run the image
```bash
docker run -dit -p 8080:8080 -p 8443:8443 --name elemental evolvedbinary/elemental:latest
```

### What does this do?

*   `-it` allocates a TTY and keeps STDIN open.  This allows you to interact with the running Docker container via your console.
*   `-d` detaches the container from the terminal that started it. So your container won't stop when you close the terminal.
*   `-p` maps the Containers internal and external port assignments (we recommend sticking with matching pairs). This allows you to connect to the Elemental Server running in the Docker container.
*   `--name` lets you provide a name (instead of using a randomly generated one)

The only required parts are `docker run evolvedbinary/elemental`. 
For a full list of available options see the official [Docker documentation](https://docs.docker.com/engine/reference/commandline/run/)

After running the `pull` and `run` commands, you can access Elemental via [localhost:8080](localhost:8080) in your browser.

To stop the container issue:
```bash
docker stop elemental
```

or if you omitted the `-d` flag earlier press `CTRL-C` inside the terminal showing the Elemental log messages.

### Interacting with the running container
You can interact with a running container as if it were a regular Linux host (without a shell in our case). 
You can issue shell-like commands to the Java admin client, as we do throughout this readme, but you can't open the shell in interactive mode (unless you use the `debug` image).

The name of the container in this readme is `elemental`, adjust the name in the commands to suit your needs:

```bash
# Using java syntax on a running Elemental instances
docker exec elemental java org.exist.start.Main client --no-gui --xpath "system:get-version()"

# Interacting with the JVM
docker exec elemental java -version
```

Containers built from this image run periodic health checks to ensure that Elemental is operating normally. 
If `docker ps` reports `unhealthy` you can get a more detailed report with this command:  
```bash
docker inspect --format='{{json .State.Health}}' elemental
```

### Logging
You can access the Elemental logs via:
```bash
docker logs elemental
```

This works best when providing the `-t` flag when running an image.

## Building the Image
Building is integrated into Maven via the [fabric8 plugin](https://dmp.fabric8.io): 
To build a docker image from a local clone of Elemental:
```bash
mvn -Pdocker -DskipTests -Ddependency-check.skip=true clean package
```
