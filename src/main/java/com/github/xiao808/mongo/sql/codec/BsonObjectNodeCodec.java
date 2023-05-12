package com.github.xiao808.mongo.sql.codec;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
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
import java.util.Iterator;

import static com.github.xiao808.mongo.sql.MongoIdConstants.MONGO_ID;
import static com.github.xiao808.mongo.sql.MongoIdConstants.MONGO_OBJECT_ID;
import static com.github.xiao808.mongo.sql.MongoIdConstants.REPRESENT_MONGO_ID;
import static com.github.xiao808.mongo.sql.MongoIdConstants.REPRESENT_PAGE_DATA;
import static com.github.xiao808.mongo.sql.MongoIdConstants.REPRESENT_PAGE_TOTAL;

/**
 * codec for jackson
 * only ObjectNode is supported.
 *
 * @author zengxiao
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
            ObjectNode objectNode = mapper.readValue(stringWriter.toString(), ObjectNode.class);
            if (isPageDocument(objectNode)) {
                handlePageDocument(objectNode);
            } else {
                handleNormalDocument(objectNode);
            }
            return objectNode;
        } catch (JsonProcessingException e) {
            log.error("decode bson document failed.", e);
            e.printStackTrace();
        }
        return null;
    }

    private boolean isPageDocument(ObjectNode objectNode) {
        return objectNode.has(REPRESENT_PAGE_DATA) && objectNode.has(REPRESENT_PAGE_TOTAL);
    }

    private void handleNormalDocument(ObjectNode objectNode) {
        if (objectNode.has(MONGO_ID)) {
            // for group by clause
            JsonNode groupNode = objectNode.remove(MONGO_ID);
            if (groupNode.isObject()) {
                Iterator<String> stringIterator = groupNode.fieldNames();
                while (stringIterator.hasNext()) {
                    String key = stringIterator.next();
                    String[] keyParts = key.split("_");
                    objectNode.set(keyParts[keyParts.length - 1], groupNode.get(key));
                }
            } else if (groupNode.isValueNode()) {
                // for _id which is not ObjectId
                objectNode.set(REPRESENT_MONGO_ID, groupNode);
            }
            if (objectNode.has(MONGO_OBJECT_ID)) {
                // for ObjectId
                JsonNode objectIdNode = objectNode.remove(MONGO_OBJECT_ID);
                if (objectIdNode.isValueNode()) {
                    objectNode.set(REPRESENT_MONGO_ID, objectIdNode);
                }
            }
        }
    }

    private void handlePageDocument(ObjectNode objectNode) {
        if (objectNode.has(REPRESENT_PAGE_DATA)) {
            JsonNode data = objectNode.remove(REPRESENT_PAGE_DATA);
            if (data.isArray()) {
                for (JsonNode datum : data) {
                    if (datum.isObject()) {
                        handleNormalDocument((ObjectNode) datum);
                    }
                }
                objectNode.set(REPRESENT_PAGE_DATA, data);
            }
        }
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
