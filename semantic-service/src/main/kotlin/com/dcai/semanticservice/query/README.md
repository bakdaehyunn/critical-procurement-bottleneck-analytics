# Query Package

Contains the approved read-only query catalog loader, Fuseki/TDB2 read-only
query executor, query result envelopes, and graph-binding shapers used by the
private semantic-service endpoint.

`QueryContractRegistry` is the runtime authority that joins manifest
definitions to result codecs, feature ownership, private endpoint exposure, and
stable paging policy. Endpoint approval and shaper dispatch derive from that
registry. Paged SELECT execution counts stable identities and applies bounds in
Fuseki before result shaping.

Runtime query execution is limited to manifest entries with
`implementationStatus "phase16-approved"`. Service-owned Kotlin lifecycle
operations and reference-only historical query metadata are not exposed as
browser-supplied raw SPARQL or SPARQL Update.
