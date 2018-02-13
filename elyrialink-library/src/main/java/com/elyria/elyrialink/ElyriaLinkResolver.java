package com.elyria.elyrialink;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * @author jungletian (tjsummery@gmail.com)
 */
public interface ElyriaLinkResolver {
  @NonNull Intent resolve(@Nullable String url);

  @NonNull Intent resolve(@Nullable String source, @Nullable String url);

  interface Interceptor {
    @Nullable Intent intercept(@Nullable ElyriaLinkData linkData, @NonNull String url);
  }

  interface HasUserPredicate {
    boolean hasUser();
  }

  interface FallbackResolver {
    @Nullable Intent resolve(@Nullable ElyriaLinkData entry, @NonNull String url);

    FallbackResolver NONE = new FallbackResolver() {
      @Nullable @Override
      public Intent resolve(@Nullable ElyriaLinkData entry, @NonNull String url) {
        return null;
      }
    };
  }

  interface IntentFactory {
    @NonNull Intent createLoginIntent();

    @NonNull Intent createFailureIntent(@Nullable String url);
  }

  interface Listener {
    void onSuccess(Intent successIntent, String url);

    void onFailed(Intent errorIntent, @Nullable String errorUrl);
  }

  interface Builder {
    Builder addListener(@NonNull Listener listener);

    Builder setElyriaLinkParser(@NonNull ElyriaLinkParser linkParser);

    Builder setFallbackResolver(@NonNull FallbackResolver fallbackResolver);

    Builder addInterceptor(@NonNull Interceptor interceptor);

    Builder setHasUserPredicate(@NonNull HasUserPredicate predicate);

    Builder setIntentFactory(@NonNull IntentFactory intentFactory);

    ElyriaLinkResolver build();
  }
}
