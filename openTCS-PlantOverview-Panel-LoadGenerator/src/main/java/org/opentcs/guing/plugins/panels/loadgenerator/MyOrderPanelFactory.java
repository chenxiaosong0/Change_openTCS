/**
 * Copyright (c) The openTCS Authors.
 *
 * This program is free software and subject to the MIT license. (For details,
 * see the licensing information (LICENSE.txt) you should have received with
 * this copy of the software.)
 */
package org.opentcs.guing.plugins.panels.loadgenerator;

import org.opentcs.access.Kernel;
import org.opentcs.access.SharedKernelServicePortalProvider;
import org.opentcs.components.plantoverview.PluggablePanel;
import org.opentcs.components.plantoverview.PluggablePanelFactory;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.ResourceBundle;

import static java.util.Objects.requireNonNull;
import static org.opentcs.guing.plugins.panels.loadgenerator.I18nPlantOverviewPanelLoadGenerator.BUNDLE_PATH;

/**
 * Creates load generator panels.创建负载发生器面板。
 */
public class MyOrderPanelFactory
    implements PluggablePanelFactory {

  /**
   * This classe's bundle.
   */
  private final ResourceBundle bundle = ResourceBundle.getBundle(BUNDLE_PATH);
  /**
   * A reference to the shared portal provider.
   */
  private final SharedKernelServicePortalProvider portalProvider;
  /**
   * A provider for the actual panels.
   */
  private final Provider<MyOrderPanel> panelProvider;

  /**
   * Creates a new instance.
   *
   * @param portalProvider The application's portal provider.
   * @param panelProvider A provider for the actual panels.
   */
  @Inject
  public MyOrderPanelFactory(SharedKernelServicePortalProvider portalProvider,
                             Provider<MyOrderPanel> panelProvider) {
    this.portalProvider = requireNonNull(portalProvider, "portalProvider");
    this.panelProvider = requireNonNull(panelProvider, "panelProvider");
  }

  @Override
  public String getPanelDescription() {
    return bundle.getString("continuousLoadPanelFactory.panelDescription");
  }

  @Override
  public PluggablePanel createPanel(Kernel.State state) {
    if (!providesPanel(state)) {
      return null;
    }

    return panelProvider.get();
  }

  @Override
  public boolean providesPanel(Kernel.State state) {
    return portalProvider != null && Kernel.State.OPERATING.equals(state);
  }
}
