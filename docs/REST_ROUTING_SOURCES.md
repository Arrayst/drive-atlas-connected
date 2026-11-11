# Rest itinerary coordinates and routing contract

Reviewed 2026-11-11. These coordinates are **published representative map markers**, not field-verified vehicle entrances or proof of safe, open parking. Every request containing them needs the representative-point warning and explicit recipient/coordinate consent. A successful road snap does not certify access to the facility.

| ID | Latitude, longitude | Primary source and extraction |
| --- | --- | --- |
| simgok | 37.66521090561137, 129.05301840220107 | [Gangneung Tourism Development Corporation directions](https://searoad.gtdc.or.kr/buchae_location.html), HTML `createParkingMarkers` first coordinate argument labelled 심곡 제1무료주차장 : 승용차량. The second coordinate argument positions a label, not the parking marker. |
| hourglass | 37.68649091946655, 129.0369137380555 | Same official directions page, first coordinate argument labelled 모래시계공원 주차장. Address 정동진리 567 and passenger-car parking also published in the [facilities guide](https://searoad.gtdc.or.kr/buchae_convenient.html). Restroom and stroller suitability are unknown. |
| gwanbang | 35.3239, 126.9858 | [Korea Tourism Organization accessible tourism](https://access.visitkorea.or.kr/ms/detail.do?cotId=74d46d61-8d90-4d78-bba1-2fc52e385450), `findingAway` route link and `locationX` / `locationY` in returned HTML. |
| animation | 37.8930313558032, 127.69186731892 | [Korea Tourism Organization accessible tourism](https://access.visitkorea.or.kr/ms/detail.do?cotId=4116afa4-b861-4106-984e-62507fb0e810), `findingAway` route link and `locationX` / `locationY` in returned HTML. |

No geocoding credentials, user GPS, or new paid services were used to establish these static public markers. A proposed Uiam Skywalk candidate was not added: the city's published destination point is a walking facility with explicit no-vehicle access, requiring separate parking.

## Deterministic itinerary

- Course points keep their existing order. Each chosen rest has an explicit insertion slot: -1 before the first course point, otherwise after that zero-based course point. Missing legacy slots default after the last course point. Within a slot, saved rest-list order is preserved. There is no route optimization claim.
- Invalid IDs, duplicate IDs, wrong-course facilities, invalid slots, invalid or unsourced coordinates block the whole request. No stop is silently omitted.
- Outbound means departure through the first base course point, including preceding rests. Course means first through last base course point, including rests inserted between them. Return means last base course point through any following rests back to departure. All road legs are included once.
- Only designated rest arrivals can reset a continuous-driving segment, and family planning additionally requires explicit positive rest time at that stop. Course shaping points do not reset driving time. Missing per-stop time remains unknown.
- Cache v2 includes public origin coordinates and a SHA-256 identity of ordered stop names, coordinates, rest IDs and slots. v1 entries are not read. Private SDK origins are never cached. Exact response leg totals, splits, snaps, identity and freshness are checked.
- Native SDK full request supports destination plus at most two waypoints. Larger itineraries require explicit individual destination requests while parked; they are not presented as complete native itinerary confirmation. Kakao links are bounded to two through seven points, never truncated.
- OSRM remains debug-only, explicit-request-only, serial and rate-limited. No production service agreement or real-vehicle validation is implied.
