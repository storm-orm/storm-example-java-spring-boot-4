package st.orm.demo.imdb.serialization;

import java.time.Instant;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.deser.std.StdDeserializer;
import tools.jackson.databind.ser.std.StdSerializer;

/**
 * Serializes Instant as its ISO-8601 string representation — Jackson has no
 * built-in java.time support without the JavaTime module, so a field-level
 * serializer keeps the value stable and human-readable.
 */
public class InstantAsStringSerializer extends StdSerializer<Instant> {

    public InstantAsStringSerializer() {
        super(Instant.class);
    }

    @Override
    public void serialize(Instant value, JsonGenerator gen, SerializationContext ctxt) throws JacksonException {
        gen.writeString(value.toString());
    }

    /** Reads the ISO-8601 string representation back into an Instant. */
    public static class Deserializer extends StdDeserializer<Instant> {

        public Deserializer() {
            super(Instant.class);
        }

        @Override
        public Instant deserialize(JsonParser parser, DeserializationContext ctxt) throws JacksonException {
            return Instant.parse(parser.getText());
        }
    }
}
