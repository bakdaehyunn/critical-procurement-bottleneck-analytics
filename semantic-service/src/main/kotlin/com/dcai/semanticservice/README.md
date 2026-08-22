# Semantic Service Package Layout

This package tree contains the Kotlin semantic-service runtime for the
ontology-native platform. Earlier Phase 10/11 documents treated most packages
as placeholders; those phase documents are historical records. The current
runtime includes contract checks, controlled fixture loading, approved private
query execution, controlled source promotion, executable reasoning, and local
graph lifecycle inspection.

Current package areas:

- `api`: loopback-only private semantic query endpoint
- `contracts`: contract loading and version checks
- `fixtures`: controlled RDF fixture loading
- `graph`: Fuseki/TDB2 access plus managed graph URI and controlled-ID policy
- `ingestion`: source extract DTOs and controlled local source loaders
- `lifecycle`: read-only graph lifecycle inspection
- `promotion`: source/canonical/provenance promotion orchestration
- `query`: approved query execution and result shaping
- `reasoning`: executable dependency exposure and blast-radius reasoning
- `ontology`: neutral DCAI, PROV, and Dublin Core vocabulary objects
- `validation`: cached SHACL/RDFS validation infrastructure with owned profiles
- `runtime`: separate CLI parsing, composition, typed workflow operations,
  reporting, and loopback endpoint launching
- `governance`: managed AI proposal and human-review audit workflows

Do not add public endpoints, raw SPARQL, SPARQL Update, production connector
writeback, or external AI/system mutation without separately approved scope.
