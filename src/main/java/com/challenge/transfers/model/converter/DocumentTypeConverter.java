package com.challenge.transfers.model.converter;

import com.challenge.transfers.model.api.DocumentType;
import java.util.Arrays;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
public class DocumentTypeConverter implements Converter<String, DocumentType> {

  @Override
  public DocumentType convert(@NonNull String code) {
    return Arrays.stream(DocumentType.values())
        .filter(dt -> dt.getCode().equals(code))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Invalid document type: " + code));
  }
}
