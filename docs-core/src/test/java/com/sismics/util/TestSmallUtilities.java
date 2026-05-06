package com.sismics.util;

import com.sismics.docs.core.util.jpa.PaginatedList;
import com.sismics.util.mime.MimeType;
import com.sismics.util.mime.MimeTypeUtil;
import com.sismics.util.log4j.LogCriteria;
import com.sismics.util.log4j.LogEntry;
import com.sismics.util.log4j.MemoryAppender;
import com.sismics.util.totp.GoogleAuthenticatorConfig;
import com.sismics.util.totp.KeyRepresentation;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.Assert;
import org.junit.Test;

import java.util.Locale;

/**
 * Tests for small utility classes with branch-heavy logic.
 */
public class TestSmallUtilities {
    @Test
    public void localeUtilParsesDefaultLanguageCountryAndVariant() {
        Assert.assertEquals(Locale.ENGLISH, LocaleUtil.getLocale(null));
        Assert.assertEquals(Locale.ENGLISH, LocaleUtil.getLocale(""));

        Locale french = LocaleUtil.getLocale("fr");
        Assert.assertEquals("fr", french.getLanguage());
        Assert.assertEquals("", french.getCountry());
        Assert.assertEquals("", french.getVariant());

        Locale frenchCanada = LocaleUtil.getLocale("fr_CA");
        Assert.assertEquals("fr", frenchCanada.getLanguage());
        Assert.assertEquals("CA", frenchCanada.getCountry());
        Assert.assertEquals("", frenchCanada.getVariant());

        Locale posixEnglish = LocaleUtil.getLocale("en_US_POSIX");
        Assert.assertEquals("en", posixEnglish.getLanguage());
        Assert.assertEquals("US", posixEnglish.getCountry());
        Assert.assertEquals("POSIX", posixEnglish.getVariant());
    }

    @Test
    public void memoryAppenderFiltersAndPaginatesLogs() {
        MemoryAppender appender = new MemoryAppender();
        appender.setSize(10);

        appender.append(event("RootLogger", Level.DEBUG, "debug ignored"));
        appender.append(event("com.example.Service", Level.INFO, "User created"));
        appender.append(event("com.example.Other", Level.ERROR, "Fatal user deleted"));

        PaginatedList<LogEntry> page = new PaginatedList<>(10, 0);
        appender.find(new LogCriteria()
                .setMinLevel(Level.INFO)
                .setTag("SERVICE")
                .setMessage("USER"), page);

        Assert.assertEquals(1, page.getResultCount());
        Assert.assertEquals(1, page.getResultList().size());
        Assert.assertEquals("Service", page.getResultList().get(0).getTag());
        Assert.assertEquals(Level.INFO, page.getResultList().get(0).getLevel());
        Assert.assertEquals("User created", page.getResultList().get(0).getMessage());

        PaginatedList<LogEntry> allLogs = new PaginatedList<>(10, -1);
        appender.find(new LogCriteria(), allLogs);

        Assert.assertEquals(3, allLogs.getResultCount());
        Assert.assertEquals(3, allLogs.getResultList().size());
        Assert.assertEquals("Other", allLogs.getResultList().get(0).getTag());
        Assert.assertEquals("Service", allLogs.getResultList().get(1).getTag());
        Assert.assertEquals("RootLogger", allLogs.getResultList().get(2).getTag());
    }

    @Test
    public void memoryAppenderIgnoresEventsAfterClose() {
        MemoryAppender appender = new MemoryAppender();
        appender.setSize(10);

        Assert.assertFalse(appender.requiresLayout());

        appender.close();
        appender.close();
        appender.append(event("com.example.Service", Level.INFO, "ignored"));

        Assert.assertTrue(appender.getLogList().isEmpty());
    }

    @Test
    public void memoryAppenderTrimsOldEntriesWhenQueueExceedsConfiguredSize() {
        MemoryAppender appender = new MemoryAppender();
        appender.setSize(0);

        appender.append(event("com.example.First", Level.INFO, "first"));
        appender.append(event("com.example.Second", Level.INFO, "second"));

        Assert.assertEquals(1, appender.getLogList().size());
        Assert.assertEquals("Second", appender.getLogList().peek().getTag());
    }

    @Test
    public void mimeTypeUtilMapsKnownAndUnknownExtensions() {
        Assert.assertEquals("zip", MimeTypeUtil.getFileExtension(MimeType.APPLICATION_ZIP));
        Assert.assertEquals("gif", MimeTypeUtil.getFileExtension(MimeType.IMAGE_GIF));
        Assert.assertEquals("jpg", MimeTypeUtil.getFileExtension(MimeType.IMAGE_JPEG));
        Assert.assertEquals("png", MimeTypeUtil.getFileExtension(MimeType.IMAGE_PNG));
        Assert.assertEquals("pdf", MimeTypeUtil.getFileExtension(MimeType.APPLICATION_PDF));
        Assert.assertEquals("odt", MimeTypeUtil.getFileExtension(MimeType.OPEN_DOCUMENT_TEXT));
        Assert.assertEquals("docx", MimeTypeUtil.getFileExtension(MimeType.OFFICE_DOCUMENT));
        Assert.assertEquals("txt", MimeTypeUtil.getFileExtension(MimeType.TEXT_PLAIN));
        Assert.assertEquals("csv", MimeTypeUtil.getFileExtension(MimeType.TEXT_CSV));
        Assert.assertEquals("mp4", MimeTypeUtil.getFileExtension(MimeType.VIDEO_MP4));
        Assert.assertEquals("webm", MimeTypeUtil.getFileExtension(MimeType.VIDEO_WEBM));
        Assert.assertEquals("bin", MimeTypeUtil.getFileExtension("application/x-unknown"));
    }

    @Test
    public void googleAuthenticatorConfigBuilderSetsCustomValues() {
        GoogleAuthenticatorConfig config = new GoogleAuthenticatorConfig.GoogleAuthenticatorConfigBuilder()
                .setCodeDigits(8)
                .setTimeStepSizeInMillis(60000)
                .setWindowSize(5)
                .setKeyRepresentation(KeyRepresentation.BASE64)
                .build();

        Assert.assertEquals(8, config.getCodeDigits());
        Assert.assertEquals(100000000, config.getKeyModulus());
        Assert.assertEquals(60000, config.getTimeStepSizeInMillis());
        Assert.assertEquals(5, config.getWindowSize());
        Assert.assertEquals(KeyRepresentation.BASE64, config.getKeyRepresentation());
    }

    @Test
    public void googleAuthenticatorConfigBuilderRejectsInvalidValues() {
        Assert.assertThrows(IllegalArgumentException.class, () ->
                new GoogleAuthenticatorConfig.GoogleAuthenticatorConfigBuilder().setCodeDigits(0));
        Assert.assertThrows(IllegalArgumentException.class, () ->
                new GoogleAuthenticatorConfig.GoogleAuthenticatorConfigBuilder().setCodeDigits(5));
        Assert.assertThrows(IllegalArgumentException.class, () ->
                new GoogleAuthenticatorConfig.GoogleAuthenticatorConfigBuilder().setCodeDigits(9));
        Assert.assertThrows(IllegalArgumentException.class, () ->
                new GoogleAuthenticatorConfig.GoogleAuthenticatorConfigBuilder().setTimeStepSizeInMillis(0));
        Assert.assertThrows(IllegalArgumentException.class, () ->
                new GoogleAuthenticatorConfig.GoogleAuthenticatorConfigBuilder().setWindowSize(0));
        Assert.assertThrows(IllegalArgumentException.class, () ->
                new GoogleAuthenticatorConfig.GoogleAuthenticatorConfigBuilder().setKeyRepresentation(null));
    }

    private LoggingEvent event(String loggerName, Level level, String message) {
        return new LoggingEvent(
                Logger.class.getName(),
                Logger.getLogger(loggerName),
                level,
                message,
                null);
    }
}
