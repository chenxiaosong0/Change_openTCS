package org.opentcs.guing.plugins.panels.loadgenerator;

import org.opentcs.access.KernelRuntimeException;
import org.opentcs.access.KernelServicePortal;
import org.opentcs.access.SharedKernelServicePortal;
import org.opentcs.access.SharedKernelServicePortalProvider;
import org.opentcs.access.rmi.KernelServicePortalBuilder;
import org.opentcs.access.to.order.DestinationCreationTO;
import org.opentcs.access.to.order.OrderSequenceCreationTO;
import org.opentcs.access.to.order.TransportOrderCreationTO;
import org.opentcs.components.kernel.services.DispatcherService;
import org.opentcs.components.kernel.services.ServiceUnavailableException;
import org.opentcs.components.kernel.services.TCSObjectService;
import org.opentcs.components.kernel.services.TransportOrderService;
import org.opentcs.components.plantoverview.PluggablePanel;
import org.opentcs.customizations.ApplicationEventBus;
import org.opentcs.data.model.Location;
import org.opentcs.data.order.DriveOrder;
import org.opentcs.data.order.TransportOrder;
import org.opentcs.guing.plugins.panels.loadgenerator.batchcreator.MyOrderBatchGenerator;
import org.opentcs.guing.plugins.panels.loadgenerator.batchcreator.OrderBatchCreator;
import org.opentcs.guing.plugins.panels.loadgenerator.batchcreator.RandomOrderBatchCreator;
import org.opentcs.guing.plugins.panels.loadgenerator.trigger.OrderGenerationTrigger;
import org.opentcs.guing.plugins.panels.loadgenerator.trigger.ThresholdOrderGenTrigger;
import org.opentcs.util.event.EventSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.*;

import static java.util.Objects.requireNonNull;

/**
 * @author xiaosongChen
 * @create 2023-11-16 11:07
 * @description :
 */
public class MyOrderPanel extends PluggablePanel {
  private static final Logger LOG = LoggerFactory.getLogger(ContinuousLoadPanel.class);
  private boolean initialized;
  private  List<Location> locations;
  private TCSObjectService objectService;
  private List<TransportOrderData> data;//自定义的TransportOrderData数据
  private volatile OrderGenerationTrigger orderGenTrigger;//触发器

  private final SharedKernelServicePortalProvider portalProvider;
  private  EventSource eventSource;
  private SharedKernelServicePortal sharedPortal;
//  private final KernelServicePortal kernelServicePortal;


//  public static void main(String[] args) {
//    Injector injector = Guice.createInjector(new MyOrderModule());
//    MyOrder myOrder = injector.getInstance(MyOrder.class);
//    myOrder.initComponents();
//
//  }

  @Inject
  public MyOrderPanel(SharedKernelServicePortalProvider portalProvider,
                      @ApplicationEventBus EventSource eventSource) {
//    kernelServicePortal = new KernelServicePortalBuilder("Alice","xyz").build();
//    kernelServicePortal.login("127.0.0.1",1099);
    this.portalProvider = requireNonNull(portalProvider, "portalProvider");
    this.eventSource = requireNonNull(eventSource, "eventSource");

    initComponents();
  }


  @Override
  public void initialize() {
    LOG.info("启动MyOrderPanel");
    if (isInitialized()) {
      return;
    }
    // Get a kernel reference.
    try {
      sharedPortal = portalProvider.register();
    }
    catch (ServiceUnavailableException exc) {
      LOG.warn("Kernel unavailable", exc);
      return;
    }
    objectService = (TCSObjectService) sharedPortal.getPortal().getPlantModelService();

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
    initialized = false;
  }

  /**
   * 连续生成订单
   */
  public void orderGenStateChanged(ItemEvent evt){
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
//    return new RandomOrderBatchCreator(
//        sharedPortal.getPortal().getTransportOrderService(),
//        sharedPortal.getPortal().getDispatcherService(),
//        1,
//        1);
    return new MyOrderBatchGenerator(sharedPortal.getPortal().getTransportOrderService(),
        sharedPortal.getPortal().getDispatcherService());
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

        TransportOrder newOrder = sharedPortal.getPortal().getTransportOrderService().createTransportOrder(
            new TransportOrderCreationTO("TOrder-", dests).withIncompleteName(true)//运输订单描述
        );
        return newOrder;

      }

      private void initComponents(){
        orderGenChkBox = new JCheckBox();
        orderGenPanel = new JPanel();
        orderGenPanel.setLayout(new GridBagLayout());
        orderGenChkBox.setText("GeneralCreate");
        orderGenChkBox.addItemListener(new java.awt.event.ItemListener() {
          public void itemStateChanged(ItemEvent evt) {
            LOG.info("GeneralCreate 勾选框触发");
            orderGenStateChanged(evt);//触发是否生成持续的订单
          }
        });
        orderGenPanel.add(orderGenChkBox);
        add(orderGenPanel, BorderLayout.CENTER);

      }

  private JCheckBox orderGenChkBox;
  private JPanel orderGenPanel;


}
