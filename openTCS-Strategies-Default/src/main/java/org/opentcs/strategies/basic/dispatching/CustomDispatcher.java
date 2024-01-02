package org.opentcs.strategies.basic.dispatching;

import javax.annotation.Nonnull;
import org.opentcs.components.kernel.Dispatcher;
import org.opentcs.data.model.Vehicle;
import org.opentcs.data.order.TransportOrder;

/**
 * @author xiaosongChen
 * @create 2023-11-29 14:34
 * @description :
 */
public class CustomDispatcher implements Dispatcher {
  @Override
  public void initialize() {

  }

  @Override
  public boolean isInitialized() {
    return false;
  }

  @Override
  public void terminate() {

  }

  @Override
  public void dispatch() {

  }

  @Override
  public void withdrawOrder(@Nonnull TransportOrder order, boolean immediateAbort) {

  }

  @Override
  public void withdrawOrder(@Nonnull Vehicle vehicle, boolean immediateAbort) {

  }

  @Override
  public void topologyChanged() {

  }
}
