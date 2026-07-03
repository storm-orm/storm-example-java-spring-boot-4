package st.orm.demo.imdb.serialization;

import java.math.BigDecimal;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.deser.std.StdDeserializer;
import tools.jackson.databind.ser.std.StdSerializer;

/**
 * Serializes BigDecimal as its plain string representation, preserving
 * precision and scale exactly — a JSON number would lose the trailing scale
 * that the database returns.
 */
public class BigDecimalAsStringSerializer extends StdSerializer<BigDecimal> {

    public BigDecimalAsStringSerializer() {
        super(BigDecimal.class);
    }

    @Override
    public void serialize(BigDecimal value, JsonGenerator gen, SerializationContext ctxt) throws JacksonException {
        gen.writeString(value.toPlainString());
    }

    /** Reads the plain string representation back into a BigDecimal. */
    public static class Deserializer extends StdDeserializer<BigDecimal> {

        public Deserializer() {
            super(BigDecimal.class);
        }

        @Override
        public BigDecimal deserialize(JsonParser parser, DeserializationContext ctxt) throws JacksonException {
            return new BigDecimal(parser.getString());
        }
    }
}
