package com.elyria.elyrialink;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.elyria.elyrialink.ElyriaLinker.buildExtraFromUrl;
import static com.elyria.elyrialink.ElyriaLinker.isSamePage;
import static com.elyria.elyrialink.ElyriaLinker.putElyriaLink;
import static com.elyria.elyrialink.ElyriaLinker.putRedirectLink;
import static com.elyria.elyrialink.ElyriaLinker.putSourceLink;
import static com.elyria.elyrialink.internal.Utils.checkState;

/**
 * @author jungletian (tjsummery@gmail.com)
 */
public class LinkResolverImpl implements ElyriaLinkResolver {
  private final IntentFactory intentFactory;
  private final List<Interceptor> interceptors;
  private final FallbackResolver fallbackResolver;
  private final CopyOnWriteArrayList<Listener> listeners = new CopyOnWriteArrayList<>();
  private final ElyriaLinkParser elyriaLinkParser;
  private final HasUserPredicate hasUserPredicate;

  public LinkResolverImpl(ResolverBuilder builder) {
    this.listeners.addAll(builder.listeners);
    this.intentFactory = builder.intentFactory;
    this.fallbackResolver = builder.fallbackResolver;
    this.elyriaLinkParser = builder.elyriaLinkParser;
    this.hasUserPredicate = builder.hasUserPredicate;

    List<Interceptor> list = new ArrayList<>(builder.interceptors);
    Interceptor buildInInterceptor = new BuildInInterceptor(builder.context);
    list.add(buildInInterceptor);
    interceptors = Collections.unmodifiableList(list);
  }

  @NonNull @Override public Intent resolve(@Nullable String elyriaUrl) {
    return resolve(null, elyriaUrl);
  }

  @NonNull @Override public Intent resolve(@Nullable String source, @Nullable String elyriaUrl) {
    Intent result = null;
    boolean failed = false;

    String url = elyriaUrl == null ? null : elyriaUrl.trim();
    if (!TextUtils.isEmpty(elyriaUrl)) {
      result = processUrl(url);
    }

    if (result == null) {
      failed = true;
      result = checkResultNotNull(intentFactory.createFailureIntent(url));
    }

    notifyListener(result, url, failed);
    putExtras(result, source, url, failed);
    return result;
  }

  private void putExtras(Intent intent, String source, String url, boolean failed) {
    if (!TextUtils.isEmpty(url)) {
      putElyriaLink(intent, url);
    }
    if (!TextUtils.isEmpty(source)) {
      putSourceLink(intent, source);
    }
    if (!failed) {
      intent.putExtras(buildExtraFromUrl(url));
    }
  }

  private void notifyListener(Intent intent, String url, boolean failed) {
    for (Listener listener : listeners) {
      if (failed) {
        listener.onFailed(intent, url);
      } else {
        listener.onSuccess(intent, url);
      }
    }
  }

  private Intent checkResultNotNull(Intent result) {
    checkState(result != null, "IntentFactory can't return null");
    return result;
  }

  private Intent processUrl(@NonNull String url) {
    Intent result;
    ElyriaLinkData linkData = findElyriaLinkEntry(url);
    if (linkData != null && linkData.userRequired && !hasUserPredicate.hasUser()) {
      result = checkResultNotNull(intentFactory.createLoginIntent());
      putRedirectLink(result, url);
    } else {
      result = processInterceptors(linkData, url);
    }
    if (result == null) {
      result = fallbackResolver.resolve(linkData, url);
    }
    return result;
  }

  private Intent processInterceptors(ElyriaLinkData linkData, String url) {
    Intent result = null;
    for (Interceptor interceptor : interceptors) {
      Intent intent = interceptor.intercept(linkData, url);
      if (intent != null) {
        result = intent;
        break;
      }
    }
    return result;
  }

  private ElyriaLinkData findElyriaLinkEntry(String url) {
    ElyriaLinkData linkEntry = null;
    if (!TextUtils.isEmpty(url)) {
      for (String link : elyriaLinkParser.urls()) {
        if (isSamePage(link, url)) {
          linkEntry = elyriaLinkParser.parse(link);
          break;
        }
      }
    }
    return linkEntry;
  }

  static class BuildInInterceptor implements Interceptor {
    private final Context context;

    @VisibleForTesting BuildInInterceptor(Context context) {
      this.context = context;
    }

    @Nullable @Override
    public Intent intercept(@Nullable ElyriaLinkData linkData, @NonNull String url) {
      if (linkData != null) {
        return new Intent(context, linkData.clazz);
      }
      return null;
    }
  }
}
