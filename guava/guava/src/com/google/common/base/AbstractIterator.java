/*
 * Copyright (C) 2007 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.common.base;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.annotations.GwtCompatible;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.Iterator;
import java.util.NoSuchElementException;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Note this class is a copy of {@link com.google.common.collect.AbstractIterator} (for dependency
 * reasons).
 * 带状态的迭代器抽象类
 *
 */
@GwtCompatible
abstract class AbstractIterator<T> implements Iterator<T> {
  private State state = State.NOT_READY;

  protected AbstractIterator() {}

  //枚举类 所有的运行中状态
  private enum State {
    READY,
    NOT_READY,
    DONE,
    FAILED,
  }

//下一个节点
  private @Nullable T next;

  //计算下一个节点抽象接口
  protected abstract T computeNext();

  //遍历结束 返回null值
  @CanIgnoreReturnValue
  protected final @Nullable T endOfData() {
    state = State.DONE;
    return null;
  }

  //判断是否有下一个元素
  // 如果状态为 FAILED 直接抛异常  READY 为有下一个 DONE 为结束
  // NOT_READY 另外计算
  @Override
  public final boolean hasNext() {
    checkState(state != State.FAILED);
    switch (state) {
      case READY:
        return true;
      case DONE:
        return false;
      default:
    }
    return tryToComputeNext();
  }

  //查找下一个节点  state在查找中状态改变？？？
  private boolean tryToComputeNext() {
    state = State.FAILED; // temporary pessimism
    next = computeNext();
    if (state != State.DONE) {
      state = State.READY;
      return true;
    }
    return false;
  }

  //获取下一个元素  在hasNext()方法中就已经找到了下一个元素
  @Override
  public final T next() {
    if (!hasNext()) {
      throw new NoSuchElementException();
    }
    state = State.NOT_READY;
    T result = next;
    next = null;
    return result;
  }

  //未实现
  @Override
  public final void remove() {
    throw new UnsupportedOperationException();
  }
}
