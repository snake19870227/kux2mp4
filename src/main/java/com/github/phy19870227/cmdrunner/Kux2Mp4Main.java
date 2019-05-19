package com.github.phy19870227.cmdrunner;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author BuHuaYang
 * @date 2019/5/16
 */
public class Kux2Mp4Main {

    public static void main(String[] args) {
        Arrays.stream(args).forEach(s -> System.out.println("arg : " + s));
        String exeFile = args[0];
        try {
            ThreadFactory namedThreadFactory = new ThreadFactoryBuilder().setNameFormat("ffmpeg-pool-%d").build();
            ExecutorService threadPool =
                    new ThreadPoolExecutor(2, 4, 1L,
                            TimeUnit.SECONDS, new LinkedBlockingQueue<>(2),
                            namedThreadFactory, new ThreadPoolExecutor.AbortPolicy());
            File disFile = new File(args[1]);
            for (File file : Objects.requireNonNull(disFile.listFiles())) {
                String fileName = file.getName();
                if (!StringUtils.endsWith(fileName, ".kux")) {
                    continue;
                }
                String originalFile = file.getAbsolutePath();
                System.out.println("file : " + originalFile);

                String command = exeFile + " -y -i " + "\"" + originalFile + "\"" + " -c:v copy -c:a copy -threads 2 " + "\"" + originalFile + ".mp4\"";
                System.out.println("command : " + command);

                Process process = Runtime.getRuntime().exec(command);
                try {
                    final InputStream is1 = process.getInputStream();
                    final InputStream is2 = process.getErrorStream();
                    threadPool.execute(() -> printInputStream("normal", is1));
                    threadPool.execute(() -> printInputStream("error", is2));
                    process.waitFor();
                    process.destroy();
                } catch (Exception e) {
                    e.printStackTrace();
                    try {
                        process.getErrorStream().close();
                        process.getInputStream().close();
                        process.getOutputStream().close();
                    } catch (Exception ignored) {
                    }
                }
            }
            threadPool.shutdown();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void printInputStream(String streamType, InputStream is) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(streamType + " : " + line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
