package com.github.xiao808.mongo.sql.codec;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.json.JsonReader;
import org.bson.json.JsonWriter;
import org.bson.json.JsonWriterSettings;

import java.io.StringWriter;

/**
 * @author zengxiao
 * @description
 * @date 2023/3/30 17:47
 * @since 1.0
 **/
@Slf4j
public class BsonObjectNodeCodec implements Codec<ObjectNode> {

    private final JsonWriterSettings writerSettings = JsonWriterSettings.builder().build();

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public ObjectNode decode(BsonReader reader, DecoderContext decoderContext) {
        StringWriter stringWriter = new StringWriter();
        new JsonWriter(stringWriter, writerSettings).pipe(reader);
        try {
            return mapper.readValue(stringWriter.toString(), ObjectNode.class);
        } catch (JsonProcessingException e) {
            log.error("decode bson document failed.", e);
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void encode(BsonWriter writer, ObjectNode value, EncoderContext encoderContext) {
        writer.pipe(new JsonReader(value.textValue()));
    }

    @Override
    public Class<ObjectNode> getEncoderClass() {
        return ObjectNode.class;
    }
}
