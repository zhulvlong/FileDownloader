/*
 * Copyright (c) 2015 LingoChamp Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.liulishuo.filedownloader.demo;

import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;

import com.liulishuo.filedownloader.BaseDownloadTask;
import com.liulishuo.filedownloader.FileDownloadListener;
import com.liulishuo.filedownloader.FileDownloadQueueSet;
import com.liulishuo.filedownloader.FileDownloader;
import com.liulishuo.filedownloader.util.FileDownloadUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class DownloadGroupManager {


    public enum Mode {CONCURRENT, SERIAL}

    public static GroupDownload createGroup(@NonNull String groupTag,
                                            @NonNull Mode mode,
                                            @Nullable GroupListener listener) {
        return new GroupDownload(groupTag, mode, listener);
    }

    /* ===================== 以下为组对象 ===================== */
    public static class GroupDownload {

        private final String tag;
        private final Mode mode;
        private final GroupListener listener;
        private final List<BaseDownloadTask> taskList = new ArrayList<>();

        private int concurrent = 3;
        private int autoRetryTimes = 1;


        /* 进度汇总 */
        private final AtomicLong groupSoFar = new AtomicLong(0);
        private final AtomicLong groupTotal = new AtomicLong(0);
        private volatile boolean running = false;

        private GroupDownload(String tag, Mode mode, GroupListener listener) {
            this.tag = tag;
            this.mode = mode;
            this.listener = listener;
        }

        public GroupDownload setConcurrent(int concurrent) {
            this.concurrent = concurrent;
            return this;
        }

        public GroupDownload setAutoRetryTimes(int autoRetryTimes) {
            this.autoRetryTimes = autoRetryTimes;
            return this;
        }

        public GroupDownload addTask(String url, File targetFile, Object userTag) {
            TaskExtra extra = new TaskExtra(userTag);
            BaseDownloadTask task = FileDownloader.getImpl()
                    .create(url)
                    .setPath(targetFile.getAbsolutePath(), false)
                    .setAutoRetryTimes(autoRetryTimes)
                    .setTag(R.id.tag_extra, extra);
            taskList.add(task);
            return this;
        }

        /**
         * 启动当前组
         */
        @RequiresApi(api = Build.VERSION_CODES.N)
        public synchronized void start() {
            if (running) return;
            running = true;
            /* 1. 一次性拿到全部总大小 */
            List<String> urls = new ArrayList<>();
            for (BaseDownloadTask t : taskList) urls.add(t.getUrl());
            FileDownloadUtils.probeTotalSizeAsync(urls, urlSizeMap -> {
                /* 主线程回调 */
                for (BaseDownloadTask t : taskList) {
                    long size = urlSizeMap.getOrDefault(t.getUrl(), 0L);
                    if (size > 0) {
                        groupTotal.addAndGet(size);
                        TaskExtra extra = getExtra(t);
                        extra.totalBytes = size;
                    }
                }
                /* 继续启动下载 */
                /* 2. 统一监听器 */
                FileDownloadListener listenerWrapper = new FileDownloadListener() {
                    @Override
                    protected void pending(BaseDownloadTask task, int soFar, int total) {
                        TaskExtra extra = getExtra(task);
                        if (!extra.ended) {
                            ITaskSnapshot snapshot = new Snapshot(task, extra);
                            if (listener != null) {
                                listener.onTaskPending(tag, snapshot);
                            }
                        }
                    }

                    @Override
                    protected void started(BaseDownloadTask task) {
                        TaskExtra extra = getExtra(task);
                        if (!extra.ended) {
                            ITaskSnapshot snapshot = new Snapshot(task, extra);
                            if (listener != null) {
                                listener.onTaskStarted(tag, snapshot);
                            }
                        }
                    }

                    @Override
                    protected void connected(BaseDownloadTask task, String etag, boolean isContinue, int soFar, int total) {
                        /* 首次获得总大小 */
                        TaskExtra extra = getExtra(task);
                        if (!extra.ended) {
                            ITaskSnapshot snapshot = new Snapshot(task, extra);
                            if (listener != null) {
                                listener.onTaskConnected(tag, snapshot);
                            }
                        }
                    }

                    @Override
                    protected void progress(BaseDownloadTask task, int soFar, int total) {
                        TaskExtra extra = getExtra(task);
                        long delta = soFar - extra.soFarBytes;
                        if (delta > 0) {
                            extra.soFarBytes = soFar;
                            long newGroup = groupSoFar.addAndGet(delta);
                            if (listener != null) {
                                listener.onGroupProgress(tag, newGroup, groupTotal.get());
                            }
                        }
                    }

                    @Override
                    protected void completed(BaseDownloadTask task) {
                        end(task, EndCause.COMPLETED, null);
                    }

                    @Override
                    protected void error(BaseDownloadTask task, Throwable e) {
                        end(task, EndCause.ERROR, e);
                    }

                    @Override
                    protected void paused(BaseDownloadTask task, int soFar, int total) {
                        end(task, EndCause.CANCEL, null);
                    }

                    @Override
                    protected void warn(BaseDownloadTask task) {
                        end(task, EndCause.CANCEL, null);
                    }

                    private void end(BaseDownloadTask task, EndCause cause, Throwable e) {
                        TaskExtra extra = getExtra(task);
                        if (!extra.ended) {
                            extra.ended = true;
                            ITaskSnapshot snapshot = new Snapshot(task, extra);
                            if (listener != null)
                                listener.onTaskEnd(tag, snapshot, cause, e);

                            boolean allEnd = true;
                            for (BaseDownloadTask t : taskList) {
                                if (!((TaskExtra) t.getTag(R.id.tag_extra)).ended) {
                                    allEnd = false;
                                    break;
                                }
                            }
                            if (allEnd) {
                                running = false;
                                if (listener != null)
                                    listener.onGroupEnd(tag, groupSoFar.get(), groupTotal.get());
                            }
                        }
                    }
                };

                /* 3. 启动 */
                FileDownloadQueueSet queueSet = new FileDownloadQueueSet(listenerWrapper);
                queueSet.setCallbackProgressTimes(5000); // 进度回调频率
                queueSet.setAutoRetryTimes(autoRetryTimes);
                FileDownloader.getImpl().setMaxNetworkThreadCount(concurrent);
                if (mode == Mode.CONCURRENT) {
                    queueSet.downloadTogether(taskList);   // 并发
                    queueSet.start();
                } else {
                    queueSet.downloadSequentially(taskList); // 串行
                    queueSet.start();
                }
            });

        }

        public void cancelAll() {
            for (BaseDownloadTask t : taskList) t.pause();
        }

        private static TaskExtra getExtra(BaseDownloadTask task) {
            return (TaskExtra) task.getTag(R.id.tag_extra);
        }
    }

    /* ===================== 只读接口 ===================== */
    public interface ITaskSnapshot {
        String url();

        File targetFile();

        Object taskTag();

        long soFarBytes();

        long totalBytes();

        int speed();

        BaseDownloadTask task();
    }

    /* ===================== 回调接口 ===================== */
    public interface GroupListener {
        /**
         * 单任务准备
         */
        void onTaskPending(@NonNull String groupTag, @NonNull ITaskSnapshot snapshot);

        void onTaskStarted(@NonNull String groupTag, @NonNull ITaskSnapshot snapshot);

        void onTaskConnected(@NonNull String groupTag, @NonNull ITaskSnapshot snapshot);

        /**
         * 单任务结束
         */
        void onTaskEnd(@NonNull String groupTag,
                       @NonNull ITaskSnapshot snapshot,
                       @NonNull EndCause cause,
                       @Nullable Throwable error);

        /**
         * 组实时总进度
         */
        void onGroupProgress(@NonNull String groupTag,
                             long downloadedBytes,
                             long totalBytes);

        /**
         * 整组全部结束
         */
        void onGroupEnd(@NonNull String groupTag,
                        long downloadedBytes,
                        long totalBytes);
    }

    public enum EndCause {COMPLETED, ERROR, CANCEL}

    /* ===================== 内部扩展 ===================== */
    private static final class TaskExtra {
        final Object userTag;
        volatile long soFarBytes = 0;
        volatile long totalBytes = 0;
        volatile boolean ended = false;

        TaskExtra(Object userTag) {
            this.userTag = userTag;
        }
    }

    private static final class Snapshot implements ITaskSnapshot {
        private final BaseDownloadTask task;
        private final TaskExtra extra;

        Snapshot(BaseDownloadTask task, TaskExtra extra) {
            this.task = task;
            this.extra = extra;
        }

        @Override
        public String url() {
            return task.getUrl();
        }

        @Override
        public File targetFile() {
            return new File(task.getPath());
        }

        @Override
        public Object taskTag() {
            return extra.userTag;
        }

        @Override
        public long soFarBytes() {
            return extra.soFarBytes;
        }

        @Override
        public long totalBytes() {
            return extra.totalBytes;
        }

        @Override
        public int speed() {
            return (int) task.getSpeed();
        }

        @Override
        public BaseDownloadTask task() {
            return  task;
        }
    }

    /* ===================== 避免 id 冲突 ===================== */
    private static final class R {
        private static final class id {
            private static final int tag_extra = 0x7f0e0201;
        }
    }
}
