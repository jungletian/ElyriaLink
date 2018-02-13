package com.elyria.elyrialink;

import android.content.Context;
import android.support.annotation.NonNull;
import com.elyria.elyrialink.ElyriaLinkResolver.Builder;
import com.elyria.elyrialink.ElyriaLinkResolver.FallbackResolver;
import com.elyria.elyrialink.ElyriaLinkResolver.HasUserPredicate;
import com.elyria.elyrialink.ElyriaLinkResolver.IntentFactory;
import com.elyria.elyrialink.ElyriaLinkResolver.Interceptor;
import com.elyria.elyrialink.ElyriaLinkResolver.Listener;
import java.util.ArrayList;
import java.util.List;

import static com.elyria.elyrialink.ElyriaLinkResolver.FallbackResolver.NONE;
import static com.elyria.elyrialink.internal.Utils.checkNotNull;

/**
 * @author jungletian (tjsummery@gmail.com)
 */
public final class ResolverBuilder implements Builder {

  final Context context;
  List<Interceptor> interceptors;
  ElyriaLinkParser elyriaLinkParser;
  IntentFactory intentFactory;
  List<Listener> listeners;
  FallbackResolver fallbackResolver = NONE;
  HasUserPredicate hasUserPredicate;

  private ResolverBuilder(Context context) {
    this.context = context;
  }

  public static ResolverBuilder newBuilder(Context context) {
    return new ResolverBuilder(context);
  }

  @Override public Builder addListener(@NonNull Listener listener) {
    if (listeners == null) {
      listeners = new ArrayList<>();
    }
    listeners.add(listener);
    return this;
  }

  @Override public Builder setElyriaLinkParser(@NonNull ElyriaLinkParser linkParser) {
    this.elyriaLinkParser = checkNotNull(linkParser, "linkParser == null");
    return this;
  }

  @Override public Builder setFallbackResolver(@NonNull FallbackResolver fallbackResolver) {
    this.fallbackResolver = checkNotNull(fallbackResolver, "fallbackResolver == null");
    return this;
  }

  @Override public Builder addInterceptor(@NonNull Interceptor interceptor) {
    if (interceptors == null) {
      interceptors = new ArrayList<>();
    }
    interceptors.add(interceptor);
    return this;
  }

  @Override public Builder setHasUserPredicate(@NonNull HasUserPredicate predicate) {
    this.hasUserPredicate = checkNotNull(predicate, "predicate == null");
    return this;
  }

  @Override public Builder setIntentFactory(@NonNull IntentFactory intentFactory) {
    this.intentFactory = checkNotNull(intentFactory, "intentFactory == null");
    return this;
  }

  @Override public ElyriaLinkResolver build() {
    checkNotNull(context);
    checkNotNull(elyriaLinkParser);
    checkNotNull(intentFactory);
    checkNotNull(hasUserPredicate);
    if (interceptors == null) {
      interceptors = new ArrayList<>();
    }
    if (listeners == null) {
      listeners = new ArrayList<>();
    }
    return new LinkResolverImpl(this);
  }
}
