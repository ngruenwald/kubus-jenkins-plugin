package com.ngruenwald.jenkins.plugin.kubus;

import java.io.File;
import java.io.IOException;

import org.jenkinsci.remoting.RoleChecker;
import org.kohsuke.stapler.DataBoundConstructor;

import hudson.FilePath;
import hudson.FilePath.FileCallable;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class KubusUploader implements FileCallable<Void> {

    String serverURL;
    int apiVersion;
    String apiKey;
    String metaData;
    FilePath filePath;

    TaskListener listener;

    @DataBoundConstructor
    public KubusUploader(
        String serverURL,
        int apiVersion,
        String apiKey,
        String metaData,
        FilePath filePath,
        TaskListener listener) {
        this.serverURL = serverURL;
        this.apiVersion = apiVersion;
        this.apiKey = apiKey;
        this.metaData = metaData;
        this.filePath = filePath;
        this.listener = listener;
    }

    @Override
    public void checkRoles(RoleChecker checker) throws SecurityException {
        // not implemented
    }

    @Override
    public Void invoke(File f, VirtualChannel channel) throws IOException, InterruptedException {
        try {
            String fileName = filePath.getName();
            MediaType mediaType = getMediaType(fileName);
            File file = new File(filePath.getRemote());

            HttpUrl requestURL = HttpUrl.parse(serverURL)
                .newBuilder()
                .addPathSegment("api")
                .addPathSegment(String.format("v%d", apiVersion))
                .addPathSegment("upload")
                .build();

            RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("meta", metaData)
                .addFormDataPart("file", fileName, RequestBody.create(mediaType, file))
                .build();

            Request request = new Request.Builder()
                .header("X-API-Key", apiKey)
                .url(requestURL)
                .post(requestBody)
                .build();

            OkHttpClient client = new OkHttpClient();
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new IOException("Unexpected code " + response);
                }
            }
        }
        catch (IOException e) {
            e.printStackTrace(listener.error("Communication error, failed to upload file"));
        }

        return null;
    }

    private MediaType getMediaType(String fileName) {
        String mediaType = "application/octet-stream";

        if (fileName.endsWith(".zip")) {
            mediaType = "application/zip";
        } else if (fileName.endsWith(".rpm")) {
            mediaType = "application/x-rpm";
        }

        return MediaType.parse(mediaType);
    }
}