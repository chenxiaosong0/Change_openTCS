/**
 * Copyright (c) The openTCS Authors.
 *
 * This program is free software and subject to the MIT license. (For details,
 * see the licensing information (LICENSE.txt) you should have received with
 * this copy of the software.)
 */
package org.opentcs.guing.plugins.panels.loadgenerator;

import org.opentcs.configuration.ConfigurationEntry;
import org.opentcs.configuration.ConfigurationPrefix;

/**
 * Provides methods to configure the continuous load panel.提供配置连续负载面板的方法
 */
@ConfigurationPrefix(ContinuousLoadPanelConfiguration.PREFIX)
public interface ContinuousLoadPanelConfiguration {

  /**
   * This configuration's prefix.
   */
  String PREFIX = "continuousloadpanel";

  @ConfigurationEntry(
      type = "Boolean",
      description = "Whether to enable to register/enable the continuous load panel.是否启用注册/启用连续加载面板",
      orderKey = "0_enable")

  boolean enable();
}
