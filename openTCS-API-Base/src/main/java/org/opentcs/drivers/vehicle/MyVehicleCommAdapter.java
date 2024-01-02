package org.opentcs.drivers.vehicle;

import java.util.List;
import java.util.concurrent.Executor;
import javax.annotation.Nullable;
import org.opentcs.util.ExplainedBoolean;

/**
 * @author xiaosongChen
 * @create 2023-11-13 16:36
 * @description :
 */
public class MyVehicleCommAdapter extends BasicVehicleCommAdapter{

  private boolean isConnected;

  public MyVehicleCommAdapter(VehicleProcessModel vehicleModel){

    super(vehicleModel,2,2,"charge",null);

  }


  @Override
  public ExplainedBoolean canProcess(List<String> operations) {
    //默认可以处理所有操作,实际操作是否支持需要根据项目的情况来判断
    return new ExplainedBoolean(true,"");
  }

  @Override
  public void sendCommand(MovementCommand cmd)
    //这里处理实际的运动指令，运动指令被包装到了MovementCommand这个类中。你需要提取Path然后做响应的处理。

  throws IllegalArgumentException {

  }

  @Override
  protected void connectVehicle() {
    //这里需要连接具体的Vehicle。如果你的Vehicle是TCP接口的那就这里进行Socket连接。
    //如果是其他接口的也在这里进行网络连接

    isConnected = true;
  }

  @Override
  protected void disconnectVehicle() {
    //这里需要断开具体的Vehicle连接。如果你的Vehicle是TCP接口的那就这里断开Socket连接。
    //如果是其他接口的也在这里断开连接

    isConnected = false;
  }

  @Override
  protected boolean isVehicleConnected() {
    // 设备是否已连接
    return isConnected;
  }

  @Override
  public void processMessage(@Nullable Object message) {

  }
}
