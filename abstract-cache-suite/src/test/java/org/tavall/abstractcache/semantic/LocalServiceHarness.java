package org.tavall.abstractcache.semantic;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * Starts isolated local service processes for integration tests.
 */
final class LocalServiceHarness {

    private LocalServiceHarness() {
    }

    static RedisService startRedis(Path rootDirectory) throws IOException, InterruptedException {
        Path redisServer = resolveCommand("redis-server");
        Path redisCli = resolveCommand("redis-cli");
        int port = reservePort();
        Path workingDirectory = Files.createDirectories(rootDirectory.resolve("redis"));
        Path pidFile = workingDirectory.resolve("redis.pid");
        Path logFile = workingDirectory.resolve("redis.log");

        runCommand(
                workingDirectory,
                Duration.ofSeconds(15),
                Map.of(),
                redisServer.toString(),
                "--save",
                "",
                "--appendonly",
                "no",
                "--bind",
                "127.0.0.1",
                "--port",
                Integer.toString(port),
                "--dir",
                workingDirectory.toString(),
                "--dbfilename",
                "dump.rdb",
                "--daemonize",
                "yes",
                "--pidfile",
                pidFile.toString(),
                "--logfile",
                logFile.toString()
        );
        waitForPort("127.0.0.1", port, Duration.ofSeconds(15));
        return new RedisService(port, pidFile, redisCli);
    }

    static PostgresService startPostgres(Path rootDirectory) throws IOException, InterruptedException {
        Path initdb = resolveCommand("initdb");
        Path pgCtl = resolveCommand("pg_ctl");
        int port = reservePort();
        Path workingDirectory = Files.createDirectories(rootDirectory.resolve("postgres"));
        Path dataDirectory = workingDirectory.resolve("data");
        Path logFile = workingDirectory.resolve("postgres.log");
        Map<String, String> env = Map.of(
                "LC_ALL", "C",
                "LANG", "C"
        );

        runCommand(
                workingDirectory,
                Duration.ofSeconds(30),
                env,
                initdb.toString(),
                "-A",
                "trust",
                "-U",
                "postgres",
                "-D",
                dataDirectory.toString()
        );
        runCommand(
                workingDirectory,
                Duration.ofSeconds(30),
                env,
                pgCtl.toString(),
                "-D",
                dataDirectory.toString(),
                "-l",
                logFile.toString(),
                "-o",
                "-p " + port + " -h 127.0.0.1 -k " + workingDirectory,
                "-w",
                "start"
        );
        waitForPort("127.0.0.1", port, Duration.ofSeconds(15));
        return new PostgresService(port, dataDirectory, pgCtl);
    }

    static MongoService startMongo(Path rootDirectory) throws IOException, InterruptedException {
        Path mongod = resolveCommand("mongod");
        int port = reservePort();
        Path workingDirectory = Files.createDirectories(rootDirectory.resolve("mongo"));
        Path dataDirectory = Files.createDirectories(workingDirectory.resolve("data"));
        Path pidFile = workingDirectory.resolve("mongod.pid");
        Path logFile = workingDirectory.resolve("mongod.log");

        runCommand(
                workingDirectory,
                Duration.ofSeconds(30),
                Map.of(),
                mongod.toString(),
                "--dbpath",
                dataDirectory.toString(),
                "--bind_ip",
                "127.0.0.1",
                "--port",
                Integer.toString(port),
                "--logpath",
                logFile.toString(),
                "--pidfilepath",
                pidFile.toString(),
                "--fork",
                "--quiet"
        );
        waitForPort("127.0.0.1", port, Duration.ofSeconds(15));
        return new MongoService(port, pidFile);
    }

    private static Path resolveCommand(String command) {
        if (command.contains("/")) {
            Path directPath = Path.of(command);
            if (Files.isExecutable(directPath)) {
                return directPath;
            }
        }

        String path = System.getenv("PATH");
        if (path != null && !path.isBlank()) {
            for (String segment : path.split(":")) {
                Path candidate = Path.of(segment, command);
                if (Files.isExecutable(candidate)) {
                    return candidate;
                }
            }
        }

        Path postgresRoot = Path.of("/usr/lib/postgresql");
        if (Files.isDirectory(postgresRoot)) {
            try (Stream<Path> stream = Files.walk(postgresRoot, 4)) {
                Optional<Path> candidate = stream
                        .filter(pathEntry -> pathEntry.getFileName().toString().equals(command))
                        .filter(Files::isExecutable)
                        .sorted(Comparator.reverseOrder())
                        .findFirst();
                if (candidate.isPresent()) {
                    return candidate.get();
                }
            } catch (IOException exception) {
                throw new IllegalStateException("failed to scan postgres binaries", exception);
            }
        }

        throw new IllegalStateException("required test command not found: " + command);
    }

    private static int reservePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            socket.setReuseAddress(true);
            return socket.getLocalPort();
        }
    }

    private static void waitForPort(String host, int port, Duration timeout) throws InterruptedException {
        Instant deadline = Instant.now().plus(timeout);
        while (Instant.now().isBefore(deadline)) {
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(host, port), 500);
                return;
            } catch (IOException exception) {
                Thread.sleep(100);
            }
        }
        throw new IllegalStateException("service did not start on " + host + ":" + port);
    }

    private static void runCommand(
            Path workingDirectory,
            Duration timeout,
            Map<String, String> environment,
            String... command
    ) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(workingDirectory.toFile());
        processBuilder.redirectErrorStream(true);
        processBuilder.environment().putAll(environment);

        Process process = processBuilder.start();
        boolean finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        if (!finished) {
            process.destroyForcibly();
            throw new IllegalStateException("command timed out: " + String.join(" ", command));
        }
        if (process.exitValue() != 0) {
            throw new IllegalStateException(
                    "command failed (" + process.exitValue() + "): " + String.join(" ", command) + System.lineSeparator() + output
            );
        }
    }

    static final class RedisService implements AutoCloseable {

        private final int port;
        private final Path pidFile;
        private final Path redisCli;

        private RedisService(int port, Path pidFile, Path redisCli) {
            this.port = port;
            this.pidFile = pidFile;
            this.redisCli = redisCli;
        }

        String url() {
            return "redis://127.0.0.1:" + port;
        }

        @Override
        public void close() {
            try {
                runCommand(
                        pidFile.getParent(),
                        Duration.ofSeconds(15),
                        Map.of(),
                        redisCli.toString(),
                        "-h",
                        "127.0.0.1",
                        "-p",
                        Integer.toString(port),
                        "shutdown",
                        "nosave"
                );
            } catch (Exception exception) {
                destroyPid(pidFile);
            }
        }
    }

    static final class PostgresService implements AutoCloseable {

        private final int port;
        private final Path dataDirectory;
        private final Path pgCtl;

        private PostgresService(int port, Path dataDirectory, Path pgCtl) {
            this.port = port;
            this.dataDirectory = dataDirectory;
            this.pgCtl = pgCtl;
        }

        String jdbcUrl() {
            return "jdbc:postgresql://127.0.0.1:" + port + "/postgres";
        }

        String username() {
            return "postgres";
        }

        String password() {
            return "";
        }

        @Override
        public void close() {
            try {
                runCommand(
                        dataDirectory.getParent(),
                        Duration.ofSeconds(30),
                        Map.of(
                                "LC_ALL", "C",
                                "LANG", "C"
                        ),
                        pgCtl.toString(),
                        "-D",
                        dataDirectory.toString(),
                        "-w",
                        "stop",
                        "-m",
                        "fast"
                );
            } catch (Exception exception) {
                throw new IllegalStateException("failed to stop postgres test service", exception);
            }
        }
    }

    static final class MongoService implements AutoCloseable {

        private final int port;
        private final Path pidFile;

        private MongoService(int port, Path pidFile) {
            this.port = port;
            this.pidFile = pidFile;
        }

        String connectionString() {
            return "mongodb://127.0.0.1:" + port;
        }

        @Override
        public void close() {
            destroyPid(pidFile);
        }
    }

    private static void destroyPid(Path pidFile) {
        try {
            if (!Files.exists(pidFile)) {
                return;
            }
            long pid = Long.parseLong(Files.readString(pidFile).trim());
            ProcessHandle.of(pid).ifPresent(process -> {
                process.destroy();
                try {
                    process.onExit().get(15, TimeUnit.SECONDS);
                } catch (Exception exception) {
                    process.destroyForcibly();
                }
            });
        } catch (IOException exception) {
            throw new IllegalStateException("failed to read service pid file", exception);
        }
    }
}
