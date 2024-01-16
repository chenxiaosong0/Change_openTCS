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
import org.opentcs.data.model.Location;
import org.opentcs.data.order.DriveOrder;
import org.opentcs.data.order.TransportOrder;
import org.opentcs.guing.plugins.panels.loadgenerator.DriveOrderStructure;
import org.opentcs.guing.plugins.panels.loadgenerator.TransportOrderData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

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
  private Map<String,Long> emptyLocation = new TreeMap<>(new MyComparator());
  private Map<String,Long> fullLocation = new TreeMap<>(new MyComparator());

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
      initLocations();
  }

  private void initLocations(){
    Set<Location> locations = transportOrderService.fetchObjects(Location.class);

    for (Location location : locations) {//获取地图的Location
      if(location.getProperties().containsKey("isFull")){
        if(location.getProperty("isFull").contentEquals("true")){//true
          fullLocation.put(location.getName(),System.currentTimeMillis());
        }else{//false
          emptyLocation.put(location.getName(),System.currentTimeMillis());
        }
      }
    }

  }

  private String handleLocations(Map<String,Long> map){
    //1：把map转换成entryset，再转换成保存Entry对象的list。
    List<Map.Entry<String,Long>> entrys=new ArrayList<>(map.entrySet());
    if (entrys == null){
      LOG.warn(" ***** entrys is null!");
      return null;
    }
    if(!entrys.iterator().hasNext()){
      LOG.warn(" ***** entrys is empty!");
      return null;
    }
    Collections.sort(entrys, new Comparator<Map.Entry<String, Long>>() {
      @Override//小-> 大
      public int compare(Map.Entry<String, Long> o1, Map.Entry<String, Long> o2) {
        return o1.getValue().compareTo(o2.getValue());//返回负数则o1在前，正数则o2在前
      }
    });
    String first = entrys.get(0).getKey();
    System.out.println("fullLocation.get(0).getKey() = " + first);
    map.remove(first);//移除
    return first;

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
    TransportOrder singleOrder = createSingleOrder();
    if(singleOrder == null){
      LOG.warn(" ***** createOrderBatch() return null!");
      return null;
    }
    createdOrders.add(singleOrder);
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
    List<DestinationCreationTO> toList = createDestinations();
    if(toList == null){
      LOG.warn(" ***** createSingleOrder() return null!");
      return null;
    }
    TransportOrder newOrder = transportOrderService.createTransportOrder(
        new TransportOrderCreationTO("TOrder-", toList)
            .withIncompleteName(true)
            .withDeadline(Instant.now().plus(1, ChronoUnit.HOURS))
            .withIntendedVehicleName(null));

    return newOrder;
  }

  /**
   * return a List : the DirverOrder and its Operation
   * @param
   * @return
   */
  private List<DestinationCreationTO> createDestinations() {
    List<DestinationCreationTO> result = new ArrayList<>();
    String locations = handleLocations(fullLocation);
    if (locations == null){
      LOG.warn(" ***** createDestinations() return null!");
      return null;
    }
    //如何设置Location属性
    result.add(new DestinationCreationTO(locations,"Load").withProperty("isFull","false"));
    result.add(new DestinationCreationTO("robotLocation","unLoad"));
    result.add(new DestinationCreationTO("whileLocation", DriveOrder.Destination.OP_NOP));

    return result;
  }

  private void  Listener(){


  }
  public Map<String, Long> getFullLocation() {
    return fullLocation;
  }

  public Map<String, Long> getEmptyLocation() {
    return emptyLocation;
  }
}
class MyComparator implements Comparator<String>{
  @Override
  public int compare(String o1, String o2) {
    return o2.compareTo(o1);//大 -> 小
  }
}
