# Building Elemental from Source Code

Elemental itself is written in Java 17. The build system is [Apache Maven](http://maven.apache.org/). If you're not familiar with Git, we recommend [this excellent online interactive tutorial](http://try.github.io).

To build Elemental:

- Checkout the Git Repository
- Execute a Maven to compile Elemental

```bash
$ git clone https://github.com/evolvedbinary/elemental.git
$ cd elemental
$ git checkout gold
$ mvn -DskipTests package
```

From here, you now have a compiled version of Elemental in the `exist-distribution/target` folder that you may use just as you would an installed version of Elemental. An installer is also build and present in `exist-installer/target` for easy installation elsewhere.

Useful build switches:
- `-Ddocker=true` : builds the docker image
- `-DskipTests` : skips running tests
- `-Ddependency-check.skip=true` : skips validating dependencies

Further build options can be found at: [eXist-db Build Documentation](http://www.exist-db.org/exist/apps/doc/exist-building.xml "How to build eXist-db").

**NOTE:** 
In the above example, we switched the current (checked-out) branch from `main` to `gold`.
- `main` is the current (and stable) work-in-progress (the next release)
- `gold` is the last release
The choice of which to use is up to you.


