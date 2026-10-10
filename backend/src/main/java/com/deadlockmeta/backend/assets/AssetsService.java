package com.deadlockmeta.backend.assets;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class AssetsService {

    private static final Logger log = LoggerFactory.getLogger(AssetsService.class);

    private final RestClient restClient;

    private volatile Map<Integer, HeroAsset> heroes = Map.of();
    private volatile Map<Long, ItemAsset> items = Map.of();
    private volatile Map<Integer, RankAsset> ranks = Map.of();

    public AssetsService(@Value("${app.assets.base-url}") String baseUrl) {
        this.restClient = RestClient.create(baseUrl);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void loadAssets() {
        try {
            List<HeroAsset> heroList = restClient.get()
                    .uri("/v1/assets/heroes")
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<HeroAsset>>() {
                    });

            List<ItemAsset> itemList = restClient.get()
                    .uri("/v1/assets/items")
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<ItemAsset>>() {
                    });

            List<RankAsset> rankList = restClient.get()
                    .uri("/v1/assets/ranks")
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<RankAsset>>() {
                    });

            heroes = Objects.requireNonNullElse(heroList, List.<HeroAsset>of()).stream()
                    .collect(Collectors.toMap(HeroAsset::id, Function.identity(), (first, second) -> first));
            items = Objects.requireNonNullElse(itemList, List.<ItemAsset>of()).stream()
                    .collect(Collectors.toMap(ItemAsset::id, Function.identity(), (first, second) -> first));
            ranks = Objects.requireNonNullElse(rankList, List.<RankAsset>of()).stream()
                    .collect(Collectors.toMap(RankAsset::tier, Function.identity(), (first, second) -> first));

            log.info("Loaded assets: {} heroes, {} items, {} ranks", heroes.size(), items.size(), ranks.size());
        } catch (Exception e) {
            log.error("Could not load assets from the Deadlock API", e);
        }
    }

    public String heroName(int heroId) {
        HeroAsset hero = heroes.get(heroId);
        return hero != null ? hero.name() : "Unknown hero " + heroId;
    }

    public String itemName(long itemId) {
        ItemAsset item = items.get(itemId);
        return item != null ? item.name() : "Unknown item " + itemId;
    }

    public boolean isShopItem(long itemId) {
        ItemAsset item = items.get(itemId);
        return item != null && "upgrade".equals(item.type());
    }

    public String rankName(int badge) {
        int tier = badge / 10;
        int subrank = badge % 10;
        RankAsset rank = ranks.get(tier);
        if (rank == null) {
            return "Unknown rank " + badge;
        }
        return subrank == 0 ? rank.name() : rank.name() + " " + subrank;
    }

    public Map<String, Long> itemCountsByType() {
        return items.values().stream()
                .collect(Collectors.groupingBy(item -> String.valueOf(item.type()), Collectors.counting()));
    }

    public int heroCount() {
        return heroes.size();
    }

    public int itemCount() {
        return items.size();
    }

    public int rankCount() {
        return ranks.size();
    }
}