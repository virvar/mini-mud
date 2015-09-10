# Building
```
$ lein uberjar
```

# Running
```
$ cd target
```

#### Server
```
$ java -jar mini-mud-0.1.0-SNAPSHOT-standalone.jar server port
```
where **port** is optional, default *10100*

#### Client
```
$ java -jar mini-mud-0.1.0-SNAPSHOT-standalone.jar client host port
```
where **host** is optional, default *localhost*, **port** is optional, default *10100*
