package com.elyria.example;

import android.app.Application;
import android.support.annotation.NonNull;
import com.elyria.elyrialink.ElyriaLinker;
import com.google.gson.Gson;
import java.io.IOException;
import java.lang.reflect.Type;
import timber.log.Timber;

/**
 * @author jungletian (tjsummery@gmail.com)
 */
public class ExampleApplication extends Application {

  private ObjectGraph objectGraph;

  @Override public void onCreate() {
    Timber.plant(new Timber.DebugTree());
    super.onCreate();
    objectGraph = new ObjectGraph(this);
    ElyriaLinker.setDebug(true);
    ElyriaLinker.installJsonConverter(new GsonConverter());
  }

  @Override public Object getSystemService(String name) {
    if (ObjectGraph.matches(name)) {
      return objectGraph;
    }
    return super.getSystemService(name);
  }

  private static final class GsonConverter implements ElyriaLinker.JsonConverter {
    private final Gson gson = new Gson();

    @Override public Object fromJson(@NonNull String jsonString, Type type) throws IOException {
      return gson.fromJson(jsonString, type);
    }

    @Override public String toJson(@NonNull Object value, Type type) throws IOException {
      return gson.toJson(value, type);
    }
  }
}
