/*
 * Copyright (C) 2011 The Guava Authors
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

import com.google.common.annotations.GwtCompatible;
import java.io.Serializable;
import java.util.Iterator;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * 判断容器是否相等
 * @param <T>
 */
@GwtCompatible(serializable = true)
final class PairwiseEquivalence<T> extends Equivalence<Iterable<T>> implements Serializable {

  final Equivalence<? super T> elementEquivalence;

  PairwiseEquivalence(Equivalence<? super T> elementEquivalence) {
    this.elementEquivalence = Preconditions.checkNotNull(elementEquivalence);
  }

  //比较两个可迭代类 是否相等
  @Override
  protected boolean doEquivalent(Iterable<T> iterableA, Iterable<T> iterableB) {
    Iterator<T> iteratorA = iterableA.iterator();
    Iterator<T> iteratorB = iterableB.iterator();

    while (iteratorA.hasNext() && iteratorB.hasNext()) {
      if (!elementEquivalence.equivalent(iteratorA.next(), iteratorB.next())) {
        return false;
      }
    }

    return !iteratorA.hasNext() && !iteratorB.hasNext();
  }

  //计算 容器hash
  @Override
  protected int doHash(Iterable<T> iterable) {
    int hash = 78721;
    for (T element : iterable) {
      hash = hash * 24943 + elementEquivalence.hash(element);
    }
    return hash;
  }

  @Override
  public boolean equals(@Nullable Object object) {
    if (object instanceof PairwiseEquivalence) {
      PairwiseEquivalence<?> that = (PairwiseEquivalence<?>) object;
      return this.elementEquivalence.equals(that.elementEquivalence);
    }

    return false;
  }

  @Override
  public int hashCode() {
    return elementEquivalence.hashCode() ^ 0x46a3eb07;
  }

  @Override
  public String toString() {
    return elementEquivalence + ".pairwise()";
  }

  private static final long serialVersionUID = 1;
}
