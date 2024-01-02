package org.opentcs.drivers;


import java.util.concurrent.Executor;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opentcs.data.model.Vehicle;
import org.opentcs.drivers.vehicle.*;

/**
 * @author xiaosongChen
 * @create 2023-11-13 16:41
 * @description :
 */
public class MyVehicleCommAdapterFactory
    implements VehicleCommAdapterFactory {

  public MyVehicleCommAdapterFactory() {
  }

  @Override
  public void initialize() {

  }

  @Override
  public boolean isInitialized() {
    return true;
  }

  @Override
  public void terminate() {

  }

  @Override
  public VehicleCommAdapterDescription getDescription() {
    //这里返回一个Adapter描述对象,这里其实是得到一个字符串用于标识你的Adapter名称
    return new VehicleCommAdapterDescription(){
      @Override
      public String getDescription() {
        return "MyVehicle";
      }

      @Override
      public boolean isSimVehicleCommAdapter() {
        return true;
      }
    };
  }

  @Override
  public boolean providesAdapterFor(@Nonnull Vehicle vehicle) {
    return true;
  }


  @Override
  public VehicleCommAdapter getAdapterFor( Vehicle vehicle) {
    //这里返回一个Adapter实例
    return new MyVehicleCommAdapter(new VehicleProcessModel(vehicle));
  }
}
