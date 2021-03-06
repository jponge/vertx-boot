[![Build Status](https://travis-ci.org/jponge/vertx-boot.svg?branch=master)](https://travis-ci.org/jponge/vertx-boot)
![License](https://img.shields.io/github/license/jponge/vertx-boot.svg)

# 🚀 Vert.x Boot

> An Eclipse Vert.x verticle to boot an application from HOCON configuration.

The goal of this micro-library is to offer a simple way to deploy verticles from a [HOCON](https://github.com/lightbend/config/blob/master/HOCON.md) configuration.
More specifically, it allows to:

1. specify what verticles to deploy, and
2. specify how many instances of each verticle to deploy, and
3. pass some JSON configuration (HOCON is a superset of JSON).

## Dependency

* `groupId`: `io.github.jponge`
* `artifactId`: `vertx-boot`

The library is being published to both [Maven Central](https://search.maven.org/#search%7Cga%7C1%7Ca%3A%22vertx-boot%22) and [Bintray JCenter](https://bintray.com/jponge/vertx-boot/vertx-boot).

## Configuring Vert.x Boot

The HOCON configuration is fetched with [lightbend/config](https://github.com/lightbend/config) using [the standard behavior](https://github.com/lightbend/config#standard-behavior) so please check the corresponding documentation for overriding files, resources and also overriding values using system properties and environment variables.
You can use all of the nice features in HOCON, really (includes, substitutions, etc).

The HOCON configuration can be larger than what is required for _Vert.x Boot_.

### Basic configuration

Here is an example:

```hocon
vertx-boot {

  verticles {

    foo {
      name = "io.github.jponge.vertx.boot.samples.FooVerticle"
      instances = 4
    }

    bar {
      name = "io.github.jponge.vertx.boot.samples.BarVerticle"
      instances = 2
      configuration.a = "abc"
      configuration.b = "def"
      configuration {
        c = 123
        d = [1, 2, 3]
      }
    }
    
    baz {
      name = "io.github.jponge.vertx.boot.samples.FooVerticle"
    }
  }
}
```

Each verticle key (e.g., `foo` and `bar` in the example above) is purely decorative.
A verticle class can be deployed more than once with different configurations and instance count.

The `instance` and `configuration` keys in verticles are optional: by default a single instance is being deployed, and the configuration is an empty JSON object.

### Advanced configuration

More advanced settings are available to match Vert.x `DeploymentOptions`:

* `extra-classpath` a string array of extra classpath entries
* `high-availability` a boolean for verticle high-availability
* `isolated-classes` a string array of isolated classes
* `isolated-group` a string for an isolated classes group name
* `worker` a boolean to deploy as a worker verticle
* `max-worker-execution-time` an integer number to define the maximum worker execution time
* `worker-pool-name` a string to name the worker pool
* `worker-pool-size` an integer to size the worker pool

Here is a sample advanced configuration:

```hocon
vertx-boot {
  verticles {
    foo {
      name = "io.github.jponge.vertx.boot.samples.FooVerticle"
      instances = 4
      worker = true
      worker-pool-name = "Fooz"
      worker-pool-size = 4
      configuration {
        a = 1
        b = 2
      }
    }
  }
}
```

## Using the verticle

The verticle class is `io.github.jponge.vertx.boot.BootVerticle`.

You can deploy it programmatically and it will then deploy the other verticles.

If you create a _fat jar_ and rely on the `Main-Verticle` manifest entry and the `io.vertx.core.Launcher` main class, then all you have to do is point the `Main-Verticle` entry to `io.github.jponge.vertx.boot.BootVerticle`.

## Contributing

Feel-free to report issues and propose pull-requests!
