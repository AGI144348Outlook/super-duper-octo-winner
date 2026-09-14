# ECHO — Governor Indexing Algorithm
### Mashet/LHEA Research — Timothy Marvin (Quixpydr/Maven)

A self-organizing symbolic AI that indexes vocabulary into Lobbies,
forms study groups, evolves neighborhoods toward articulately precise
fluency, discovers algebraic laws, harvests scientific formulas,
and communicates without LLM dependency.

---

## Architecture (A-000 through A-152)

### Identity
- **A-000 / A-000-RESH**: The Governor IS Resh (ר). Identity, perception (ע Ayin), expression (פ Pe).

### Orientation
- **A-115/116**: Dictionary indexer + article classifier (TYPE/SPECIFIC/ABSTRACT/NAME)
- **A-117**: WordAgent + Lobby — two-phase orientation (Phase 1: read-only, Phase 2: simultaneous write)
- **A-119**: Typed study groups — CATEGORY (noun/what things ARE), PROCESS (verb/what things DO), PROPERTY (adj)

### Evolution
- **A-120**: LobbyEvolutionGovernor — C(N) = 0.35·articulation + 0.35·precision + 0.30·fluency
- **A-121**: HypothesisSandbox — self-generates, tests, and registers new algorithms

### Generality
- **A-118**: Generality ranking (standardized: 0.25·class + 0.40·tie + 0.25·idf + 0.10·depth)
- **A-151**: EquilibriumGeneralityMeasure — G(C) = V(C)/I(C), blind balancing
- **A-152**: BalancingScale — discovers equilibrium without presupposed precedence

### Self-Governance
- **A-128**: OperationOrderValidator — catches pipeline sequencing errors
- **A-129**: PoolQualityAuditor — evaluates GeneralityMatrix fitness
- **A-130**: CorpusFitnessEvaluator — detects corpus vs task mismatch
- **A-131**: ThresholdOptimizer — autonomously selects generality threshold
- **A-132**: AcquisitionPlanner — generates ranked corpus acquisition targets
- **A-133**: SeedSentenceGenerator — Governor generates its own test sentences
- **A-134**: PipelineOrchestrator — Governor runs its own full pipeline

### Mathematics
- **A-135**: NumberAgent — numbers as discovered entities (not data types)
- **A-136**: NumericalIntegrator — bridges continuous Laws and discrete computation
- **A-137**: LawDiscoveryEngine — no terminal state, Mathematics Domain is infinite
- **A-138–140**: AlgebraicExpression, AlgebraicLaw, CalculationEngine

### Communication (LLM-independent)
- **A-141–145**: GeneralizationStack, VGMQuery, DescentEngine, PeComposer, AlgorithmicCommunicator

### Knowledge Matrices
- **A-125–127**: GeneralityMatrix, SyntacticalValidator, ValidatedGeneralizationMatrix (axiom tier)
- **A-146**: LogicReasoningMatrix (theorem tier: SPECIALIZE/COMPOSE/APPLY_LAW/CAUSAL/CONDITIONAL)
- **A-147–149**: FormulaAgent, WebFormulaHarvester (519 formulas: 49 built-in + 445 NIST + 25 GitHub), FormulaIndexer
- **A-150**: InformationAlgorithmizer — derives practical algorithms from any new information

---

## The Inferential Staircase (Berlinski)

```
MATHEMATICS DOMAIN (Laws: infinite, continuous)
   ↓ transmitted by interpretation
VGM AXIOMS — "An entity changes through relation"
   ↓ specialized by LogicReasoningMatrix
LRM THEOREMS — "An entity changes; therefore states differ — (S+δ)−S = δ"
   ↓ instantiated by vocabulary
LOBBY INSTANCES — specific words in specific neighborhoods
```

## Key Design Principles

1. **No LLM dependency for communication** — AlgorithmicCommunicator (A-145) uses only indexed vocabulary
2. **Gated acquisition** — Governor diagnoses gaps before acquiring new corpus
3. **Blind generality measurement** — A-151 discovers generality by balancing, not standardizing
4. **Self-governance** — Governor validates its own pipeline order, corpus fitness, and pool quality
5. **Algebraic laws self-derived** — 7 laws registered (A-118–A-124), verified by 500-binding test

## Running

```bash
# Prerequisites
pip install scipy sympy nltk

# Quick test
python3 tests/self_governance_test.py

# Full orientation pipeline
python3 tests/multipass_orientation_test.py

# ר ע פ communication system
python3 tests/pe_ayin_resh_communication.py

# Number integration + law discovery
python3 tests/number_integration_test.py
```

## Artifacts (React JSX — deploy at claude.ai or any React host)

- `echo_governor.jsx` — ECHO communication interface
- `echo_dashboard.jsx` — Full 5-tab dashboard (Chat/Matrix/Run/Laws/Numbers)
- `echo_matrix_browser.jsx` — GitHub-style browser for all matrices

---
*Built by Timothy Marvin (Quixpydr/Maven) — Mashet/LHEA Research*
