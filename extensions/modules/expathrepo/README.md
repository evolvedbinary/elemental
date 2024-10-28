# EXPath Repo for Elemental

This module uses a .xar file as part of the resources for its tests.

The .xar source code is in a separate Maven module and can be found in the `expathrepo-trigger-test` sub-folder.
To build the .xar file and copy it for use in the tests for this module:

```bash
cd expathrepo-trigger-test
mvn clean package
cp target/exist-expathrepo-trigger-test-6.4.0-SNAPSHOT.xar ../src/test/resources/exist-expathrepo-trigger-test.xar
```

Each time you modify the source code of the `expathrepo-trigger-test` module, you should rebuild it and copy the .xar file to the `src/test/resources` folder of this module and re-commit it to Git.