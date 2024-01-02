/**
 * Copyright (c) The openTCS Authors.
 *
 * This program is free software and subject to the MIT license. (For details,
 * see the licensing information (LICENSE.txt) you should have received with
 * this copy of the software.)
 */
package org.opentcs.guing.plugins.panels.loadgenerator.batchcreator;

import org.opentcs.access.KernelRuntimeException;
import org.opentcs.access.to.order.DestinationCreationTO;
import org.opentcs.access.to.order.TransportOrderCreationTO;
import org.opentcs.components.kernel.services.DispatcherService;
import org.opentcs.components.kernel.services.TransportOrderService;
import org.opentcs.data.order.DriveOrder;
import org.opentcs.data.order.TransportOrder;
import org.opentcs.guing.plugins.panels.loadgenerator.DriveOrderStructure;
import org.opentcs.guing.plugins.panels.loadgenerator.TransportOrderData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.Objects.requireNonNull;

/**
 * A batch generator for creating explicit transport orders.用于创建显式传输订单的批处理生成器
 */
public class MyOrderBatchGenerator
    implements OrderBatchCreator {

  /**
   * This class's Logger.
   */
  private static final Logger LOG = LoggerFactory.getLogger(MyOrderBatchGenerator.class);
  /**
   * The transport order service we talk to.
   */
  private final TransportOrderService transportOrderService;
  /**
   * The dispatcher service.
   */
  private final DispatcherService dispatcherService;
  /**
   * The TransportOrderData we're building the transport orders from.
   */
//  private final List<TransportOrderData> data;
  private  List<String> locationName = new ArrayList<>();
  private static int index = 1;

  /**
   * Creates a new ExplicitOrderBatchGenerator.
   *
   * @param transportOrderService The portal.
   * @param dispatcherService The dispatcher service.
   *
   */
  public MyOrderBatchGenerator(TransportOrderService transportOrderService,
                               DispatcherService dispatcherService) {
    this.transportOrderService = requireNonNull(transportOrderService, "transportOrderService");
    this.dispatcherService = requireNonNull(dispatcherService, "dispatcherService");
//    this.data = requireNonNull(data, "data");
  }

  private List<TransportOrderData> setData(){
    TransportOrderData data1 = new TransportOrderData();

    return  null;
  }

  /**
   * 依次创建多个订单，返回一个Set订单集合
   * @return
   * @throws KernelRuntimeException
   */
  @Override
  public Set<TransportOrder> createOrderBatch()
      throws KernelRuntimeException {
    Set<TransportOrder> createdOrders = new HashSet<>();

    createdOrders.add(createSingleOrder());

    dispatcherService.dispatch();

    return createdOrders;
  }

  /**
   * 根据指定顺序的返回单个订单
   * @param
   * @return
   * @throws KernelRuntimeException
   */
  private TransportOrder createSingleOrder()
      throws KernelRuntimeException {

    TransportOrder newOrder = transportOrderService.createTransportOrder(
        new TransportOrderCreationTO("TOrder-",
                                     createDestinations())
            .withIncompleteName(true)
            .withDeadline(Instant.now().plus(1, ChronoUnit.HOURS))
            .withIntendedVehicleName(null));

    return newOrder;
  }

  /**
   * 返回经过的DirverOrder和对应的Operation组成的List
   * @param
   * @return
   */
  private List<DestinationCreationTO> createDestinations() {
    List<DestinationCreationTO> result = new ArrayList<>();
    String name = null;

    if(index < 10){
      name = "Location-000" + index;
    }else if(index < 25){
      name = "Location-00" + index;
    }
    index++;
    if (index == 25){
      index = 1;
    }
    //如何添加外围作业
    result.add(new DestinationCreationTO(name,"Load"));
    result.add(new DestinationCreationTO("robotLocation","unLoad"));
    result.add(new DestinationCreationTO("whileLocation", DriveOrder.Destination.OP_NOP));

    return result;
  }
}
