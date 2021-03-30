package space.shugen.KHCoreBot;

import arc.struct.Seq;
import com.alibaba.fastjson.JSONArray;
import mindustry.game.Schematic;
import mindustry.type.ItemSeq;
import space.shugen.KHCoreBot.types.Author;

import java.util.HashMap;

public class CardGenerator {
    private static HashMap<String, String> resourceMap;

    static {
        resourceMap = new HashMap<>();
        resourceMap.put("copper", "3331796095471506/98e7f903f7a93600w00w");
        resourceMap.put("lead", "3331796095471506/8c2c778a26146b00w00w");
        resourceMap.put("graphite", "3331796095471506/63f4470f3fe65600w00w");
        resourceMap.put("metaglass", "3331796095471506/bc5609ffc3382300w00w");
        resourceMap.put("titanium", "3331796095471506/53b25954dd703d00w00w");
        resourceMap.put("thorium", "3331796095471506/c6dd8ffc8ed59c00w00w");
        resourceMap.put("silicon", "3331796095471506/30a86ca778370f00w00w");
        resourceMap.put("plastanium", "3331796095471506/afdf4fd7d1c0d500w00w");
        resourceMap.put("surge-alloy", "3331796095471506/f846a82e2c99ed00w00w");
        resourceMap.put("phase-fabric", "3331796095471506/0509eb885ccebc00w00w");
    }

    public static String SchematicDisplay(Schematic schem, String previewFileURL, String schemFileURL, Author author) {
        var authorCard = new Card();
        var title = new Section("(met)" + author.id + "(met)发布了一张蓝图", "kmarkdown");
        title.accessory = new Image(author.avatar, "sm");
        authorCard.modules.add(title);
        var modules = new JSONArray();
        var name = schem.name();
        if (name.isEmpty()) name = "empty";
        modules.add(new Header(schem.name()));
        modules.add(new Section(schem.description()));
        modules.add(new Section(stringifyResourceRequirement(schem.requirements()), "kmarkdown"));
        //noinspection SpellCheckingInspection
        modules.add(new Section(String.format(
                "(emj)powerin(emj)[3331796095471506/457dfff0e1fd4c03104c] %s (emj)powerout(emj)[3331796095471506/750e89bc1a684c03104c] %s",
                schem.powerProduction(), schem.powerConsumption()),
                "kmarkdown"));
        modules.add(new ImageGroup(previewFileURL));
        modules.add(new File("unknown.msch", schemFileURL));
        var schematicCard = new Card();
        schematicCard.modules = modules;
        var cards = new JSONArray();
        cards.add(authorCard);
        cards.add(schematicCard);
        return cards.toString();
    }

    private static class Text {
        public String content;
        public String type = "plain-text";

        Text(String content) {
            this.content = content;
        }

        Text(String content, String type) {
            this.content = content;
            this.type = type;
        }
    }

    private static class Header {
        public String type = "header";
        public Text text;

        Header(String content) {
            this.text = new Text(content);
        }
    }

    private static class Image {
        public String type = "image";
        public String src;
        public String size = "lg";
        public Boolean circle = false;

        Image(String src) {
            this.src = src;
        }

        Image(String src, String size) {
            this.src = src;
            this.size = size;
        }
    }

    private static class ResourceImage {
        public String type = "image";
        public String src;
        public String content;

        ResourceImage(String itemType) {
            if (resourceMap.containsKey(itemType)) {
                this.type = "image";
                this.src = resourceMap.get(itemType);
            } else {
                this.type = "plain-text";
                this.content = itemType;
            }
        }
    }

    private static class Section {
        public String type = "section";
        public Text text;
        public Object accessory = null;

        Section(String content) {
            this.text = new Text(content);
        }

        Section(String content, String type) {
            this.text = new Text(content, type);
        }
    }

    private static class ImageGroup {
        public String type = "image-group";
        public JSONArray elements = new JSONArray();

        ImageGroup() {}

        ImageGroup(String url) {
            this.elements.add(new Image(url));
        }

    }

    private static class File {
        public String type = "file";
        public String title = "";
        public String src;

        File(String src) {
            this.src = src;
        }

        File(String name, String src) {
            this.src = src;
            this.title = name;
        }
    }

    private static class Context {
        public String type = "context";
        public JSONArray elements = new JSONArray();
    }

    private static class Card {
        public String type = "card";
        public String theme = "secondary";
        public String size = "lg";
        public JSONArray modules = new JSONArray();
    }

    private static String stringifyResourceRequirement(ItemSeq requirements) {
        var req = requirements.toSeq();
        var result = new Seq<String>();
        for (int i = 0; i < req.size; i++) {
            if (resourceMap.containsKey(req.get(i).item.name)) {
                result.add(String.format("(emj)%s(emj)[%s] %d", req.get(i).item.name, resourceMap.get(req.get(i).item.name), req.get(i).amount));
            } else {
                result.add(String.format("非标准矿物%s %d", req.get(i).item.name, req.get(i).amount));
            }
        }
        return String.join(" ", result);
    }
}
