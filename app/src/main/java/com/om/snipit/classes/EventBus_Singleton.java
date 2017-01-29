package com.om.snipit.classes;

import com.squareup.otto.Bus;

public class EventBus_Singleton {
  private static final Bus BUS = new Bus();

  private EventBus_Singleton() {
    //no instances
  }

  public static Bus getInstance() {
    return BUS;
  }
}
