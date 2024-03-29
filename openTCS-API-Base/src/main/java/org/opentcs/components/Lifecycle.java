/**
 * Copyright (c) The openTCS Authors.
 *
 * This program is free software and subject to the MIT license. (For details,
 * see the licensing information (LICENSE.txt) you should have received with
 * this copy of the software.)
 */
package org.opentcs.components;

/**
 * Defines methods for controlling a generic component's lifecycle.定义控制通用组件生命周期的方法。
 */
public interface Lifecycle {

  /**
   * (Re-)Initializes this component before it is being used.(Re-)在使用该组件之前初始化该组件。
   */
  void initialize();

  /**
   * Checks whether this component is initialized.
   *
   * @return <code>true</code> if, and only if, this component is initialized.
   */
  boolean isInitialized();

  /**
   * Terminates the instance and frees resources.
   */
  void terminate();
}
