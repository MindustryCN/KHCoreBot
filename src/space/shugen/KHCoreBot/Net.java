package space.shugen.KHCoreBot;

import arc.struct.Seq;
import arc.util.Log;
import arc.util.Strings;
import arc.util.io.Streams;
import arc.util.serialization.Json;
import arc.util.serialization.JsonValue;
import mindustry.net.Host;
import mindustry.net.NetworkIO;
import org.apache.logging.log4j.Logger;

import java.io.InputStream;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.apache.logging.log4j.LogManager.getLogger;

public class Net {

    private static final Logger logger = getLogger("Net");

    public Net() {
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> getChangelog(list -> {
            try {
                VersionInfo latest = list.first();

                String lastVersion = getLastBuild();

                if (!latest.build.equals(lastVersion)) {
                    logger.info("Posting update!");

                    //don't post revisions
                    //sendUpdate
                    KHCoreBot.prefs.put("lastBuild", latest.build);
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }, e -> {
        }), 60, 240, TimeUnit.SECONDS);
    }

    public String getLastBuild() {
        return KHCoreBot.prefs.get("lastBuild", "101");
    }

    public InputStream download(String url) {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/56.0.2924.87 Safari/537.36");
            return connection.getInputStream();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void pingServer(String ip, Consumer<Host> listener) {
        run(0, () -> {
            try {
                String resultIP = ip;
                int port = 6567;
                if (ip.contains(":") && Strings.canParsePositiveInt(ip.split(":")[1])) {
                    resultIP = ip.split(":")[0];
                    port = Strings.parseInt(ip.split(":")[1]);
                }

                DatagramSocket socket = new DatagramSocket();
                socket.send(new DatagramPacket(new byte[]{-2, 1}, 2, InetAddress.getByName(resultIP), port));

                socket.setSoTimeout(2000);

                DatagramPacket packet = new DatagramPacket(new byte[256], 256);

                long start = System.currentTimeMillis();
                socket.receive(packet);

                ByteBuffer buffer = ByteBuffer.wrap(packet.getData());
                listener.accept(readServerData(buffer, ip, (int) (System.currentTimeMillis() - start)));
                socket.disconnect();
            } catch (Exception e) {
                listener.accept(new Host(0, null, ip, null, 0, 0, 0, null, null, 0, null, null));
            }
        });
    }

    public void getChangelog(Consumer<Seq<VersionInfo>> success, Consumer<Throwable> fail) {
        try {
            URL url = new URL(KHCoreBot.releasesURL);
            URLConnection con = url.openConnection();
            InputStream in = con.getInputStream();
            String encoding = con.getContentEncoding();
            encoding = encoding == null ? "UTF-8" : encoding;
            String body = Streams.copyString(in, 1000, encoding);

            Json j = new Json();
            Seq<JsonValue> list = j.fromJson(null, body);
            Seq<VersionInfo> out = new Seq<>();
            for (JsonValue value : list) {
                String name = value.getString("name");
                String description = value.getString("body").replace("\r", "");
                int id = value.getInt("id");
                String build = value.getString("tag_name").substring(1);
                out.add(new VersionInfo(name, description, id, build));
            }
            success.accept(out);
        } catch (Throwable e) {
            fail.accept(e);
        }
    }

    public static class VersionInfo {
        public final String name, description, build;
        public final int id;

        public VersionInfo(String name, String description, int id, String build) {
            this.name = name;
            this.description = description;
            this.id = id;
            this.build = build;
        }

        @Override
        public String toString() {
            return "VersionInfo{" +
                    "name='" + name + '\'' +
                    ", description='" + description + '\'' +
                    ", id=" + id +
                    ", build=" + build +
                    '}';
        }
    }

    public void run(long delay, Runnable r) {
        new Timer().schedule(
                new TimerTask() {
                    @Override
                    public void run() {
                        r.run();
                    }
                }, delay);
    }

    public Host readServerData(ByteBuffer buffer, String ip, int ping) {
        Host host = NetworkIO.readServerData((int) ping, ip, buffer);
        host.ping = (int) ping;
        return host;
        //return new PingResult(ip, ping, players + "", host, map, wave + "", version == -1 ? "Custom Build" : (""+version));
    }
}
