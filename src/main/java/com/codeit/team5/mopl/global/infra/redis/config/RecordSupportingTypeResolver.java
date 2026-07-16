package com.codeit.team5.mopl.global.infra.redis.config;

import java.io.IOException;
import java.util.Collection;
import com.fasterxml.jackson.databind.DatabindContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.TypeIdResolver;
import com.fasterxml.jackson.databind.jsontype.impl.ClassNameIdResolver;
import com.fasterxml.jackson.databind.type.TypeFactory;

public class RecordSupportingTypeResolver extends ObjectMapper.DefaultTypeResolverBuilder {

    public RecordSupportingTypeResolver(ObjectMapper.DefaultTyping typing, PolymorphicTypeValidator validator) {
        super(typing, validator);
    }

    @Override
    public boolean useForType(JavaType type) {
        if (type.getRawClass().isRecord()) {
            return true;
        }
        return super.useForType(type);
    }

    @Override
    protected TypeIdResolver idResolver(MapperConfig<?> config, JavaType baseType,
                                        PolymorphicTypeValidator subtypeValidator,
                                        Collection<NamedType> subtypes, boolean forSer, boolean forDeser) {
        return new PackageStrippingTypeIdResolver(baseType, config.getTypeFactory(), subtypes, subtypeValidator);
    }

    public static class PackageStrippingTypeIdResolver extends ClassNameIdResolver {
        private static final String PREFIX = "com.codeit.team5.mopl.";

        public PackageStrippingTypeIdResolver(JavaType baseType, TypeFactory typeFactory,
                                              Collection<NamedType> subtypes, PolymorphicTypeValidator ptv) {
            super(baseType, typeFactory, subtypes, ptv);
        }

        @Override
        public String idFromValue(Object value) {
            String id = super.idFromValue(value);
            if (id != null && id.startsWith(PREFIX)) {
                return id.substring(PREFIX.length());
            }
            return id;
        }

        @Override
        public String idFromValueAndType(Object value, Class<?> type) {
            String id = super.idFromValueAndType(value, type);
            if (id != null && id.startsWith(PREFIX)) {
                return id.substring(PREFIX.length());
            }
            return id;
        }

        public JavaType typeFromId(DatabindContext context, String id) throws IOException {
            // 외부 라이브러리나 자바 기본 타입, 혹은 null인 경우 원본 그대로 반환 (Early Return)
            if (id == null || id.startsWith("java.") || id.startsWith("com.") || id.startsWith("org.") || id.startsWith("[L")) {
                return super.typeFromId(context, id);
            }

            // 그 외의 경우(우리가 패키지명을 잘라낸 도메인 객체) 원래의 prefix를 복구
            return super.typeFromId(context, PREFIX + id);
        }
    }
}
