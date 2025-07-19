# Building Elemental from Source Code

Elemental itself is written in Java 21. The build system is [Apache Maven](http://maven.apache.org/). If you're not familiar with Git, we recommend [this excellent online interactive tutorial](http://try.github.io).

To build Elemental:

1. Checkout the Git Repository

    ```bash
    $ git clone https://github.com/evolvedbinary/elemental.git
    $ git checkout gold
    $ cd elemental
    ```

2. Execute Maven to compile Elemental

    We provide a build script to try and make common build tasks easier for users.

    For macOS/Linux/Unix platforms:
    ```bash
    $ ./build.sh quick
    ```

    or, for Windows platforms:
    ```cmd
    > build.bat quick
    ```

From here, you now have a compiled version of Elemental in the `exist-distribution/target` folder that you may use just as you would an installed version of Elemental. An installer is also build and present in `exist-installer/target` for easy installation elsewhere.

The `quick` build target will build distribution directory that can be found at: `exist-distribution/target/elemental-x.y.x-dir`.
If you wish to see what other build targets are available, you can run `./build.sh --help` (or `build.bat --help` on Windows platforms).


Further build options can be found at: [eXist-db Build Documentation](http://www.exist-db.org/exist/apps/doc/exist-building.xml "How to build eXist-db").

**NOTE:** 
In the above example, we switched the current (checked-out) branch from `main` to `gold`.
- `main` is the current (and stable) work-in-progress (the next release)
- `gold` is the last release
The choice of which to use is up to you.


