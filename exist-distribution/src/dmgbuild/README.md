The App_DS_Store file was created using https://github.com/al45tair/dmgbuild

```bash
$ git clone https://github.com/al45tair/dmgbuild
$ cd dmgbuild
$ python setup.py install
```

Then with these settings:

```bash
$ dmgbuild -s settings.py "Elemental" elemental-${project.version}.dmg
```

The .DS_Store file can then be found in:
```bash
$ open elemental-${project.version}.dmg
$ file /Volumes/Elemental/.DS_Store
```
