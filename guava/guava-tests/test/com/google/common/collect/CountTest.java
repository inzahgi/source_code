/*
 * Copyright (C) 2011 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;
import junit.framework.TestCase;

/**
 * Tests for {@code Count}.
 *
 * @author Louis Wasserman
 */
@GwtCompatible
public class CountTest extends TestCase {
  //get取当前值
  public void testGet() {
    assertEquals(20, new Count(20).get());
  }

  //先get再add指定值
  public void testGetAndAdd() {
    Count holder = new Count(20);
    assertEquals(20, holder.get());
    holder.add(1);
    assertEquals(21, holder.get());
  }

  //先add再get指定值
  public void testAddAndGet() {
    Count holder = new Count(20);
    assertEquals(21, holder.addAndGet(1));
  }

  //先get在set
  public void testGetAndSet() {
    Count holder = new Count(10);
    assertEquals(10, holder.getAndSet(20));
    assertEquals(20, holder.get());
  }

  //直接set方法
  public void testSet() {
    Count holder = new Count(10);
    holder.set(20);
    assertEquals(20, holder.get());
  }
}
