# Governance Package

Internal AI governance proposal/audit v1 keeps AI-generated recommendations
inside a managed `urn:dcai:graph:ai-audit:*` graph. It supports deterministic
local proposal fixtures for reasoning finding suggestions, action
recommendations, and evidence summaries, then validates required source
evidence, confidence, model/prompt metadata, risk, generatedAt, provenance, and
SHACL constraints before writing audit facts.

This package deliberately does not approve, reject, or write AI-generated graph
changes into canonical, reasoning, provenance, source, operations, or production
graphs. Review UI controls are read-only/audit-only until a separate approval
and writeback architecture is explicitly approved.
