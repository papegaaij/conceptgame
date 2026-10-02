package vanguard.content;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Optional;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.cfg.MapperConfig;
import tools.jackson.databind.introspect.Annotated;
import tools.jackson.databind.introspect.AnnotatedMember;
import tools.jackson.databind.introspect.JacksonAnnotationIntrospector;
import tools.jackson.dataformat.yaml.YAMLMapper;

/**
 * The YAML mapper for the data files: snake_case keys, enums in lower case, unknown keys rejected.
 * Every record component is required unless its type is {@link Optional}, and {@code notes} (the
 * README wording that tools/sync_tables.py renders) is skipped everywhere: the game never reads it.
 */
final class DataMapper {
    private DataMapper() {}

    static YAMLMapper create() {
        return YAMLMapper.builder()
                .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                .annotationIntrospector(new Introspector())
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
                .disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT)
                .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
                .build();
    }

    private static final class Introspector extends JacksonAnnotationIntrospector {
        private static final long serialVersionUID = 1L;

        @Override
        public Boolean hasRequiredMarker(MapperConfig<?> config, AnnotatedMember member) {
            return !Optional.class.equals(member.getRawType());
        }

        @Override
        public JsonIgnoreProperties.Value findPropertyIgnoralByName(MapperConfig<?> config, Annotated annotated) {
            return super.findPropertyIgnoralByName(config, annotated)
                    .withOverrides(JsonIgnoreProperties.Value.forIgnoredProperties("notes"));
        }
    }
}
