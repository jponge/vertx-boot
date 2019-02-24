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

import com.typesafe.config.ConfigFactory;
import io.github.jponge.vertx.boot.samples.ConfigDumpingVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(VertxExtension.class)
@DisplayName("🚀 Verticle deployments")
class BootVerticleTest {

  @BeforeEach
  void prepare() {
    System.clearProperty("config.resource");
    ConfigFactory.invalidateCaches();
  }

  @Test
  @DisplayName("Deploy multiple verticles with a default application.conf resource file")
  void deployment_default_config(Vertx vertx, VertxTestContext testContext) {
    Checkpoint fooCheckpoint = testContext.checkpoint(4);
    Checkpoint barCheckpoint = testContext.checkpoint(2);

    vertx.deployVerticle(new BootVerticle(), testContext.succeeding(id -> {

      vertx.eventBus().consumer("foo", message -> {
        testContext.verify(() -> {
          assertTrue(message.body() instanceof JsonObject);
          assertTrue(((JsonObject) message.body()).isEmpty());
          fooCheckpoint.flag();
        });
      });

      vertx.eventBus().consumer("bar", message -> {
        testContext.verify(() -> {
          assertTrue(message.body() instanceof JsonObject);
          JsonObject conf = (JsonObject) message.body();
          assertEquals("abc", conf.getString("a"));
          assertEquals("def", conf.getString("b"));
          assertEquals((Integer) 123, conf.getInteger("c"));
          JsonArray d = conf.getJsonArray("d");
          assertEquals(3, d.size());
          assertEquals((Integer) 1, d.getInteger(0));
          assertEquals((Integer) 2, d.getInteger(1));
          assertEquals((Integer) 3, d.getInteger(2));
          barCheckpoint.flag();
        });
      });

    }));
  }

  @Test
  @DisplayName("Deploy from a alternative.conf resource file")
  void deployment_alternative_config(Vertx vertx, VertxTestContext testContext) {
    System.setProperty("config.resource", "alternative.conf");
    Checkpoint checkpoint = testContext.checkpoint();

    vertx.deployVerticle(new BootVerticle(), testContext.succeeding(id -> {

      vertx.eventBus().consumer("foo", message -> {
        testContext.verify(() -> {
          assertTrue(message.body() instanceof JsonObject);
          JsonObject conf = (JsonObject) message.body();
          assertEquals(1, conf.size());
          assertEquals("Yo!", conf.getString("abc"));
          checkpoint.flag();
        });
      });

    }));
  }

  @Test
  @DisplayName("Do not pass any extra configuration (worker verticle, etc)")
  void pass_no_extra_config(Vertx vertx, VertxTestContext testContext) {
    System.setProperty("config.resource", "config-dump-noparameters.conf");

    vertx.eventBus().consumer("config.dump", message -> {
      testContext.verify(() -> {
        assertTrue(message.body() instanceof JsonObject);
        JsonObject conf = (JsonObject) message.body();
        assertEquals(false, conf.getBoolean("worker"));
        assertEquals(false, conf.getBoolean("clustered"));
        testContext.completeNow();
      });
    });

    vertx.deployVerticle(new BootVerticle(), testContext.succeeding());
  }

  @Test
  @DisplayName("Pass extra configuration (worker verticle, etc)")
  void pass_extra_config(Vertx vertx, VertxTestContext testContext) {
    System.setProperty("config.resource", "config-dump-withparameters.conf");

    vertx.eventBus().consumer("config.dump", message -> {
      testContext.verify(() -> {
        assertTrue(message.body() instanceof JsonObject);
        JsonObject conf = (JsonObject) message.body();
        assertEquals(true, conf.getBoolean("worker"));
        assertEquals(false, conf.getBoolean("clustered"));
        testContext.completeNow();
      });
    });

    vertx.deployVerticle(new BootVerticle(), testContext.succeeding());
  }
}
