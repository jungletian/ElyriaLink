package com.elyria.example;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.elyria.elyrialink.ElyriaLinkData;
import com.elyria.elyrialink.ElyriaLinkResolver;
import com.elyria.elyrialink.ElyriaLinkResolver.HasUserPredicate;
import com.elyria.elyrialink.ResolverBuilder;
import com.elyria.elyrialink.SimpleElyriaLinkParser;
import timber.log.Timber;

/**
 * @author jungletian (tjsummery@gmail.com)
 */
public class ObjectGraph implements HasUserPredicate {
  private static final String SERVICE_NAME = "com.elyria.example.OBJECT_GRAPH";

  private Context context;
  private ElyriaLinkResolver elyriaLinkResolver;
  private boolean hasUser;

  public ObjectGraph(Context context) {
    this.context = context.getApplicationContext();
    this.elyriaLinkResolver = create(this.context);
  }

  static boolean matches(String name) {
    return SERVICE_NAME.equals(name);
  }

  static ObjectGraph get(Context context) {
    Context app = context.getApplicationContext();
    return (ObjectGraph) app.getSystemService(SERVICE_NAME);
  }

  public ElyriaLinkResolver linkResolver() {
    return elyriaLinkResolver;
  }

  private ElyriaLinkResolver create(Context context) {
    HybridUrlProcessor hybridUrlProcessor = new HybridUrlProcessor(context);
    return ResolverBuilder.newBuilder(context)
        .setElyriaLinkParser(new SimpleElyriaLinkParser())
        .addInterceptor(new HttpUrlInterceptor(context))
        .addInterceptor(hybridUrlProcessor)
        .setIntentFactory(new IntentFactory(context))
        .setFallbackResolver(hybridUrlProcessor)
        .setHasUserPredicate(this)
        .addListener(new UrlListener())
        .build();
  }

  @Override public boolean hasUser() {
    return hasUser;
  }

  public void setHasUser(boolean hasUser) {
    this.hasUser = hasUser;
  }

  private static class IntentFactory implements ElyriaLinkResolver.IntentFactory {
    private final Context context;

    public IntentFactory(Context context) {
      this.context = context;
    }

    @NonNull @Override public Intent createLoginIntent() {
      return new Intent(context, LoginActivity.class);
    }

    @NonNull @Override public Intent createFailureIntent(@Nullable String url) {
      return ErrorLinkActivity.create(context, url);
    }
  }

  private static class HttpUrlInterceptor implements ElyriaLinkResolver.Interceptor {
    private final Context context;

    public HttpUrlInterceptor(Context context) {
      this.context = context;
    }

    @Nullable @Override
    public Intent intercept(@Nullable ElyriaLinkData linkData, @NonNull String url) {
      Timber.d("HttpUrlInterceptor.intercept");
      Uri uri = Uri.parse(url);
      if (uri != null && isHttpUrl(uri.getScheme())) {
        return WebViewActivity.create(context, url);
      }
      return null;
    }

    private static boolean isHttpUrl(String scheme) {
      return "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
    }
  }

  private static class UrlListener implements ElyriaLinkResolver.Listener {
    @Override public void onSuccess(Intent successIntent, String url) {
      Timber.d("Resolve %s into %s", url, successIntent.getComponent());
    }

    @Override public void onFailed(Intent errorIntent, @Nullable String errorUrl) {
      Timber.d("Can't resolve url: %s", errorUrl);
    }
  }
}
