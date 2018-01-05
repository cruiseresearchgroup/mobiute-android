package com.ute.mobi.managers;

import com.ute.mobi.models.UteModelCellInfo;

import java.util.List;

/**
 * Created by jonathanliono on 30/12/2016.
 */

public interface CellPhoneListener {
  public void onCellInfoChanged(List<UteModelCellInfo> cellInfos);
}
