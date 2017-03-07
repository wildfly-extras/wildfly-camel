package org.wildfly.camel.test.common.rule;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.junit.Assume;
import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wildfly.camel.test.common.utils.EnvironmentUtils;

/**
 * Junit rule to download and execute external test resources
 */
public class ExecutableResource extends ExternalResource {

    private static final Path DOWNLOAD_HOME = Paths.get(System.getProperty("user.home"), ".arquillian", "wildfly-camel");
    private static final Logger LOG = LoggerFactory.getLogger(ExecutableResource.class);

    private String executable;
    private String downloadFrom;
    private String args;
    private int waitForPort;
    private boolean managed = true;
    private Process process;

    public ExecutableResource() {
    }

    @Override
    protected void before() throws Throwable {
        Assume.assumeFalse("Unknown environment: " + EnvironmentUtils.getOSName(), EnvironmentUtils.isUnknown());
        downloadExecutable();

        if (isManaged()) {
            startProcess();
        }
    }

    @Override
    protected void after() {
        if (isManaged()) {
            stopProcess();
        }
    }

    private void downloadExecutable() throws IOException {
        DOWNLOAD_HOME.toFile().mkdirs();
        File file = DOWNLOAD_HOME.resolve(this.executable).toFile();
        if (!file.exists()) {
            LOG.info("Downloading: {}", downloadFrom);
            URLConnection connection = new URL(downloadFrom).openConnection();
            InputStream in = new BufferedInputStream(connection.getInputStream(), 1024);
            if (isZippedResponse(connection.getContentType())) {
                file = DOWNLOAD_HOME.resolve(file.getName() + ".zip").toFile();
                LOG.debug("Downloading compressed archive to: {}", file.getAbsolutePath());
                copyStream(in, new FileOutputStream(file));
                unpackArchive(file);
                file.delete();
            } else {
                LOG.debug("Downloading executable to: {}", file.getAbsolutePath());
                copyStream(in, new FileOutputStream(file));
            }
        } else {
            LOG.info("Download skipped (already downloaded) for: {}", downloadFrom);
        }
    }

    public void startProcess() throws Exception {
        File file;
        if (this.executable == "java") {
            file = EnvironmentUtils.getJavaExecutablePath().toFile();
        } else {
            file = DOWNLOAD_HOME.resolve(this.executable).toFile();
            file.setExecutable(true);
        }

        List<String> command = new ArrayList<>();
        command.add(file.getAbsolutePath());
        command.addAll(Arrays.asList(args.split(" ")));

        LOG.info("Starting process: {}", command.stream().collect(Collectors.joining(" ")));
        ProcessBuilder builder = new ProcessBuilder();
        builder.command(command);
        process = builder.start();

        boolean socketAvailable = false;
        int count = 0;
        while (count < 10) {
            LOG.info("Waiting for process {} on localhost:{} ...", file.getName(), waitForPort);
            try (Socket socket = new Socket("localhost", waitForPort)) {
                if (socket.isConnected()) {
                    LOG.info("Successfully connected to localhost:{}", waitForPort);
                    socketAvailable = true;
                    break;
                }
            } catch (IOException e) {
            }
            count++;
            Thread.sleep(1000);
        }

        if (!socketAvailable) {
            stopProcess();
            throw new IllegalStateException("Failed to connect to external resource at localhost:" + waitForPort);
        }
    }

    public void stopProcess() {
        if (process != null) {
            LOG.info("Terminating process");
            process.destroy();

            int count = 0;
            while (process.isAlive()) {
                LOG.info("Process is still running. Waiting for exit...");
                if (count == 10) {
                    LOG.info("Gave up waiting for process to exit. Forcibly terminating!");
                    process.destroyForcibly();
                    return;
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                }
                count++;
            }
        }
    }

    private boolean isZippedResponse(String contentType) {
        return contentType.equals("application/zip") || contentType.equals("application/octet-stream");
    }

    private void unpackArchive(File file) throws IOException {
        ZipFile zipFile = new ZipFile(file);
        for (Enumeration entries = zipFile.entries(); entries.hasMoreElements();) {
            ZipEntry entry = (ZipEntry) entries.nextElement();
            LOG.debug("Extracting archive entry: {}", entry.getName());
            File zipFileEntry = DOWNLOAD_HOME.resolve(entry.getName()).toFile();
            if (!entry.isDirectory()) {
                copyStream(zipFile.getInputStream(entry), new FileOutputStream(zipFileEntry));
            } else {
                zipFileEntry.mkdirs();
            }
        }
        zipFile.close();
    }

    private void copyStream(InputStream inputStream, OutputStream outputStream) throws IOException {
        try {
            byte[] buffer = new byte[1024];
            int len = inputStream.read(buffer);
            while (len >= 0) {
                outputStream.write(buffer, 0, len);
                len = inputStream.read(buffer);
            }
        } finally {
            inputStream.close();
            outputStream.flush();
            outputStream.close();
        }
    }

    public ExecutableResourceBuilder builder() {
        return new ExecutableResourceBuilder();
    }

    public String getDownloadFrom() {
        return downloadFrom;
    }

    public void setDownloadFrom(String downloadFrom) {
        this.downloadFrom = downloadFrom;
    }

    public String getArgs() {
        return args;
    }

    public void setArgs(String args) {
        this.args = args;
    }

    public int getWaitForPort() {
        return waitForPort;
    }

    public void setWaitForPort(int waitForPort) {
        this.waitForPort = waitForPort;
    }

    public String getExecutable() {
        return executable;
    }

    public void setExecutable(String executable) {
        this.executable = executable;
    }

    public boolean isManaged() {
        return managed;
    }

    public void setManaged(boolean managed) {
        this.managed = managed;
    }

    public class ExecutableResourceBuilder {
        private String executable;
        private String downloadFrom;
        private String args;
        private int waitForPort;
        private boolean managed;

        public ExecutableResourceBuilder executable(String executable) {
            String fileSuffix = "";
            if (EnvironmentUtils.isWindows() && !executable.endsWith(".exe")) {
                fileSuffix = ".exe";
            }
            this.executable = executable + fileSuffix;
            return this;
        }

        public ExecutableResourceBuilder downloadFrom(String downloadFrom) {
            this.downloadFrom = downloadFrom;
            return this;
        }

        public ExecutableResourceBuilder args(String args) {
            this.args = args;
            return this;
        }

        public ExecutableResourceBuilder waitForPort(int waitForPort) {
            this.waitForPort = waitForPort;
            return this;
        }

        public ExecutableResourceBuilder managed(boolean managed) {
            this.managed = managed;
            return this;
        }

        public ExecutableResource build() {
            ExecutableResource resource = new ExecutableResource();
            resource.setExecutable(this.executable);
            resource.setDownloadFrom(this.downloadFrom);
            resource.setArgs(this.args);
            resource.setWaitForPort(this.waitForPort);
            resource.setManaged(this.managed);
            return resource;
        }
    }
}
