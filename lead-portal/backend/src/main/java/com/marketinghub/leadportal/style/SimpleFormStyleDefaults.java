package com.marketinghub.leadportal.style;

import com.marketinghub.leadportal.model.SimpleFormStyle;
import com.marketinghub.leadportal.model.SimpleFormStyleDefinition;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Provides fallback definitions for known simple form styles when the persisted representation
 * does not include the full visual metadata (for example, legacy flows stored without
 * {@link SimpleFormStyleDefinition}).
 */
@Component
public class SimpleFormStyleDefaults {

    private static final String LUDICO_01_SLUG = "ludico-01";
    private static final String LUDICO_01_NAME = "Estilo lúdico 01";

    private static final String LUDICO_01_HERO_IMAGE =
            "data:image/svg+xml;base64,"
                    + "PHN2ZyB3aWR0aD0iODAwIiBoZWlnaHQ9IjgwMCIgdmlld0JveD0iMCAwIDgwMCA4MDAiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyI+CiAgPGRlZnM+CiAgICA8bGluZWFyR3JhZGllbnQgaWQ9Imx1ZGljb0hlcm8iIHgxPSIwJSIgeTE9IjAlIiB4Mj0iMTAwJSIgeTI9IjEwMCUiPgogICAgICA8c3RvcCBvZmZzZXQ9IjAlIiBzdG9wLWNvbG9yPSIjZmRlNjhhIiAvPgogICAgICA8c3RvcCBvZmZzZXQ9IjUwJSIgc3RvcC1jb2xvcj0iI2ZiY2ZlOCIgLz4KICAgICAgPHN0b3Agb2Zmc2V0PSIxMDAlIiBzdG9wLWNvbG9yPSIjYzdkMmZlIiAvPgogICAgPC9saW5lYXJHcmFkaWVudD4KICAgIDxsaW5lYXJHcmFkaWVudCBpZD0iaGVyb1N0cm9rZSIgeDE9IjAlIiB5MT0iMCUiIHgyPSIxMDAlIiB5Mj0iMCUiPgogICAgICA8c3RvcCBvZmZzZXQ9IjAlIiBzdG9wLWNvbG9yPSIjYTViNGZjIiAvPgogICAgICA8c3RvcCBvZmZzZXQ9IjEwMCUiIHN0b3AtY29sb3I9IiNmNDcyYjYiIC8+CiAgICA8L2xpbmVhckdyYWRpZW50PgogICAgPGZpbHRlciBpZD0ic29mdCIgeD0iLTIwJSIgeT0iLTIwJSIgd2lkdGg9IjE0MCUiIGhlaWdodD0iMTQwJSIgY29sb3ItaW50ZXJwb2xhdGlvbi1maWx0ZXJzPSJzUkdCIj4KICAgICAgPGZlR2F1c3NpYW5CbHVyIHN0ZERldmlhdGlvbj0iMTIiIHJlc3VsdD0iYmx1ciIgLz4KICAgIDwvZmlsdGVyPgogIDwvZGVmcz4KICA8cmVjdCB3aWR0aD0iODAwIiBoZWlnaHQ9IjgwMCIgcng9IjE0MCIgZmlsbD0idXJsKCNsdWRpY29IZXJvKSIgLz4KICA8Y2lyY2xlIGN4PSI2MjAiIGN5PSIxNjAiIHI9IjkwIiBmaWxsPSIjZmRmMmY4IiBvcGFjaXR5PSIwLjc1IiAvPgogIDxjaXJjbGUgY3g9IjE5MCIgY3k9IjIxMCIgcj0iMTIwIiBmaWxsPSIjZGJlYWZlIiBvcGFjaXR5PSIwLjYiIC8+CiAgPGNpcmNsZSBjeD0iNjQwIiBjeT0iNTIwIiByPSIxNTAiIGZpbGw9IiNmZmU0ZTYiIG9wYWNpdHk9IjAuNTUiIC8+CiAgPGNpcmNsZSBjeD0iMjIwIiBjeT0iNTgwIiByPSIxMTAiIGZpbGw9IiNlMGU3ZmYiIG9wYWNpdHk9IjAuNzUiIC8+CiAgPHBhdGggZD0iTTE2MCAzNjAgQzI2MCAzMDAgMzIwIDM2MCA0MDAgMzIwIEM1MjAgMjYwIDYyMCAzMjAgNjYwIDQyMCIgc3Ryb2tlPSJ1cmwoI2hlcm9TdHJva2UpIiBzdHJva2Utd2lkdGg9IjE4IiBzdHJva2UtbGluZWNhcD0icm91bmQiIHN0cm9rZS1saW5lam9pbj0icm91bmQiIGZpbGw9InRyYW5zcGFyZW50IiBmaWx0ZXI9InVybCgjc29mdCkiIC8+CiAgPHBhdGggZD0iTTE1MCA1MjAgQzIzMCA0ODAgMzAwIDU0MCAzNjAgNTAwIEM0NjAgNDMwIDYwMCA1MjAgNjQwIDYwMCIgc3Ryb2tlPSIjZmI3MTg1IiBzdHJva2Utd2lkdGg9IjE0IiBzdHJva2UtbGluZWNhcD0icm91bmQiIHN0cm9rZS1saW5lam9pbj0icm91bmQiIGZpbGw9InRyYW5zcGFyZW50IiBvcGFjaXR5PSIwLjY1IiAvPgogIDxnIG9wYWNpdHk9IjAuMzUiPgogICAgPGNpcmNsZSBjeD0iNTIwIiBjeT0iMzAwIiByPSIxOCIgZmlsbD0iI2Y0NzJiNiIgLz4KICAgIDxjaXJjbGUgY3g9IjU4MCIgY3k9IjM2MCIgcj0iMTIiIGZpbGw9IiMyMmQzZWUiIC8+CiAgICA8Y2lyY2xlIGN4PSI0NjAiIGN5PSI2MjAiIHI9IjE0IiBmaWxsPSIjMzhiZGY4IiAvPgogICAgPGNpcmNsZSBjeD0iMzAwIiBjeT0iNjYwIiByPSIxMiIgZmlsbD0iI2ZjZDM0ZCIgLz4KICA8L2c+Cjwvc3ZnPgo=";

    private static final String LUDICO_01_PATTERN =
            "data:image/svg+xml;base64,"
                    + "PHN2ZyB3aWR0aD0iMzIwIiBoZWlnaHQ9IjMyMCIgdmlld0JveD0iMCAwIDMyMCAzMjAiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyI+CiAgPHJlY3Qgd2lkdGg9IjMyMCIgaGVpZ2h0PSIzMjAiIGZpbGw9Im5vbmUiIC8+CiAgPGcgb3BhY2l0eT0iMC40NSI+CiAgICA8Y2lyY2xlIGN4PSI0MCIgY3k9IjQwIiByPSIxMiIgZmlsbD0iI2ZiY2ZlOCIgLz4KICAgIDxjaXJjbGUgY3g9IjE2MCIgY3k9IjgwIiByPSIxMCIgZmlsbD0iI2ZkZTY4YSIgLz4KICAgIDxjaXJjbGUgY3g9IjI4MCIgY3k9IjQwIiByPSI5IiBmaWxsPSIjYmFlNmZkIiAvPgogICAgPGNpcmNsZSBjeD0iODAiIGN5PSIxNjAiIHI9IjExIiBmaWxsPSIjYzdkMmZlIiAvPgogICAgPGNpcmNsZSBjeD0iMjAwIiBjeT0iMjAwIiByPSIxNCIgZmlsbD0iI2ZkYTRhZiIgLz4KICAgIDxjaXJjbGUgY3g9IjQwIiBjeT0iMjgwIiByPSIxMCIgZmlsbD0iI2E1ZjNmYyIgLz4KICAgIDxjaXJjbGUgY3g9IjI4MCIgY3k9IjI2MCIgcj0iMTIiIGZpbGw9IiNkOGI0ZmUiIC8+CiAgICA8Y2lyY2xlIGN4PSIxNTAiIGN5PSIyNjAiIHI9IjgiIGZpbGw9IiNmY2QzNGQiIC8+CiAgPC9nPgogIDxnIG9wYWNpdHk9IjAuMzUiIHN0cm9rZS13aWR0aD0iNiIgc3Ryb2tlLWxpbmVjYXA9InJvdW5kIj4KICAgIDxwYXRoIGQ9Ik0yMCAxNDAgQzcwIDExMCAxMTAgMTUwIDE1MCAxMjAiIHN0cm9rZT0iI2Y0NzJiNiIgLz4KICAgIDxwYXRoIGQ9Ik0yMDAgNDAgQzI0MCA4MCAyODAgNjAgMzAwIDEwMCIgc3Ryb2tlPSIjMzhiZGY4IiAvPgogICAgPHBhdGggZD0iTTYwIDIyMCBDMTAwIDI2MCAxNDAgMjMwIDE5MCAyNjAiIHN0cm9rZT0iI2Y5NzMxNiIgLz4KICAgIDxwYXRoIGQ9Ik0yMzAgMTUwIEMyNjAgMTEwIDMwMCAxNDAgMzAwIDkwIiBzdHJva2U9IiM3YzNhZWQiIC8+CiAgPC9nPgo8L3N2Zz4K";

    private final Map<String, StyleTemplate> defaults;

    public SimpleFormStyleDefaults() {
        this.defaults = Map.of(
                LUDICO_01_SLUG, new StyleTemplate(LUDICO_01_NAME, buildLudico01Definition()));
    }

    public Optional<SimpleFormStyleDefinition> findDefinition(String slug) {
        return findTemplate(slug).map(StyleTemplate::definition);
    }

    public SimpleFormStyle applyDefaults(SimpleFormStyle style) {
        if (style == null || style.definition() != null) {
            return style;
        }

        return findTemplate(style.slug())
                .map(template -> new SimpleFormStyle(
                        style.slug(),
                        resolveName(style.name(), template),
                        template.definition()))
                .orElse(style);
    }

    private Optional<StyleTemplate> findTemplate(String slug) {
        if (!StringUtils.hasText(slug)) {
            return Optional.empty();
        }
        return Optional.ofNullable(defaults.get(slug));
    }

    private String resolveName(String currentName, StyleTemplate template) {
        if (StringUtils.hasText(currentName)) {
            return currentName;
        }
        return template.name();
    }

    private SimpleFormStyleDefinition buildLudico01Definition() {
        return new SimpleFormStyleDefinition(
                "#fff9f2",
                "linear-gradient(120deg, #fff9f2 0%, #fbe8ff 45%, #e0f2ff 100%)",
                LUDICO_01_PATTERN,
                "#ffffff",
                "rgba(244, 114, 182, 0.25)",
                "0 30px 60px rgba(15,23,42,0.12)",
                "#0f172a",
                "#1f2937",
                "#475569",
                "#7c3aed",
                "#f97316",
                "linear-gradient(135deg, #7c3aed 0%, #f97316 100%)",
                "#ffffff",
                "0 20px 45px rgba(124,58,237,0.35)",
                "32px",
                "rgba(124,58,237,0.12)",
                "#ffffff",
                "rgba(124,58,237,0.25)",
                "image-right",
                LUDICO_01_HERO_IMAGE,
                "rgba(124,58,237,0.15)");
    }

    private record StyleTemplate(String name, SimpleFormStyleDefinition definition) {}
}
