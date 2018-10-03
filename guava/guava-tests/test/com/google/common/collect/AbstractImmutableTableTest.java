/*
 * Copyright (C) 2009 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;
import junit.framework.TestCase;

/**
 * Tests {@link ImmutableTable}
 *
 * @author Gregory Kick
 */
@GwtCompatible
public abstract class AbstractImmutableTableTest extends TestCase {

  abstract Iterable<ImmutableTable<Character, Integer, String>> getTestInstances();

  //immutabletable 不能清空
  public final void testClear() {
    for (Table<Character, Integer, String> testInstance : getTestInstances()) {
      try {
        testInstance.clear();
        fail();
      } catch (UnsupportedOperationException e) {
        e.printStackTrace();
        // success
      }
    }
  }

  //immutableTable不能添加单个
  public final void testPut() {
    for (Table<Character, Integer, String> testInstance : getTestInstances()) {
      try {
        testInstance.put('a', 1, "blah");
        fail();
      } catch (UnsupportedOperationException e) {
        // success
      }
    }
  }

  //immutableTable不能添加全部
  public final void testPutAll() {
    for (Table<Character, Integer, String> testInstance : getTestInstances()) {
      try {
        testInstance.putAll(ImmutableTable.of('a', 1, "blah"));
        fail();
      } catch (UnsupportedOperationException e) {
        // success
      }
    }
  }

  //immutable也不能移除
  public final void testRemove() {
    for (Table<Character, Integer, String> testInstance : getTestInstances()) {
      try {
        testInstance.remove('a', 1);
        fail();
      } catch (UnsupportedOperationException e) {
        // success
      }
    }
  }

  //视图和其遍历内容一致
  public final void testConsistentToString() {
    for (ImmutableTable<Character, Integer, String> testInstance : getTestInstances()) {
      System.out.println("rowMap = " + testInstance.rowMap().toString());
      System.out.println("testInstance = " + testInstance.toString());
      assertEquals(testInstance.rowMap().toString(), testInstance.toString());
    }
  }

  //单元视图 和其遍历hashcode一致
  public final void testConsistentHashCode() {
    for (ImmutableTable<Character, Integer, String> testInstance : getTestInstances()) {
      assertEquals(testInstance.cellSet().hashCode(), testInstance.hashCode());
    }
  }
}
