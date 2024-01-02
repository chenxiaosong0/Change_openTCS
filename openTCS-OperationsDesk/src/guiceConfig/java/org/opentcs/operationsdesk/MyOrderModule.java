package org.opentcs.operationsdesk;

import com.google.inject.AbstractModule;
import org.opentcs.customizations.ApplicationEventBus;
import org.opentcs.util.event.EventBus;
import org.opentcs.util.event.EventHandler;
import org.opentcs.util.event.EventSource;
import org.opentcs.util.event.SimpleEventBus;

/**
 * @author xiaosongChen
 * @create 2023-12-05 17:32
 * @description :
 */
public class MyOrderModule extends AbstractModule {
  @Override
  protected void configure() {
    configureEventBus();
    bind(MyOrder.class);
  }

  private void configureEventBus() {//配置事件总线
    EventBus newEventBus = new SimpleEventBus();
    bind(EventHandler.class)
        .annotatedWith(ApplicationEventBus.class)
        .toInstance(newEventBus);
    bind(EventSource.class)
        .annotatedWith(ApplicationEventBus.class)
        .toInstance(newEventBus);
    bind(EventBus.class)
        .annotatedWith(ApplicationEventBus.class)
        .toInstance(newEventBus);
  }


}
