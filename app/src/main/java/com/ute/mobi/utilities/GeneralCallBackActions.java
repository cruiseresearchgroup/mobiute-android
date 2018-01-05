package com.ute.mobi.utilities;

/**
 * Created by jonathanliono on 23/12/2015.
 */
public final class GeneralCallBackActions {
  public static interface OnSuccess {
    void onSuccess(String result);
  }

  public static interface OnFail {
    void onFail(String result);
  }
}
