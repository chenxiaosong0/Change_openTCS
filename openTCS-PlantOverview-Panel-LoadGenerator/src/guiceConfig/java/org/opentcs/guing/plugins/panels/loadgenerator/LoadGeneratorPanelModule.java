/**
 * Copyright (c) The openTCS Authors.
 *
 * This program is free software and subject to the MIT license. (For details,
 * see the licensing information (LICENSE.txt) you should have received with
 * this copy of the software.)
 */
package org.opentcs.guing.plugins.panels.loadgenerator;

import org.opentcs.customizations.plantoverview.PlantOverviewInjectionModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Configures the load generator panel.配置负载发生器面板。
 */
public class LoadGeneratorPanelModule
    extends PlantOverviewInjectionModule {

  /**
   * This class's logger.
   */
  private static final Logger LOG = LoggerFactory.getLogger(LoadGeneratorPanelModule.class);

  /**
   * Creates a new instance.
   */
  public LoadGeneratorPanelModule() {
  }

  @Override
  protected void configure() {
    ContinuousLoadPanelConfiguration configuration
        = getConfigBindingProvider().get(ContinuousLoadPanelConfiguration.PREFIX,
                                         ContinuousLoadPanelConfiguration.class);

    if (!configuration.enable()) {
      LOG.info("Continuous load panel disabled by configuration.连续加载面板被配置禁用");//
      return;
    }
    // tag::documentation_createPluginPanelModule[]
    pluggablePanelFactoryBinder().addBinding().to(ContinuousLoadPanelFactory.class);
//    pluggablePanelFactoryBinder().addBinding().to(MyOrderPanelFactory.class);

    // end::documentation_createPluginPanelModule[]
  }
}
