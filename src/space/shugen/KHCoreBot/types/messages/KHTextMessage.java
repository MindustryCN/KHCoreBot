package space.shugen.KHCoreBot.types.messages;

import space.shugen.KHCoreBot.types.Author;

public class KHTextMessage extends KHMessage {
    public Extra extra;

    public static class Extra {
        public Author author;
    }
}