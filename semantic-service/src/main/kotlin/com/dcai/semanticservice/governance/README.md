# Governance Package

Internal AI governance proposal/audit v1 keeps AI-generated recommendations
inside a managed `urn:dcai:graph:ai-audit:*` graph. It supports deterministic
local proposal fixtures for reasoning finding suggestions, action
recommendations, and evidence summaries, then validates required source
evidence, confidence, model/prompt metadata, risk, generatedAt, provenance, and
SHACL constraints before writing audit facts.

Human review v1 can approve or reject AI proposals through the private internal
review boundary. Review decisions write only managed ai-audit facts; approved
action recommendations may create governed local action-audit requests. AI
proposal review still does not write AI-generated graph changes into canonical,
reasoning, provenance, source, operations, production, external systems, or any
public endpoint.
