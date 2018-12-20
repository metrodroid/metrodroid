# osm

Example:

* Yamanote line (inner): https://www.openstreetmap.org/relation/1972960
* Yamanote line (outer): https://www.openstreetmap.org/relation/1972920
* Yamanote line (parent relation): https://www.openstreetmap.org/relation/1139468

```
// Setup a boundary for Japan
area["ISO3166-1"="JP"][admin_level=2][boundary=administrative]->.jp;

// Select all train routes in Japan with English and Japanese names.
(
  // route*=train: The route that a train service takes
  // https://wiki.openstreetmap.org/wiki/Tag:route%3Dtrain
  rel[route=train]["name"]["name:en"](area.jp);
  
  // route=railway: The physical infrastructure
  // (useful as a fallback)
  rel[route=railway]["name"]["name:en"](area.jp);
  
) -> .routes;

// Also select parent route_masters
(
  rel(br.routes)[route_master];
  rel.routes;
) -> .routes;

// Output union
(
  // All train routes
  rel.routes;
  
  // All stations on those routes with English and Japanese names.
  node(r.routes)[railway=station]["name"]["name:en"];

  // Sometimes a station is listed as just a different sort of stop.
  // eg: https://www.openstreetmap.org/relation/5419188
  node(r.routes:"stop")["name"]["name:en"];
);

out;
```

