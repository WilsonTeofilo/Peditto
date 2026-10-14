package com.peditto.identity.application;

import com.peditto.identity.domain.IdentityInputNormalizer;
import java.text.Normalizer;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class StoreSlugFactory {
    public String uniqueSlug(String storeName, java.util.function.Predicate<String> exists) {
        String ascii = Normalizer.normalize(IdentityInputNormalizer.requiredDisplayName(storeName, 120), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
        if (ascii.isBlank()) ascii = "loja";
        String base = ascii.substring(0, Math.min(54, ascii.length())).replaceAll("-$", "");
        for (int attempt = 0; attempt < 5; attempt++) {
            String slug = base + "-" + UUID.randomUUID().toString().substring(0, 8);
            if (!exists.test(slug)) return slug;
        }
        throw new IllegalStateException("Não foi possível reservar o endereço da loja.");
    }
}
