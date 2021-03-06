/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.kafka.clients.admin.internals;

import org.apache.kafka.clients.MetadataUpdater;
import org.apache.kafka.common.Cluster;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.errors.AuthenticationException;
import org.apache.kafka.common.requests.MetadataResponse;
import org.apache.kafka.common.requests.RequestHeader;
import org.apache.kafka.common.utils.LogContext;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.List;

/**
 * Manages the metadata for KafkaAdminClient.
 *
 * This class is not thread-safe.  It is only accessed from the AdminClient
 * service thread (which also uses the NetworkClient).
 * Kafka 客户端 管理元数据 非线程安全
 */
public class AdminMetadataManager {
    private Logger log;

    /**
     * The minimum amount of time that we should wait between subsequent
     * retries, when fetching metadata.
     */
    //拉取更新 最小过期时间
    private final long refreshBackoffMs;

    /**
     * The minimum amount of time that we should wait before triggering an
     * automatic metadata refresh.
     */
    //元数据最大保留时间
    private final long metadataExpireMs;

    /**
     * Used to update the NetworkClient metadata.
     */
    private final AdminMetadataUpdater updater;

    /**
     * The current metadata state.
     */
    private State state = State.QUIESCENT;

    /**
     * The time in wall-clock milliseconds when we last updated the metadata.
     */
    //最新更新元数据时间
    private long lastMetadataUpdateMs = 0;

    /**
     * The time in wall-clock milliseconds when we last attempted to fetch new
     * metadata.
     */
    //最新获取元数据时间
    private long lastMetadataFetchAttemptMs = 0;

    /**
     * The current cluster information.
     */
    //集群信息
    private Cluster cluster = Cluster.empty();

    /**
     * If we got an authorization exception when we last attempted to fetch
     * metadata, this is it; null, otherwise.
     */
    //权限认证异常
    private AuthenticationException authException = null;

    //元数据更新内部类
    public class AdminMetadataUpdater implements MetadataUpdater {
        @Override
        public List<Node> fetchNodes() {
            return cluster.nodes();
        }

        @Override
        public boolean isUpdateDue(long now) {
            return false;
        }

        @Override
        public long maybeUpdate(long now) {
            return Long.MAX_VALUE;
        }

        @Override
        public void handleDisconnection(String destination) {
            // Do nothing
        }

        @Override
        public void handleFatalException(KafkaException e) {
            updateFailed(e);
        }

        @Override
        public void handleCompletedMetadataResponse(RequestHeader requestHeader, long now, MetadataResponse metadataResponse) {
            // Do nothing
        }

        @Override
        public void requestUpdate() {
            AdminMetadataManager.this.requestUpdate();
        }

        @Override
        public void close() {
        }
    }

    /**
     * The current AdminMetadataManager state.
     */
    enum State {
        QUIESCENT, //保持静态
        UPDATE_REQUESTED,  //请求
        UPDATE_PENDING   //获取
    }

    public AdminMetadataManager(LogContext logContext, long refreshBackoffMs, long metadataExpireMs) {
        this.log = logContext.logger(AdminMetadataManager.class);
        this.refreshBackoffMs = refreshBackoffMs;
        this.metadataExpireMs = metadataExpireMs;
        this.updater = new AdminMetadataUpdater();
    }

    public AdminMetadataUpdater updater() {
        return updater;
    }

    public boolean isReady() {
        if (authException != null) {
            log.debug("Metadata is not usable: failed to get metadata.", authException);
            throw authException;
        }
        if (cluster.nodes().isEmpty()) {
            log.trace("Metadata is not ready: bootstrap nodes have not been " +
                "initialized yet.");
            return false;
        }
        if (cluster.isBootstrapConfigured()) {
            log.trace("Metadata is not ready: we have not fetched metadata from " +
                "the bootstrap nodes yet.");
            return false;
        }
        log.trace("Metadata is ready to use.");
        return true;
    }
    //获取管理节点
    public Node controller() {
        return cluster.controller();
    }
    //根据nodeId获取节点
    public Node nodeById(int nodeId) {
        return cluster.nodeById(nodeId);
    }
    //请求更新 更改state状态
    public void requestUpdate() {
        if (state == State.QUIESCENT) {
            state = State.UPDATE_REQUESTED;
            log.debug("Requesting metadata update.");
        }
    }

    //生成新的controller类
    public void clearController() {
        if (cluster.controller() != null) {
            log.trace("Clearing cached controller node {}.", cluster.controller());
            this.cluster = new Cluster(cluster.clusterResource().clusterId(),
                cluster.nodes(),
                Collections.emptySet(),
                Collections.emptySet(),
                Collections.emptySet(),
                null);
        }
    }

    /**
     * Determine if the AdminClient should fetch new metadata.
     */
    //拉取元数据时间
    public long metadataFetchDelayMs(long now) {
        switch (state) {
            case QUIESCENT:
                // Calculate the time remaining until the next periodic update.
                // We want to avoid making many metadata requests in a short amount of time,
                // so there is a metadata refresh backoff period.
                return Math.max(delayBeforeNextAttemptMs(now), delayBeforeNextExpireMs(now));
            case UPDATE_REQUESTED:
                // Respect the backoff, even if an update has been requested
                return delayBeforeNextAttemptMs(now);
            default:
                // An update is already pending, so we don't need to initiate another one.
                return Long.MAX_VALUE;
        }
    }
    //下次更新时间时隔
    private long delayBeforeNextExpireMs(long now) {
        long timeSinceUpdate = now - lastMetadataUpdateMs;
        return Math.max(0, metadataExpireMs - timeSinceUpdate);
    }
    //下次拉取时间间隔
    private long delayBeforeNextAttemptMs(long now) {
        long timeSinceAttempt = now - lastMetadataFetchAttemptMs;
        return Math.max(0, refreshBackoffMs - timeSinceAttempt);
    }

    /**
     * Transition into the UPDATE_PENDING state.  Updates lastMetadataFetchAttemptMs.
     */
    //转换为拉取更新模式
    public void transitionToUpdatePending(long now) {
        this.state = State.UPDATE_PENDING;
        this.lastMetadataFetchAttemptMs = now;
    }

    //更新失败 设置为安静模式
    public void updateFailed(Throwable exception) {
        // We depend on pending calls to request another metadata update
        this.state = State.QUIESCENT;

        if (exception instanceof AuthenticationException) {
            log.warn("Metadata update failed due to authentication error", exception);
            this.authException = (AuthenticationException) exception;
        } else {
            log.info("Metadata update failed", exception);
        }
    }

    /**
     * Receive new metadata, and transition into the QUIESCENT state.
     * Updates lastMetadataUpdateMs, cluster, and authException.
     */
    //更新元数据
    public void update(Cluster cluster, long now) {
        if (cluster.isBootstrapConfigured()) {
            log.debug("Setting bootstrap cluster metadata {}.", cluster);
        } else {
            log.debug("Updating cluster metadata to {}", cluster);
            this.lastMetadataUpdateMs = now;
        }

        this.state = State.QUIESCENT;
        this.authException = null;

        if (!cluster.nodes().isEmpty()) {
            this.cluster = cluster;
        }
    }
}
