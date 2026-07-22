package com.somagochi.pochakfarm.common.converter;

import java.util.Arrays;
import java.util.stream.Collectors;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;

@SuppressWarnings("rawtypes")
public class CaseInsensitiveEnumConverterFactory implements ConverterFactory<String, Enum> {

  @Override
  @SuppressWarnings("unchecked")
  public <T extends Enum> Converter<String, T> getConverter(Class<T> targetType) {
    return new StringToEnumConverter<>((Class<T>) enumTypeOf(targetType));
  }

  private static Class<?> enumTypeOf(Class<?> targetType) {
    Class<?> enumType = targetType;
    while (enumType != null && !enumType.isEnum()) {
      enumType = enumType.getSuperclass();
    }
    if (enumType == null) {
      throw new IllegalArgumentException(
          "The target type " + targetType.getName() + " does not refer to an enum");
    }
    return enumType;
  }

  private static final class StringToEnumConverter<T extends Enum> implements Converter<String, T> {

    private final Class<T> enumType;

    private StringToEnumConverter(Class<T> enumType) {
      this.enumType = enumType;
    }

    @Override
    public T convert(String source) {
      String value = source.trim();
      if (value.isEmpty()) {
        return null;
      }
      return Arrays.stream(enumType.getEnumConstants())
          .filter(constant -> constant.name().equalsIgnoreCase(value))
          .findFirst()
          .orElseThrow(() -> new IllegalArgumentException(message(value)));
    }

    private String message(String value) {
      String allowed =
          Arrays.stream(enumType.getEnumConstants())
              .map(Enum::name)
              .collect(Collectors.joining(", "));
      return "No enum constant "
          + enumType.getSimpleName()
          + "."
          + value
          + " (allowed: "
          + allowed
          + ")";
    }
  }
}
