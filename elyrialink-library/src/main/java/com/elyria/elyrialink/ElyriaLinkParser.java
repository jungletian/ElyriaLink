package com.elyria.elyrialink;

import android.support.annotation.NonNull;
import java.util.Set;

/**
 * @author jungletian (tjsummery@gmail.com)
 */
public interface ElyriaLinkParser {
  ElyriaLinkData parse(@NonNull String link);

  Set<String> urls();
}
