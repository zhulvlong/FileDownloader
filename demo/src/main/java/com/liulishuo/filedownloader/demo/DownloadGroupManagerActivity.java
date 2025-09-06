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

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.io.File;

public class DownloadGroupManagerActivity extends Activity {

    private TextView consoleA;
    private TextView consoleB;
    // 短视频 (下载速度快 2.77 MB)
    private String url = "https://cdnfile.sucai999.com/uploadfile/20211209/267440/132835061671596283.mp4?";
    // 国外视频 (下载速度慢 642MB)
    private String url2 = "https://mirror.clarkson.edu/blender/demo/movies/BBB/bbb_sunflower_2160p_60fps_normal.mp4";
    private String url3 = "https://jm-shitu.oss-cn-shenzhen.aliyuncs.com/video/4.mp4";
    private String url4 = "https://mirrors.cloud.tencent.com/gradle/gradle-5.4.1-all.zip";
    private String url5 = "http://cdn.llsapp.com/android/LLS-v4.0-595-20160908-143200.apk";
    private DownloadGroupManager.GroupDownload groupDownloadA;
    private DownloadGroupManager.GroupDownload groupDownloadB;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download_group_test);
        initView();
    }

    private void initView() {
        consoleA = findViewById(R.id.consoleA);
        consoleB = findViewById(R.id.consoleB);

        groupDownloadA = DownloadGroupManager
                .createGroup("A", DownloadGroupManager.Mode.CONCURRENT,
                        new DownloadGroupManager.GroupListener() {

                            @Override
                            public void onTaskPending(@NonNull String groupTag, @NonNull DownloadGroupManager.ITaskSnapshot snapshot) {
                                Log.d("A（DL_onTaskPending）", "任务准备:groupTag " + groupTag + ",taskTag=" + snapshot.taskTag() + ",下载文件=" + snapshot.task().getFilename());
                            }

                            @Override
                            public void onTaskStarted(@NonNull String groupTag, @NonNull DownloadGroupManager.ITaskSnapshot snapshot) {
                                Log.d("A（DL_onTaskStarted）", "任务开始:groupTag " + groupTag + ",taskTag=" + snapshot.taskTag() + ",下载文件=" + snapshot.task().getFilename());
                            }

                            @Override
                            public void onTaskConnected(@NonNull String groupTag, @NonNull DownloadGroupManager.ITaskSnapshot snapshot) {
                                Log.d("A（DL_onTaskConnected）", "任务连接:groupTag " + groupTag + ",taskTag=" + snapshot.taskTag() + ",下载文件=" + snapshot.task().getFilename());
                            }

                            @Override
                            public void onTaskEnd(@NonNull String groupTag, @NonNull DownloadGroupManager.ITaskSnapshot snapshot, @NonNull DownloadGroupManager.EndCause cause, @Nullable Throwable error) {
                                Log.d("A（DL_onTaskEnd）", "任务结束:groupTag " + groupTag + ",taskTag=" + snapshot.taskTag() + ",下载文件=" + snapshot.task().getFilename() + ",保存路径 = " + snapshot.targetFile().getAbsolutePath() + " 原因=" + cause + ",错误=" + error);
                            }

                            @Override
                            public void onGroupProgress(@NonNull String g,
                                                        long down, long total) {
                                float p = total == 0 ? 0 : (float) (down * 100 / total);
                                Log.d("A（DL_onGroupProgress）", "下载中:groupTag " + g + "，总进度 " + p + "%");
                            }

                            @Override
                            public void onGroupEnd(@NonNull String g,
                                                   long down, long total) {
                                Log.d("A（DL_onGroupEnd）", "任务结束:groupTag " + g + "，全部完成！");
                            }
                        })
                .setConcurrent(2)
                .addTask(url3, new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).getPath(), "A1.mp4"), "userTagA1")
                .addTask(url3, new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).getPath(), "A2.mp4"), "userTagA2")
                .addTask(url3, new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).getPath(), "A3.mp4"), "userTagA3")
                .addTask(url3, new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).getPath(), "A4.mp4"), "userTagA4")
                .addTask(url3, new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).getPath(), "A5.mp4"), "userTagA5")
                .addTask(url3, new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).getPath(), "A6.mp4"), "userTagA6");

        groupDownloadB = DownloadGroupManager
                .createGroup("B", DownloadGroupManager.Mode.CONCURRENT,
                        new DownloadGroupManager.GroupListener() {
                            @Override
                            public void onTaskPending(@NonNull String groupTag, @NonNull DownloadGroupManager.ITaskSnapshot snapshot) {
                                Log.d("B（DL_onTaskPending）", "任务准备:groupTag " + groupTag + ",taskTag=" + snapshot.taskTag() + ",下载文件=" + snapshot.task().getFilename());
                            }

                            @Override
                            public void onTaskStarted(@NonNull String groupTag, @NonNull DownloadGroupManager.ITaskSnapshot snapshot) {
                                Log.d("B（DL_onTaskStarted）", "任务开始:groupTag " + groupTag + ",taskTag=" + snapshot.taskTag() + ",下载文件=" + snapshot.task().getFilename());
                            }

                            @Override
                            public void onTaskConnected(@NonNull String groupTag, @NonNull DownloadGroupManager.ITaskSnapshot snapshot) {
                                Log.d("B（DL_onTaskConnected）", "任务连接:groupTag " + groupTag + ",taskTag=" + snapshot.taskTag() + ",下载文件=" + snapshot.task().getFilename());
                            }

                            @Override
                            public void onTaskEnd(@NonNull String groupTag, @NonNull DownloadGroupManager.ITaskSnapshot snapshot, @NonNull DownloadGroupManager.EndCause cause, @Nullable Throwable error) {
                                Log.d("B（DL_onTaskEnd）", "任务结束:groupTag=" + groupTag + ",taskTag=" + snapshot.taskTag() + ",下载文件=" + snapshot.task().getFilename() + ",保存路径 = " + snapshot.targetFile().getAbsolutePath() + " 原因=" + cause + ",错误=" + error);
                            }

                            @Override
                            public void onGroupProgress(@NonNull String groupTag,
                                                        long down, long total) {
                                float p = total == 0 ? 0 : (float) (down * 100 / total);
                                Log.d("B（DL_onGroupProgress）", "下载中:groupTag=" + groupTag + "，总进度 " + p + "%");
                            }

                            @Override
                            public void onGroupEnd(@NonNull String groupTag, long down, long total) {
                                Log.d("B（DL_onGroupEnd）", "任务结束:groupTag=" + groupTag + "，全部完成！");
                            }
                        })
                .setConcurrent(3)
                .addTask(url3, new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).getPath(), "B1.mp4"), "userTagB1")
                .addTask(url3, new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).getPath(), "B2.mp4"), "userTagB2")
                .addTask(url3, new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).getPath(), "B3.mp4"), "userTagB3")
                .addTask(url3, new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).getPath(), "B4.mp4"), "userTagB4")
                .addTask(url3, new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).getPath(), "B5.mp4"), "userTagB5")
                .addTask(url3, new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).getPath(), "B6.mp4"), "userTagB6");

    }

    public void startDownA(View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            groupDownloadA.start();
        }
    }

    public void pauseDownA(View view) {

    }

    public void resumeDownA(View view) {
    }

    public void canceDownA(View view) {
        groupDownloadA.cancelAll();
    }

    public void startDownB(View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            groupDownloadB.start();
        }
    }

    public void pauseDownB(View view) {
    }

    public void resumeDownB(View view) {
    }

    public void canceDownB(View view) {
        groupDownloadB.cancelAll();
    }
}
