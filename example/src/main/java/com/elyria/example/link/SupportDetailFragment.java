package com.elyria.example.link;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import com.elyria.elyrialink.ElyriaLinkQuery;
import com.elyria.elyrialink.ElyriaLinker;
import com.elyria.elyrialink.IllegalElyriaLinkException;
import com.elyria.elyrialink.Json;

/**
 * @author jungletian (tjsummery@gmail.com)
 */

public class SupportDetailFragment extends Fragment {
  @ElyriaLinkQuery("name") String productName;
  @Json @ElyriaLinkQuery("product") Product product;

  @Override public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    try {
      ElyriaLinker.bind(this);
    } catch (IllegalElyriaLinkException e) {
      getActivity().finish();
    }
  }
}
