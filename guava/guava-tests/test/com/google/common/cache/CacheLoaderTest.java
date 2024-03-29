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

package com.google.common.cache;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import junit.framework.TestCase;

/**
 * Unit tests for {@link CacheLoader}.
 *
 * @author Charles Fry
 */
public class CacheLoaderTest extends TestCase {

  private static class QueuingExecutor implements Executor {
    private LinkedList<Runnable> tasks = Lists.newLinkedList();

    @Override
    public void execute(Runnable task) {
      tasks.add(task);
    }

    private void runNext() {
      tasks.removeFirst().run();
    }
  }

  //异步加载
  public void testAsyncReload() throws Exception {
    final AtomicInteger loadCount = new AtomicInteger();
    final AtomicInteger reloadCount = new AtomicInteger();
    final AtomicInteger loadAllCount = new AtomicInteger();

    CacheLoader<Object, Object> baseLoader =
        new CacheLoader<Object, Object>() {
          @Override
          public Object load(Object key) {
            loadCount.incrementAndGet();
            return new Object();
          }

          @Override
          public ListenableFuture<Object> reload(Object key, Object oldValue) {
            reloadCount.incrementAndGet();
            return Futures.immediateFuture(new Object());
          }

          @Override
          public Map<Object, Object> loadAll(Iterable<?> keys) {
            loadAllCount.incrementAndGet();
            return ImmutableMap.of();
          }
        };

    //初始为0
    assertEquals(0, loadCount.get());
    assertEquals(0, reloadCount.get());
    assertEquals(0, loadAllCount.get());
    //load reload loadAll 分别调用一次
    baseLoader.load(new Object());
    @SuppressWarnings("unused") // go/futurereturn-lsc
    Future<?> possiblyIgnoredError = baseLoader.reload(new Object(), new Object());
    baseLoader.loadAll(ImmutableList.of(new Object()));
    assertEquals(1, loadCount.get());
    assertEquals(1, reloadCount.get());
    assertEquals(1, loadAllCount.get());

    //构造另外一个队列时 对原来的缓存有影响
    QueuingExecutor executor = new QueuingExecutor();
    CacheLoader<Object, Object> asyncReloader = CacheLoader.asyncReloading(baseLoader, executor);

    asyncReloader.load(new Object());
    @SuppressWarnings("unused") // go/futurereturn-lsc
    Future<?> possiblyIgnoredError1 = asyncReloader.reload(new Object(), new Object());
    asyncReloader.loadAll(ImmutableList.of(new Object()));
    assertEquals(2, loadCount.get());
    assertEquals(1, reloadCount.get());
    assertEquals(2, loadAllCount.get());

    executor.runNext();
    assertEquals(2, loadCount.get());
    assertEquals(2, reloadCount.get());
    assertEquals(2, loadAllCount.get());
  }
}
