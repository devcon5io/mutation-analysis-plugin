package ch.devcon5.sonar.plugins.mutationanalysis.testharness;

import java.util.Locale;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class SystemLocaleExtension implements AfterEachCallback, BeforeEachCallback {

  private final Locale overrideLocale;
  private Locale defaultLocale = Locale.getDefault();

  public SystemLocaleExtension(Locale locale) {
    this.overrideLocale = locale;
  }

  public static SystemLocaleExtension overrideDefault(Locale locale) {
    return new SystemLocaleExtension(locale);
  }

  @Override
  public void beforeEach(ExtensionContext context) throws Exception {
    defaultLocale = Locale.getDefault();
    Locale.setDefault(overrideLocale);
  }

  @Override
  public void afterEach(ExtensionContext context) throws Exception {
    Locale.setDefault(defaultLocale);
  }

}
