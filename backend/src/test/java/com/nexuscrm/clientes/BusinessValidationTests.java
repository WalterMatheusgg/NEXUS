package com.nexuscrm.clientes;

import static org.assertj.core.api.Assertions.*;

import com.nexuscrm.chamados.AttachmentService;
import com.nexuscrm.relatorios.ReportService;
import com.nexuscrm.shared.ApiException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class BusinessValidationTests {
  @Test
  void validatesCpfAndCnpjChecksumsAndRejectsRepeatedDigits() {
    assertThat(BrazilianDocument.normalize("PERSON", "529.982.247-25")).isEqualTo("52998224725");
    assertThat(BrazilianDocument.normalize("COMPANY", "11.222.333/0001-81"))
        .isEqualTo("11222333000181");
    assertThatThrownBy(() -> BrazilianDocument.normalize("PERSON", "11111111111"))
        .isInstanceOf(ApiException.class);
    assertThatThrownBy(() -> BrazilianDocument.normalize("PERSON", "52998224726"))
        .isInstanceOf(ApiException.class);
    assertThatThrownBy(() -> BrazilianDocument.normalize("COMPANY", "52998224725"))
        .isInstanceOf(ApiException.class);
  }

  @Test
  void filesMustMatchAnAllowedExtensionAndMagicBytes() {
    assertThat(
            AttachmentService.validate(
                "report.pdf", "%PDF-1.7\n".getBytes(StandardCharsets.US_ASCII)))
        .isEqualTo("application/pdf");
    assertThatThrownBy(
            () ->
                AttachmentService.validate("report.pdf", "<html>".getBytes(StandardCharsets.UTF_8)))
        .isInstanceOf(ApiException.class);
    assertThatThrownBy(
            () ->
                AttachmentService.validate(
                    "payload.txt", "<svg onload=alert(1)>".getBytes(StandardCharsets.UTF_8)))
        .isInstanceOf(ApiException.class);
    assertThatThrownBy(() -> AttachmentService.validate("file.exe", new byte[] {'M', 'Z'}))
        .isInstanceOf(ApiException.class);
    assertThat(AttachmentService.sanitize("../../private/document.txt")).isEqualTo("document.txt");
  }

  @Test
  void csvEscapesQuotesAndNeutralizesFormulaPrefixes() {
    assertThat(ReportService.csvCell("  =1+1")).isEqualTo("\"'  =1+1\"");
    assertThat(ReportService.csvCell("@SUM(A1:A2)")).startsWith("\"'");
    assertThat(ReportService.csvCell("Normal \"name\"")).isEqualTo("\"Normal \"\"name\"\"\"");
    assertThat(ReportService.csvCell("\t=1")).startsWith("\"'");
  }
}
