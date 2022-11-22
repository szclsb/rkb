# Remote Keyboard

The Remote Keyboard (RKB) is a simple Java project to simulate keystrokes on one computer,
which are intercepted and forwarded by another computer.

## Modules

### Driver 

Module `ch.szclsb.rkb.driver`

Simple Java Keyboard Driver wrapper module, which is build on top of [rkb-native](https://github.com/szclsb/rkb-native).
This module uses the foreign function and memory API (ffm).
Thus don't forget to enable preview if needed and grant native access 
```
--enable-native-access=ch.szclsb.rkb.driver
```

### Comm

Module `ch.szclsb.rkb.comm`

Simple Java peer-to-peer network communication implementation

### App

Module `ch.szclsb.rkb.app`

JavaFX UI Application, which uses both other modules.
