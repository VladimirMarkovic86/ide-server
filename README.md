# Integrated development environment server

Integrated development environment server project implements backend part of ide project, and is hosted on server-lib.

### Installing

Clone project from git by executing:

```
git clone git@github.com:VladimirMarkovic86/ide-server.git

or

git clone https://github.com/VladimirMarkovic86/ide-server.git
```

After that execute command:

```
cd ide-server
```

Add following line in hosts file:

```
127.0.0.1 ide
```

and run project with this command:

```
lein run
```

By default project listens on port 1604, so you can make requests on https://ide:1604 address.

**For purpose of making requests ide-client was made and you should start up ide-client also.**

## Authors

* **Vladimir Markovic** - [VladimirMarkovic86](https://github.com/VladimirMarkovic86)

## License

This project is licensed under the Eclipse Public License 1.0 - see the [LICENSE](LICENSE) file for details

