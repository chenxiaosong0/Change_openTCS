package org.opentcs.operationsdesk;

import java.awt.*;
import java.awt.event.ItemEvent;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.List;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.opentcs.access.KernelRuntimeException;
import org.opentcs.access.KernelServicePortal;
import org.opentcs.access.SharedKernelServicePortalProvider;
import org.opentcs.access.rmi.KernelServicePortalBuilder;
import org.opentcs.access.to.order.DestinationCreationTO;
import org.opentcs.access.to.order.OrderSequenceCreationTO;
import org.opentcs.access.to.order.TransportOrderCreationTO;
import org.opentcs.components.kernel.services.*;
import org.opentcs.components.plantoverview.PluggablePanel;
import org.opentcs.customizations.ApplicationEventBus;
import org.opentcs.data.model.Location;
import org.opentcs.data.model.Path;
import org.opentcs.data.model.Point;
import org.opentcs.data.model.TCSResourceReference;
import org.opentcs.data.order.DriveOrder;
import org.opentcs.data.order.TransportOrder;
//import org.opentcs.guing.base.components.properties.type.Relation;
import org.opentcs.guing.plugins.panels.loadgenerator.ContinuousLoadPanel;
import org.opentcs.guing.plugins.panels.loadgenerator.DriveOrderStructure;
import org.opentcs.guing.plugins.panels.loadgenerator.TransportOrderData;
import org.opentcs.guing.plugins.panels.loadgenerator.batchcreator.OrderBatchCreator;
import org.opentcs.guing.plugins.panels.loadgenerator.batchcreator.RandomOrderBatchCreator;
import org.opentcs.guing.plugins.panels.loadgenerator.trigger.OrderGenerationTrigger;
import org.opentcs.guing.plugins.panels.loadgenerator.trigger.ThresholdOrderGenTrigger;
//import org.opentcs.operationsdesk.application.PlantOverviewStarter;
import org.opentcs.util.event.EventSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.swing.*;

import static java.util.Objects.requireNonNull;

/**
 * @author xiaosongChen
 * @create 2023-11-16 11:07
 * @description :
 */
public class MyOrder  extends PluggablePanel {
  private static final Logger LOG = LoggerFactory.getLogger(ContinuousLoadPanel.class);
  private boolean initialized;
  private  List<Location> locations;
  private TCSObjectService objectService;
  private List<TransportOrderData> data;//自定义的TransportOrderData数据
  private volatile OrderGenerationTrigger orderGenTrigger;//触发器
  private final TransportOrderService transportOrderService;
  private final DispatcherService dispatcherService;
  private  EventSource eventSource;
  private final KernelServicePortal kernelServicePortal;
  private Map<String,Long> emptyLocation = new TreeMap<>(new MyComparator()),fullLocation = new TreeMap<>(new MyComparator());

  public static void main(String[] args) {
    Injector injector = Guice.createInjector(new MyOrderModule());
    MyOrder myOrder = injector.getInstance(MyOrder.class);
    myOrder.getMeaasge();

  }

//  @Inject
//  public MyOrder(SharedKernelServicePortalProvider portalProvider,
//                 @ApplicationEventBus EventSource eventSource) {
//    kernelServicePortal = new KernelServicePortalBuilder("Alice","xyz").build();
//    kernelServicePortal.login("127.0.0.1",1099);
//    this.portalProvider = requireNonNull(portalProvider, "portalProvider");
//    this.objectService = kernelServicePortal.getPlantModelService();
//    this.transportOrderService = kernelServicePortal.getTransportOrderService();
//    this.dispatcherService = kernelServicePortal.getDispatcherService();
//    this.eventSource = requireNonNull(eventSource, "eventSource");
//    locations = new ArrayList<>(transportOrderService.fetchObjects(Location.class));
//  }
  @Inject
  public MyOrder( @ApplicationEventBus EventSource eventSource) {
    kernelServicePortal = new KernelServicePortalBuilder("Alice","xyz").build();
    kernelServicePortal.login("127.0.0.1",1099);
//    this.portalProvider = requireNonNull(portalProvider, "portalProvider");
    this.objectService = kernelServicePortal.getPlantModelService();
    this.transportOrderService = kernelServicePortal.getTransportOrderService();
    this.dispatcherService = kernelServicePortal.getDispatcherService();
    this.eventSource = requireNonNull(eventSource, "eventSource");
    locations = new ArrayList<>(transportOrderService.fetchObjects(Location.class));
  }

  @Override
  public void initialize() {
    LOG.info("启动MyOrder");
    if (isInitialized()) {
      return;
    }

    initialized = true;
  }

  @Override
  public boolean isInitialized() {
    return initialized;
  }

  @Override
  public void terminate() {
    if (!isInitialized()) {
      return;
    }

    LOG.info("注销/清除到远程内核服务门户的连接");
    kernelServicePortal.logout();
    initialized = false;
  }

  /**
   * 添加单个订单
   */
  public void getMeaasge()
  {
//    DispatcherService dispatcherService = kernelServicePortal.getDispatcherService();
    PlantModelService plantModelService = kernelServicePortal.getPlantModelService();
    Set<Path> paths = plantModelService.fetchObjects(Path.class);

    System.out.println("获取运输订单服务---------");
    TransportOrderService transportOrderService = kernelServicePortal.getTransportOrderService();
    locations = new ArrayList<>(transportOrderService.fetchObjects(Location.class));
//    int maxNum = 0;
//    Map<Integer,Location> map = new HashMap();
//    for (Location location : locations) {//获取地图的Location
//      if(location.getProperties().containsKey("isFull")){
//        if(location.getProperty("isFull").contentEquals("true")){
//          Integer num = Integer.parseInt(location.getName().substring(9,13));
//          map.put(num,location);
//          maxNum = Math.max(num,maxNum);
//
//        }
//      }
//    }
    int i = 0;
    for (Location location : locations) {//获取地图的Location
      if(location.getProperties().containsKey("isFull")){
        i++;
        if(location.getProperty("isFull").contentEquals("true")){//true
          fullLocation.put(location.getName(),System.currentTimeMillis() +(int)(Math.random()*10));
        }else{//false
          emptyLocation.put(location.getName(),System.currentTimeMillis() + i);
        }
      }
    }

    List<String> fullList = new ArrayList<>();
    //Stack<String> stack = new Stack<>();
    Set<String> keySet = fullLocation.keySet();
    Iterator<String> iterator = keySet.iterator();
    long current = System.currentTimeMillis();
    String small = "Location-0000";

    //1：把map转换成entryset，再转换成保存Entry对象的list。
    List<Map.Entry<String,Long>> entrys=new ArrayList<>(fullLocation.entrySet());
    Collections.sort(entrys, new Comparator<Map.Entry<String, Long>>() {
      @Override
      public int compare(Map.Entry<String, Long> o1, Map.Entry<String, Long> o2) {
        return o1.getValue().compareTo(o2.getValue());//返回负数则o1在前，正数则o2在前
      }
    });
    while (iterator.hasNext()){
      String next = iterator.next();
      System.out.println(next + ": " + fullLocation.get(next));
      //stack.push(next);
    }
    System.out.println("------------------");
    for (Map.Entry<String, Long> entry : entrys) {
      System.out.println(entry.getKey()+", "+entry.getValue());
    }

    //System.out.println(stack);







//      for(Path path:paths){
//        if(path.getSourcePoint().equals(linkPoint)){
//          System.out.println("source: " + path.getName());
//        } else if (path.getDestinationPoint().equals(linkPoint)) {
//          System.out.println("destination: " + path.getName());
//        }
//      }


//    List<DestinationCreationTO> destinations = new LinkedList<>();
//    System.out.println("将目的地添加到列表中---------");
//    destinations.add(new DestinationCreationTO("Location-0001",
//                                               "Type2Load"));
//    destinations.add(new DestinationCreationTO("robotLocation",
//                                               "unLoad"));


//    String orderName = "OSystem-" + UUID.randomUUID();
//    TransportOrderCreationTO transportOrderCreationTO = new TransportOrderCreationTO("testCreatTransportOrder", destinations);
//    transportOrderCreationTO = transportOrderCreationTO.withIncompleteName(true);
//    OrderSequenceCreationTO sequenceTO = new OrderSequenceCreationTO("MyOrderSequence");
//    System.out.println("获取小车服务对象*******");
//    VehicleService vehicleService = kernelServicePortal.getVehicleService();
//    String vehicleName = "Vehicle-0001";
//    Vehicle vehicle = vehicleService.fetchObject(Vehicle.class, vehicleName);

//    transportOrderCreationTO = transportOrderCreationTO.withIntendedVehicleName(vehicleName);
//    transportOrderCreationTO = transportOrderCreationTO.withDeadline(Instant.now().plus(5, ChronoUnit.SECONDS));

//    transportOrderService.createTransportOrder(transportOrderCreationTO);
//    OrderSequence orderSequence = transportOrderService.createOrderSequence(sequenceTO);
//    dispatcherService.dispatch();
//    kernelServicePortal.logout();
  }

  /**
   * 连续生成订单
   */
  public void orderGenStateChanged(java.awt.event.ItemEvent evt){
    if (evt.getStateChange() == ItemEvent.SELECTED) {
      orderGenTrigger = createOrderGenTrigger(1);//订单生成触发器
      if (orderGenTrigger == null) {
        return;
      }
      orderGenTrigger.setTriggeringEnabled(true);//启用生成订单

    } else if (orderGenTrigger != null) {
      orderGenTrigger.setTriggeringEnabled(false);
      orderGenTrigger = null;
    }

  }

  //返回新的订单生成触发器（降低到阈值threshold后触发）
  private OrderGenerationTrigger createOrderGenTrigger(int threshold) {
    OrderBatchCreator batchCreator = createOrderBatchCreator();//创建批次订单
    if (batchCreator == null) {
      return null;
    }
    LOG.info("新的订单生成触发器（降低到阈值: {} 后触发）",threshold);
    //返回生成触发器
    return new ThresholdOrderGenTrigger(eventSource,
        objectService,
        threshold,
        batchCreator);
  }

  //返回指定数据顺序的订单批次创建器
  private OrderBatchCreator createOrderBatchCreator() {
    Location location = null;
    TransportOrderData orderData = new TransportOrderData();//数据
    List<DriveOrderStructure> driveOrders = orderData.getDriveOrders();
    DriveOrderStructure structure = new DriveOrderStructure();

    //返回创建订单对象
//    return new ExplicitOrderBatchGenerator(transportOrderService,
//        dispatcherService,
//        data);
    //返回随机批次订单
    return new RandomOrderBatchCreator(transportOrderService,
        dispatcherService,
        2,
        3);
  }

  private Set<TransportOrder> creatOrderList(int batchSize){
    Set<TransportOrder> orders = new HashSet<>();
    for (int i = 0; i < batchSize; i++) {
      orders.add(createOrder());//添加几个生成的订单
    }
    return orders;
  }

  private TransportOrder createOrder()//生成单个订单
      throws KernelRuntimeException {
        List<DestinationCreationTO> dests = new ArrayList<>();//目的地列表
            dests.add(new DestinationCreationTO(locations.get(0).getName(),
                DriveOrder.Destination.OP_NOP));//到目的地无操作
            dests.add(new DestinationCreationTO(locations.get(1).getName(),
                DriveOrder.Destination.OP_NOP));//到目的地无操作
            dests.add(new DestinationCreationTO(locations.get(2).getName(),
                DriveOrder.Destination.OP_NOP));//到目的地无操作

        TransportOrder newOrder = transportOrderService.createTransportOrder(
            new TransportOrderCreationTO("TOrder-", dests).withIncompleteName(true)//运输订单描述
        );
        return newOrder;

      }

      private void initComponents(){
        orderGenChkBox = new javax.swing.JCheckBox();
        orderGenPanel = new javax.swing.JPanel();
        frame = new JFrame("Test General Create Orders");
        orderGenPanel.setLayout(new java.awt.GridBagLayout());
        orderGenChkBox.setText("GeneralCreate");
        orderGenChkBox.addItemListener(new java.awt.event.ItemListener() {
          public void itemStateChanged(java.awt.event.ItemEvent evt) {
            System.out.println("GeneralCreate 勾选框触发");
            orderGenStateChanged(evt);//触发是否生成持续的订单
          }
        });
        orderGenPanel.add(orderGenChkBox);
        add(orderGenPanel, BorderLayout.CENTER);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);//设置:关闭窗口，则程序退出
        frame.setSize(500,100);
        frame.setContentPane(orderGenPanel);
        frame.setVisible(true);//显示窗口
      }

  private javax.swing.JCheckBox orderGenChkBox;
  private javax.swing.JPanel orderGenPanel;
  private javax.swing.JFrame frame;

}
class MyComparator implements Comparator<String>{
  @Override
  public int compare(String o1, String o2) {
    return o2.compareTo(o1);//大 -> 小
  }
}
