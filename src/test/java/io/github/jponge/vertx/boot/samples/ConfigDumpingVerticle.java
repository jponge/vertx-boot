package io.github.jponge.vertx.boot.samples;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;

public class ConfigDumpingVerticle extends AbstractVerticle {

  @Override
  public void start() {
    JsonObject dump = new JsonObject();
    dump.put("worker", context.isWorkerContext());
    dump.put("clustered", vertx.isClustered());
    vertx.eventBus().send("config.dump", dump);
  }
}
