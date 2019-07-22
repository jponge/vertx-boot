/*
 * MIT License
 *
 * Copyright (c) 2018 Julien Ponge
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package io.github.jponge.vertx.boot;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigRenderOptions;

import io.vertx.core.*;
import io.vertx.core.json.JsonObject;

/**
 * A verticle to deploy other verticles, based on a HOCON configuration.
 *
 * @author Julien Ponge
 */
public class BootVerticle extends AbstractVerticle {

  private static final String VERTX_BOOT_VERTICLES_PATH = "vertx-boot.verticles";
  private static final String CONF_KEY = "configuration";
  private static final String INSTANCES_KEY = "instances";
  private static final String EXTRA_CLASSPATH_KEY = "extra-classpath";
  private static final String HA_KEY = "high-availability";
  private static final String ISOLATED_CLASSES_KEY = "isolated-classes";
  private static final String ISOLATION_GROUP_KEY = "isolation-group";
  private static final String MAXWORKER_EXECTIME_KEY = "max-worker-execution-time";
  private static final String WORKER_KEY = "worker";
  private static final String WORKER_POOLNAME_KEY = "worker-pool-name";
  private static final String WORKER_POOLSIZE_KEY = "worker-pool-size";

  @Override
  public void start(Promise<Void> promise) {
    try {
      Config bootConfig = ConfigFactory.load();
      List<Config> configList = bootConfig.getConfig(VERTX_BOOT_VERTICLES_PATH).root().keySet().stream()
        .map(key -> bootConfig.getConfig(VERTX_BOOT_VERTICLES_PATH + "." + key)).collect(Collectors.toList());

      List<Future> futures = configList.stream()
        .map(this::deployVerticle)
        .collect(Collectors.toList());

      CompositeFuture.all(futures).setHandler(ar -> {
        if (ar.succeeded()) {
          promise.complete();
        } else {
          promise.fail(ar.cause());
        }
      });
    } catch (Throwable t) {
      promise.fail(t);
    }
  }

  private Future<String> deployVerticle(Config config) {
    Promise<String> promise = Promise.promise();
    try {
      String name = config.getString("name");
      DeploymentOptions options = new DeploymentOptions()
        .setInstances(getInstances(config))
        .setConfig(getConfig(config))
        .setExtraClasspath(getExtraClasspath(config))
        .setHa(getHa(config))
        .setIsolatedClasses(getIsolatedClasses(config))
        .setIsolationGroup(getIsolationGroup(config))
        .setMaxWorkerExecuteTime(getMaxWorkerExecuteTime(config))
        .setWorker(getWorker(config))
        .setWorkerPoolName(getWorkerPoolName(config))
        .setWorkerPoolSize(getWorkerPoolSize(config));
      vertx.deployVerticle(name, options, ar -> {
        if (ar.succeeded()) {
          promise.complete(ar.result());
        } else {
          promise.fail(ar.cause());
        }
      });
    } catch (Throwable t) {
      promise.fail(t);
    }
    return promise.future();
  }

  private int getWorkerPoolSize(Config config) {
    if (config.hasPath(WORKER_POOLSIZE_KEY)) {
      return config.getInt(WORKER_POOLSIZE_KEY);
    }
    return 1;
  }

  private String getWorkerPoolName(Config config) {
    if (config.hasPath(WORKER_POOLNAME_KEY)) {
      return config.getString(WORKER_POOLNAME_KEY);
    }
    return null;
  }

  private boolean getWorker(Config config) {
    if (config.hasPath(WORKER_KEY)) {
      return config.getBoolean(WORKER_KEY);
    }
    return false;
  }

  private long getMaxWorkerExecuteTime(Config config) {
    if (config.hasPath(MAXWORKER_EXECTIME_KEY)) {
      return config.getLong(MAXWORKER_EXECTIME_KEY);
    }
    return Long.MAX_VALUE;
  }

  private String getIsolationGroup(Config config) {
    if (config.hasPath(ISOLATION_GROUP_KEY)) {
      return config.getString(ISOLATION_GROUP_KEY);
    }
    return null;
  }

  private List<String> getIsolatedClasses(Config config) {
    if (config.hasPath(ISOLATED_CLASSES_KEY)) {
      return config.getStringList(ISOLATED_CLASSES_KEY);
    }
    return null;
  }

  private boolean getHa(Config config) {
    if (config.hasPath(HA_KEY)) {
      return config.getBoolean(HA_KEY);
    }
    return false;
  }

  private List<String> getExtraClasspath(Config config) {
    if (config.hasPath(EXTRA_CLASSPATH_KEY)) {
      return config.getStringList(EXTRA_CLASSPATH_KEY);
    }
    return null;
  }

  private JsonObject getConfig(Config config) {
    if (config.hasPath(CONF_KEY)) {
      return new JsonObject(config.getValue(CONF_KEY).render(ConfigRenderOptions.concise()));
    }
    return new JsonObject();
  }

  private int getInstances(Config config) {
    if (config.hasPath(INSTANCES_KEY)) {
      return config.getInt(INSTANCES_KEY);
    }
    return 1;
  }
}
