# Ontology Action Request Fixtures

Internal ontology action audit runner v1 uses controlled local action request
fixtures under this directory. The format is `dcai-ontology-action-request-v1`
encoded as Java `.properties`.

These fixtures are not public API contracts and do not authorize browser
writeback, production mutation, external source-system writeback, or AI
governance workflows. They are local deterministic inputs for the
semantic-service CLI action audit runner.

## Submit A Controlled Action Audit

The action runner reads existing managed canonical, provenance, and reasoning
graphs, validates the requested action against those graph facts, and writes
only to a managed `urn:dcai:graph:action-audit:*` graph.

```bash
docker run --rm \
  -v "$PWD":/workspace \
  -w /workspace/semantic-service \
  -e DCAI_FUSEKI_DATASET_URL=http://host.docker.internal:3030/infrastructure \
  gradle:8.10.2-jdk17 \
  gradle --no-daemon run --args="--repo-root=/workspace --submit-ontology-action --action-request-file=fixtures/action-requests/acknowledge-restore-blocker.properties --action-input-release-id=local-controlled-source-v1 --action-reasoning-run-id=local-controlled-reasoning-v1 --action-audit-release-id=local-action-audit-v1"
```

Inspect the action audit graph:

```bash
docker run --rm \
  -v "$PWD":/workspace \
  -w /workspace/semantic-service \
  -e DCAI_FUSEKI_DATASET_URL=http://host.docker.internal:3030/infrastructure \
  gradle:8.10.2-jdk17 \
  gradle --no-daemon run --args="--repo-root=/workspace --inspect-action-audit --inspect-action-audit-release-id=local-action-audit-v1"
```

