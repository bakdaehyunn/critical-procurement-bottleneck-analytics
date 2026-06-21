# Query Package

Contains the approved read-only query catalog loader, Fuseki/TDB2 read-only
query executor, query result envelopes, and graph-binding shapers used by the
private semantic-service endpoint.

Runtime query execution is limited to manifest entries with
`implementationStatus "phase16-approved"`. Service-owned Kotlin lifecycle
operations and reference-only historical query metadata are not exposed as
browser-supplied raw SPARQL or SPARQL Update.
