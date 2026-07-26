package com.maddiewest.events.config;

import com.maddiewest.events.document.RentalItem;
import com.maddiewest.events.document.RentalItemMetadata;
import com.maddiewest.events.repository.RentalItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Seeds a handful of sample rental items in the "dev" profile so the public
 * browsing endpoints have data to return during local development.
 */
@Component
@Profile("dev")
@RequiredArgsConstructor
public class DevDataSeeder implements CommandLineRunner {

    private final RentalItemRepository rentalItemRepository;

    @Override
    public void run(String... args) {
        if (rentalItemRepository.count() > 0) {
            return;
        }

        rentalItemRepository.saveAll(List.of(
                item("Wooden Arbor", "Rustic wooden arbor, perfect for ceremony backdrops.",
                        "Arbors", 4, "120.00", new RentalItemMetadata("Natural Wood", "Pine", "7ft x 5ft")),
                item("Gold Vase", "Tall gold vase for centerpieces.",
                        "Vases", 50, "8.00", new RentalItemMetadata("Gold", "Metal", "16in tall")),
                item("White Linen Tablecloth", "Floor-length white linen tablecloth for round tables.",
                        "Linens", 30, "15.00", new RentalItemMetadata("White", "Linen", "120in round")),
                item("String Lights", "Warm white string lights for tent or outdoor lighting.",
                        "Lighting", 20, "25.00", new RentalItemMetadata("Warm White", "LED", "48ft strand"))
        ));
    }

    private RentalItem item(String name, String description, String category, int totalQuantity, String price,
                             RentalItemMetadata metadata) {
        RentalItem item = new RentalItem();
        item.setName(name);
        item.setDescription(description);
        item.setCategory(category);
        item.setTotalQuantity(totalQuantity);
        item.setPrice(new BigDecimal(price));
        item.setMetadata(metadata);
        item.setActive(true);
        return item;
    }
}
