/*
 * Copyright (C) 2011 The Guava Authors
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

import static com.google.common.testing.SerializableTester.reserializeAndAssert;

import com.google.common.annotations.GwtIncompatible;
import java.math.BigInteger;
import junit.framework.TestCase;

/**
 * Tests for {@link DiscreteDomain}.
 *
 * @author Chris Povirk
 */
@GwtIncompatible // SerializableTester
public class DiscreteDomainTest extends TestCase {
  //序列化测试
  public void testSerialization() {
    reserializeAndAssert(DiscreteDomain.integers());
    reserializeAndAssert(DiscreteDomain.longs());
    reserializeAndAssert(DiscreteDomain.bigIntegers());
  }

  //Integer计算离散数据域的偏移
  public void testIntegersOffset() {
    assertEquals(1, DiscreteDomain.integers().offset(0, 1).intValue());
    assertEquals(
        Integer.MAX_VALUE,
        DiscreteDomain.integers().offset(Integer.MIN_VALUE, (1L << 32) - 1).intValue());
  }

  //位移起始大于终点 报错
  public void testIntegersOffsetExceptions() {
    try {
      DiscreteDomain.integers().offset(0, -1);
      fail();
    } catch (IllegalArgumentException expected) {
    }
    try {
      DiscreteDomain.integers().offset(Integer.MAX_VALUE, 1);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  // Long型的偏移计算
  public void testLongsOffset() {
    assertEquals(1, DiscreteDomain.longs().offset(0L, 1).longValue());
    assertEquals(Long.MAX_VALUE, DiscreteDomain.longs().offset(0L, Long.MAX_VALUE).longValue());
  }

  // Long型偏移错误
  public void testLongsOffsetExceptions() {
    try {
      DiscreteDomain.longs().offset(0L, -1);
      fail();
    } catch (IllegalArgumentException expected) {
    }
    try {
      DiscreteDomain.longs().offset(Long.MAX_VALUE, 1);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  //大整数偏移计算
  public void testBigIntegersOffset() {
    assertEquals(BigInteger.ONE, DiscreteDomain.bigIntegers().offset(BigInteger.ZERO, 1));
    assertEquals(
        BigInteger.valueOf(Long.MAX_VALUE),
        DiscreteDomain.bigIntegers().offset(BigInteger.ZERO, Long.MAX_VALUE));
  }

  //偏移计算 起始大于终点错误
  public void testBigIntegersOffsetExceptions() {
    try {
      DiscreteDomain.bigIntegers().offset(BigInteger.ZERO, -1);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }
}
