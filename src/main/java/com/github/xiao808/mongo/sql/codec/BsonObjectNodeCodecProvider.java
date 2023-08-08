package com.github.xiao808.mongo.sql.codec;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.bson.codecs.Codec;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.json.BsonObjectNodeCodec;

/**
 * provider of codec for jackson
 * only ObjectNode is supported.
 *
 * @author zengxiao
 * @date 2023/3/30 18:01
 * @since 1.0
 **/
public class BsonObjectNodeCodecProvider implements CodecProvider {

    @Override
    public <T> Codec<T> get(Class<T> clazz, CodecRegistry registry) {
        if (clazz == ObjectNode.class) {
            return (Codec<T>) new BsonObjectNodeCodec();
        }
        return null;
    }
}
