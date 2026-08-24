# Verification Plan

Run commands from the repository root unless a command changes directory.

## Frontend

```bash
cd frontend
npm test
npm run lint
npm run build
```

These checks cover current unit contracts, feature-boundary lint rules, and
TypeScript/Vite production compilation. They are not browser E2E tests or an
accessibility certification.

## Semantic service

```bash
docker run --rm \
  -v "$PWD":/workspace \
  -w /workspace/semantic-service \
  gradle:8.10.2-jdk17 \
  gradle --no-daemon test
```

The suite covers contract loading, query ownership, DTO shaping, private route
handling, source mapping, SHACL/provenance gates, deterministic scenario
generation, reasoning, governed local actions, and injected write rollback.
`RecordedSourceScenarioGeneratorTest.publicPortfolioScenariosExistAndReachTheirDocumentedReasoningFindings`
checks the three public scenario IDs through promotion and expected finding
types. `QueryContractRegistryTest` checks manifest/registry/frontend ownership
agreement.

## SPARQL parsing

With `rdflib` installed in the selected `PYTHONPATH`:

```bash
PYTHONPATH=/tmp/dcai-rdf-tools python3 queries/validate_sparql.py
```

This parses all executable `.rq` files and rejects update operations. It does
not benchmark Fuseki query latency.

## RDF parsing and SHACL

The Gradle suite is the authoritative executable SHACL check for representative
valid and invalid source, canonical, reasoning, action, and governance models.
To additionally parse every committed Turtle artifact:

```bash
PYTHONPATH=/tmp/dcai-rdf-tools python3 - <<'PY'
from pathlib import Path
from rdflib import Graph

for path in sorted(Path('.').glob('**/*.ttl')):
    if '.git' not in path.parts:
        Graph().parse(path, format='turtle')
print('all Turtle artifacts parsed')
PY
```

## Repository truth checks

```bash
rg -n "SCN-20260611|SCN-20260613|SCN-20260616" \
  fixtures/source-extracts/generated-scenarios/mvp-seed-20260610/scenario_inventory.csv
rg -n "semanticAvailableActionsByFinding" \
  queries/manifest.ttl \
  semantic-service/src/main/kotlin/com/dcai/semanticservice/query/QueryContractRegistry.kt \
  frontend/src/semanticQueryCatalog.ts
docker compose config
git diff --check
git status --short
```

Documentation links in the current reading path should resolve to existing
repository files. Historical documents are not scanned as current capability
claims; their status is defined in `docs/ontology-native/README.md`.

## Interpretation

A green run proves deterministic local contracts and compilation. It does not
prove production performance, domain correctness, security, availability,
business impact, real connector compatibility, or infrastructure execution.
