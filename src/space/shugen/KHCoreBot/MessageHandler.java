package space.shugen.KHCoreBot;

import arc.files.Fi;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import mindustry.Vars;
import mindustry.game.Schematic;
import mindustry.game.Schematics;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import space.shugen.KHCoreBot.types.Author;
import space.shugen.KHCoreBot.types.KHResponse;
import space.shugen.KHCoreBot.types.messages.KHFileMessage;
import space.shugen.KHCoreBot.types.messages.KHMessage;
import space.shugen.KHCoreBot.types.messages.KHTextMessage;
import space.shugen.KHCoreBot.types.responses.CreateAsset;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import static org.apache.logging.log4j.LogManager.getLogger;
import static space.shugen.KHCoreBot.CardGenerator.SchematicDisplay;
import static space.shugen.KHCoreBot.KHCoreBot.*;

public class MessageHandler extends Thread {

    private final ArrayBlockingQueue<KHTextMessage> workQueueA;
    private final ArrayBlockingQueue<KHFileMessage> workQueueB;
    private final Logger logger;
    private String receiveChannel;
    private String publishChannel;

    MessageHandler() {
        this.logger = getLogger(MessageHandler.class);
        KHCoreBot.khBot.onEvent(this::onEvent);
        this.workQueueA = new ArrayBlockingQueue<>(100);
        this.workQueueB = new ArrayBlockingQueue<>(100);
        this.receiveChannel = prefs.get("ReceiveFromChannel", "");
        this.publishChannel = prefs.get("PublishChannel", "");
        this.start();
    }

    public void onEvent(JSONObject event) {
        var type = event.getInteger("type");
        switch (type) {
            case 1 -> {
                this.workQueueA.add(event.toJavaObject(KHTextMessage.class));
            }
            case 4 -> {
                this.workQueueB.add(event.toJavaObject(KHFileMessage.class));
            }
        }
    }

    private void onText(KHTextMessage message) {
        if (!message.channel_type.equals("GROUP")) {
            return;
        }
        if (!message.target_id.equals(this.receiveChannel)) {
            return;
        }
        if (!message.content.startsWith(ContentHandler.schemHeader)) {
            return;
        }
        logger.info("开始处理文本消息" + message.msg_id);
        try {
            Schematic schematic = contentHandler.parseSchematic(message.content);
            generatorPreview(schematic, message.msg_id, message.target_id, message.extra.author, message);
        } catch (Throwable e) {
            ignoreErrorAndLog(khBot.sendChannelMessage(message.target_id, 1, "未知错误"));
            logger.error("Failed to parse schematic, skipping.");
            logger.error(e);
        }
        logger.info("文本消息处理完毕" + message.msg_id);
    }

    private void onFile(KHFileMessage message) {
        if (!message.channel_type.equals("GROUP")) {
            return;
        }
        if (!message.target_id.equals(this.receiveChannel)) {
            return;
        }
        if (!message.extra.attachments.name.endsWith(".msch")) {
            return;
        }
        logger.info("开始处理文件消息" + message.msg_id);
        try {
            Schematic schematic = contentHandler.parseSchematicURL(message.extra.attachments.url);
            generatorPreview(schematic, message.msg_id, message.target_id, message.extra.author, message);
        } catch (Exception e) {
            ignoreErrorAndLog(khBot.sendChannelMessage(message.target_id, 1, "未知错误"));
            logger.error(e);
        }
        logger.info("文件消息处理结束" + message.msg_id);
    }

    private void ignoreErrorAndLog(Call requestCall) {
        requestCall.enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                logger.error(e);
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                logger.info(response.body().string());
            }
        });
    }

    private void generatorPreview(Schematic schematic, String msg_id, String target_id, Author author, KHMessage message) throws Exception {
        BufferedImage preview = contentHandler.previewSchematic(schematic);
        var uuid = msg_id;
        String name = schematic.name();
        if (name.isEmpty()) name = "empty";

        new File("cache").mkdir();
        File previewFile = new File("cache/img_" + uuid + ".png");
        File schematicFile = new File("cache/" + uuid + "." + Vars.schematicExtension);
        Schematics.write(schematic, new Fi(schematicFile));
        ImageIO.write(preview, "png", previewFile);

        var uploadPreviewFile = new FutureTask<>(() -> {
            Response response = khBot.createAssets(previewFile, "image/png").execute();
            String data = response.body().string();
            KHResponse<CreateAsset> res = JSONObject.parseObject(data, new TypeReference<>() {
            });
            if (res.code != 0) {
                throw new Exception(data);
            }
            return res.data.url;
        });
        uploadPreviewFile.run();

        var uploadSchematicFile = new FutureTask<>(() -> {
            Response response = khBot.createAssets(schematicFile, "application/octet-stream").execute();
            String data = response.body().string();
            KHResponse<CreateAsset> res = JSONObject.parseObject(data, new TypeReference<>() {
            });
            if (res.code != 0) {
                throw new Exception(data);
            }
            return res.data.url;
        });
        uploadSchematicFile.run();
        try {
            String previewFileURL = uploadPreviewFile.get(10, TimeUnit.SECONDS);
            String schematicFileURL = uploadSchematicFile.get(10, TimeUnit.SECONDS);
            khBot.sendChannelMessage(publishChannel, 10, SchematicDisplay(schematic, previewFileURL, schematicFileURL, author)).enqueue(new Callback() {
                @Override
                public void onFailure(@NotNull Call call, @NotNull IOException e) {
                    logger.error(e);
                }

                @Override
                public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                    logger.info(response.body().string());
                }
            });
        } catch (Exception ex) {
            khBot.sendChannelMessage(target_id, 1, "未知错误").enqueue(new Callback() {
                @Override
                public void onFailure(@NotNull Call call, @NotNull IOException e) {
                    logger.error(e);
                }

                @Override
                public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                    logger.info(response.body().string());
                }
            });
            logger.error(ex);
        }
    }


    @Override
    public void run() {
        while (true) {
            try {
                var message = workQueueA.poll(1, TimeUnit.SECONDS);
                if (message != null) {
                    this.onText(message);
                }
                var messageB = workQueueB.poll(1, TimeUnit.SECONDS);
                if (messageB != null) {
                    this.onFile(messageB);
                }

            } catch (InterruptedException ex) {
                logger.error(ex);
            }
        }
    }
}
