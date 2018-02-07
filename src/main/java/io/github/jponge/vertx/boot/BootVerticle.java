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

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigRenderOptions;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A verticle to deploy other verticles, based on a HOCON configuration.
 *
 * @author Julien Ponge
 */
public class BootVerticle extends AbstractVerticle {

  private static final String VERTX_BOOT_VERTICLES_PATH = "vertx-boot.verticles";
  private static final String CONF_KEY = "configuration";
  private static final String INSTANCES_KEY = "instances";

  @Override
  public void start(Future<Void> startFuture) {
    try {
      Config bootConfig = ConfigFactory.load();
      List<Config> configList = bootConfig
        .getConfig(VERTX_BOOT_VERTICLES_PATH)
        .root()
        .keySet()
        .stream()
        .map(key -> bootConfig.getConfig(VERTX_BOOT_VERTICLES_PATH + "." + key))
        .collect(Collectors.toList());
      List<Future> futures = Stream
        .generate(Future::future)
        .limit(configList.size())
        .collect(Collectors.toList());
      for (int i = 0; i < configList.size(); i++) {
        deployVerticle(configList.get(i), futures.get(i));
      }
      CompositeFuture.all(futures).setHandler(ar -> {
        if (ar.succeeded()) {
          startFuture.complete();
        } else {
          startFuture.fail(ar.cause());
        }
      });
    } catch (Throwable t) {
      startFuture.fail(t);
    }
  }

  private void deployVerticle(Config config, Future future) {
    try {
      String name = config.getString("name");
      JsonObject conf;
      int instances = 1;
      if (config.hasPath(CONF_KEY)) {
        conf = new JsonObject(config.getValue(CONF_KEY).render(ConfigRenderOptions.concise()));
      } else {
        conf = new JsonObject();
      }
      if (config.hasPath(INSTANCES_KEY)) {
        instances = config.getInt(INSTANCES_KEY);
      }
      DeploymentOptions options = new DeploymentOptions()
        .setInstances(instances)
        .setConfig(conf);
      vertx.deployVerticle(name, options, ar -> {
        if (ar.succeeded()) {
          future.complete();
        } else {
          future.fail(ar.cause());
        }
      });
    } catch (Throwable t) {
      future.fail(t);
    }
  }
}
