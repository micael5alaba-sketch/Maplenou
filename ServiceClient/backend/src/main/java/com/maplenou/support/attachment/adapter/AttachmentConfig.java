package com.maplenou.support.attachment.adapter;

import com.maplenou.support.attachment.adapter.out.scan.ClamAvScanner;
import com.maplenou.support.attachment.adapter.out.scan.NoMalwareScanner;
import com.maplenou.support.attachment.adapter.out.storage.CloudinaryStorage;
import com.maplenou.support.attachment.adapter.out.storage.LocalFileStorage;
import com.maplenou.support.attachment.application.AttachmentLimits;
import com.maplenou.support.attachment.application.port.BinaryStorage;
import com.maplenou.support.attachment.application.port.MalwareScanner;
import com.maplenou.support.platform.config.MaplenouProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.web.client.RestClient;

import java.time.Clock;

/** Chooses the storage ({@code ATTACHMENT_STORAGE}) and the antivirus ({@code ANTIVIRUS}) from the configuration. */
@Configuration
public class AttachmentConfig {

    @Bean
    AttachmentLimits attachmentLimits(MaplenouProperties props) {
        MaplenouProperties.Attachments a = props.attachments();
        return new AttachmentLimits(a.maxFileBytes(), a.maxFilesPerMessage(), a.maxTicketBytes(),
                Boolean.TRUE.equals(a.failClosed()));
    }

    @Bean
    BinaryStorage binaryStorage(MaplenouProperties props, RestClient.Builder builder, Clock clock, Environment env) {
        MaplenouProperties.Attachments a = props.attachments();
        if ("cloudinary".equalsIgnoreCase(a.storage())) {
            if (a.cloudinaryUrl() == null || a.cloudinaryUrl().isBlank()) {
                throw new IllegalStateException("ATTACHMENT_STORAGE=cloudinary requires CLOUDINARY_URL");
            }
            return new CloudinaryStorage(builder, a.cloudinaryUrl(), a.cloudinaryFolder(),
                    env.getProperty("maplenou.attachments.cloudinary-api-base"), clock);
        }
        return new LocalFileStorage(a.storageDir());
    }

    @Bean
    MalwareScanner malwareScanner(MaplenouProperties props) {
        MaplenouProperties.Attachments a = props.attachments();
        if ("clamav".equalsIgnoreCase(a.antivirus())) {
            return new ClamAvScanner(a.clamavHost() == null ? "localhost" : a.clamavHost(), a.clamavPort(),
                    a.scanTimeout());
        }
        return new NoMalwareScanner();
    }
}
