/**
 * Copyright (c) The openTCS Authors.
 *
 * This program is free software and subject to the MIT license. (For details,
 * see the licensing information (LICENSE.txt) you should have received with
 * this copy of the software.)
 */
package org.opentcs.guing.plugins.panels.loadgenerator.trigger;

import java.util.LinkedHashSet;
import static java.util.Objects.requireNonNull;
import java.util.Set;
import org.opentcs.access.KernelRuntimeException;
import org.opentcs.components.kernel.services.TCSObjectService;
import org.opentcs.customizations.ApplicationEventBus;
import org.opentcs.data.TCSObjectEvent;
import org.opentcs.data.order.TransportOrder;
import org.opentcs.guing.plugins.panels.loadgenerator.batchcreator.OrderBatchCreator;
import org.opentcs.util.event.EventHandler;
import org.opentcs.util.event.EventSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Triggers creation of a batch of orders if the number of transport orders
 * in progress drop to or below a given threshold.
 * 如果正在处理的传输订单数量降至或低于给定阈值，则触发一批订单的创建。
 */
public class ThresholdOrderGenTrigger
    implements EventHandler,
               OrderGenerationTrigger {

  /**
   * This class's Logger.
   */
  private static final Logger LOG = LoggerFactory.getLogger(ThresholdOrderGenTrigger.class);
  /**
   * Where we get events from.从那里获取事件。
   */
  private final EventSource eventSource;
  /**
   * The object service we talk to.
   */
  private final TCSObjectService objectService;
  /**
   * The orders that we know are in the system.
   */
  private final Set<TransportOrder> knownOrders = new LinkedHashSet<>();
  /**
   * The threshold for order generation. If the number of orders "in progress"
   * drops to or below this number, a batch of new orders is generated.
   * 订单生成的闻值。如果“进行中”的订单数量下降到或低于此数量，则会生成一批新订单。
   */
  private final int threshold;
  /**
   * The instance actually creating the new orders.
   */
  private final OrderBatchCreator orderBatchCreator;

  /**
   * Creates a new instance.
   *
   * @param eventSource Where this instance registers for events.
   * @param objectService The object service.
   * @param threshold The threshold when new order are being created
   * @param orderBatchCreator The order batch creator
   */
  public ThresholdOrderGenTrigger(final @ApplicationEventBus EventSource eventSource,
                                  final TCSObjectService objectService,
                                  final int threshold,
                                  final OrderBatchCreator orderBatchCreator) {
    this.eventSource = requireNonNull(eventSource, "eventSource");
    this.objectService = requireNonNull(objectService, "objectService");
    this.threshold = threshold;
    this.orderBatchCreator = requireNonNull(orderBatchCreator, "orderBatchCreator");
  }

  @Override
  public void setTriggeringEnabled(boolean enabled) {
    synchronized (knownOrders) {
      if (enabled) {
        // Remember all orders that are not finished, failed etc.
        //记住所有未完成的订单，失败的订单等。
        for (TransportOrder curOrder : objectService.fetchObjects(TransportOrder.class)) {
          if (!curOrder.getState().isFinalState()) {
            knownOrders.add(curOrder);
          }
        }
        eventSource.subscribe(this);
        if (knownOrders.size() <= threshold) {
          triggerOrderGeneration();
        }
      }
      else {
        eventSource.unsubscribe(this);
        knownOrders.clear();
      }
    }
  }

  @Override
  public void triggerOrderGeneration()
      throws KernelRuntimeException {
    knownOrders.addAll(orderBatchCreator.createOrderBatch());
  }

  @Override
  public void onEvent(Object event) {
    if (!(event instanceof TCSObjectEvent)) {
      return;
    }
    TCSObjectEvent objEvent = (TCSObjectEvent) event;
    if (!(objEvent.getCurrentOrPreviousObjectState() instanceof TransportOrder)) {
      return;
    }

    synchronized (knownOrders) {
      TransportOrder eventOrder
          = (TransportOrder) objEvent.getCurrentOrPreviousObjectState();
      // If a new order was created, add it to the set of known orders.
      if (TCSObjectEvent.Type.OBJECT_CREATED.equals(objEvent.getType())) {
        knownOrders.add(eventOrder);
      }
      // If an order was removed, remove it here, too.
      else if (TCSObjectEvent.Type.OBJECT_REMOVED.equals(objEvent.getType())) {
        knownOrders.remove(eventOrder);
      }
      // If an order was modified, check if it's NOT "in progress". If it's not,
      // i.e. if it's now finished, failed etc., remove it here, too.
      else if (eventOrder.getState().isFinalState()) {
        knownOrders.remove(eventOrder);
      }
      // Now let's check if the number of orders "in progress" has dropped below
      // the threshold. If so, create a new batch of orders.
      //现在让我们检查“进行中”的订单数量是否已经下降到闯值以下。如果是创建一批新的订单。
      if (knownOrders.size() <= threshold) {
        LOG.debug("orderCount = " + knownOrders.size() + ", triggering...");
        trigger();
      }
    }
  }

  private void trigger() {
    try {
      triggerOrderGeneration();
    }
    catch (KernelRuntimeException exc) {
      LOG.warn("Exception triggering order generation, terminating triggering", exc);
      setTriggeringEnabled(false);

    }
  }

}
