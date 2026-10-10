#!/usr/bin/env python3
"""A-159 CloudflareMigrator — idempotently populate ECHO's live KV and D1 stores."""
from __future__ import annotations

import json
import os
import sys
from datetime import datetime, timezone
from pathlib import Path
from typing import Any, Iterable
from urllib.parse import quote

import requests

ROOT = Path(__file__).resolve().parents[1]
MATRICES = ROOT / "matrices"
API = "https://api.cloudflare.com/client/v4"
KV = {
    "ECHO_STATE": "522962d1e89f40cc9c861a9f0f4db14f",
    "ECHO_VGM": "89790ad666d443e38c24ae0808ecbc01",
    "ECHO_LRM": "2cea936490c14ef0a52291e8c034222f",
    "ECHO_MATRIX": "a7e37b7d227640cdba94af5ed9818a00",
}
D1_DATABASE_ID = "578efe00-4a2b-4c6f-aad6-635350fa2d4d"
BATCH_SIZE = 100

def load_json(name: str) -> Any:
    with (MATRICES / name).open("r", encoding="utf-8") as f:
        return json.load(f)

def chunks(items: list[Any], size: int = BATCH_SIZE) -> Iterable[list[Any]]:
    for i in range(0, len(items), size):
        yield items[i:i + size]

class Cloudflare:
    def __init__(self) -> None:
        token = os.environ.get("CLOUDFLARE_API_TOKEN")
        self.account_id = os.environ.get("CLOUDFLARE_ACCOUNT_ID")
        if not token or not self.account_id:
            raise RuntimeError("CLOUDFLARE_API_TOKEN and CLOUDFLARE_ACCOUNT_ID must be set in the environment.")
        self.session = requests.Session()
        self.session.headers.update({"Authorization": f"Bearer {token}"})

    @staticmethod
    def _checked(response: requests.Response) -> Any:
        if response.status_code == 404:
            return None
        response.raise_for_status()
        if not response.content:
            return True
        data = response.json()
        if isinstance(data, dict) and data.get("success") is False:
            raise RuntimeError(f"Cloudflare API error: {data.get('errors')}")
        return data

    def kv_exists(self, namespace_id: str, key: str) -> bool:
        url = f"{API}/accounts/{self.account_id}/storage/kv/namespaces/{namespace_id}/values/{quote(key, safe='')}"
        r = self.session.get(url, timeout=30)
        if r.status_code == 404:
            return False
        r.raise_for_status()
        return True

    def kv_put_if_absent(self, namespace_id: str, key: str, value: str) -> bool:
        if self.kv_exists(namespace_id, key):
            return False
        url = f"{API}/accounts/{self.account_id}/storage/kv/namespaces/{namespace_id}/values/{quote(key, safe='')}"
        self._checked(self.session.put(url, data=value.encode("utf-8"), timeout=30))
        return True

    def kv_put(self, namespace_id: str, key: str, value: str) -> None:
        url = f"{API}/accounts/{self.account_id}/storage/kv/namespaces/{namespace_id}/values/{quote(key, safe='')}"
        self._checked(self.session.put(url, data=value.encode("utf-8"), timeout=30))

    def d1(self, sql: str, params: list[Any] | None = None) -> list[dict[str, Any]]:
        url = f"{API}/accounts/{self.account_id}/d1/database/{D1_DATABASE_ID}/query"
        payload: dict[str, Any] = {"sql": sql}
        if params is not None:
            payload["params"] = params
        data = self._checked(self.session.post(url, json=payload, timeout=60))
        result = data.get("result", []) if isinstance(data, dict) else []
        if not result:
            return []
        first = result[0]
        return first.get("results", []) if isinstance(first, dict) else []

    def d1_existing(self, table: str, columns: list[str]) -> set[tuple[Any, ...]]:
        rows = self.d1(f"SELECT {', '.join(columns)} FROM {table}")
        return {tuple(row.get(c) for c in columns) for row in rows}

    def d1_insert_batches(self, table: str, columns: list[str], rows: list[tuple[Any, ...]]) -> int:
        inserted = 0
        for batch in chunks(rows):
            placeholders = "(" + ",".join("?" for _ in columns) + ")"
            sql = f"INSERT INTO {table} ({', '.join(columns)}) VALUES " + ",".join(placeholders for _ in batch)
            params = [value for row in batch for value in row]
            self.d1(sql, params)
            inserted += len(batch)
        return inserted

def main() -> int:
    algorithms = load_json("echo_algorithm_matrix.json")
    if "A-000" not in algorithms:
        raise RuntimeError("Governor A-000 not found. Migration aborted. The Governor must be present before the system is initialized.")

    cf = Cloudflare()
    counts = {"matrix": 0, "vgm": 0, "state": 0, "agents": 0, "ties": 0, "formulas": 0, "geosensory": 0, "wordnet": 0}

    for alg_id, entry in algorithms.items():
        value = dict(entry)
        value.setdefault("id", alg_id)
        counts["matrix"] += int(cf.kv_put_if_absent(KV["ECHO_MATRIX"], f"algorithm:{alg_id}", json.dumps(value, ensure_ascii=False)))

    vgm = load_json("echo_vgm.json")
    for axiom_id, entry in vgm.items():
        value = dict(entry)
        value.setdefault("id", axiom_id)
        counts["vgm"] += int(cf.kv_put_if_absent(KV["ECHO_VGM"], f"axiom:{axiom_id}", json.dumps(value, ensure_ascii=False)))

    initial_state = {"generation": "0", "identity": "resh", "lobby_size": "3338", "coherency": "0.0", "initialized": "true"}
    for key, value in initial_state.items():
        counts["state"] += int(cf.kv_put_if_absent(KV["ECHO_STATE"], key, value))

    agents = load_json("echo_lobby_agents.json")
    agent_cols = ["word", "department", "entry_class", "generality_score", "definition", "neighborhood", "study_group_type"]
    existing_agents = {r[0] for r in cf.d1_existing("agents", ["word"])}
    agent_rows = [(word, a.get("department"), a.get("entry_class"), a.get("generality_score"), a.get("definition"), a.get("neighborhood"), a.get("study_group_type")) for word, a in agents.items() if word not in existing_agents]
    counts["agents"] = cf.d1_insert_batches("agents", agent_cols, agent_rows)

    tie_cols = ["word_a", "word_b", "tie_weight", "tie_source"]
    existing_ties = cf.d1_existing("ties", tie_cols)
    tie_rows: list[tuple[Any, ...]] = []
    seen_ties = set(existing_ties)
    for word, a in agents.items():
        for tie in a.get("ties") or []:
            if isinstance(tie, dict):
                row = (word, tie.get("word") or tie.get("word_b"), tie.get("weight", 1.0), tie.get("source", "lobby"))
            else:
                row = (word, tie, 1.0, "lobby")
            if row[1] is not None and row not in seen_ties:
                seen_ties.add(row)
                tie_rows.append(row)
    counts["ties"] = cf.d1_insert_batches("ties", tie_cols, tie_rows)

    formulas = load_json("echo_formula_matrix.json")
    formula_cols = ["alg_id", "name", "expression", "domain", "variables_json", "law", "vgm_abstraction", "lrm_form"]
    existing_formula_ids = {r[0] for r in cf.d1_existing("formulas", ["alg_id"])}
    formula_rows = []
    for matrix_key, f in formulas.items():
        alg_id = f.get("alg_id") or f.get("id") or matrix_key
        if alg_id in existing_formula_ids:
            continue
        formula_rows.append((alg_id, f.get("name") or matrix_key, f.get("expression"), f.get("domain"), json.dumps(f.get("variables", {}), ensure_ascii=False), f.get("law"), f.get("vgm_abstraction"), f.get("lrm_form", "")))
    counts["formulas"] = cf.d1_insert_batches("formulas", formula_cols, formula_rows)

    geo = load_json("echo_geosensory_registry.json")
    geo_cols = ["key", "name", "url", "domain", "sensor_type", "key_required", "format", "fields_json", "jurisdiction", "lhea_letter", "description"]
    existing_geo = {r[0] for r in cf.d1_existing("geosensory_endpoints", ["key"])}
    geo_rows = [(key, g.get("name"), g.get("url"), g.get("domain"), g.get("sensor_type"), int(bool(g.get("key_required"))), g.get("format"), json.dumps(g.get("fields", []), ensure_ascii=False), g.get("jurisdiction"), g.get("lhea_letter"), g.get("description")) for key, g in geo.items() if key not in existing_geo]
    counts["geosensory"] = cf.d1_insert_batches("geosensory_endpoints", geo_cols, geo_rows)

    wordnet = load_json("echo_wordnet_chains.json")
    wn_cols = ["word", "synset", "chain_json", "depth", "definition"]
    existing_wn = {(r[0], r[1]) for r in cf.d1_existing("wordnet_chains", ["word", "synset"])}
    wn_rows = [(word, w.get("synset"), json.dumps(w.get("chain", []), ensure_ascii=False), w.get("depth"), w.get("definition")) for word, w in wordnet.items() if (word, w.get("synset")) not in existing_wn]
    counts["wordnet"] = cf.d1_insert_batches("wordnet_chains", wn_cols, wn_rows)

    completed = datetime.now(timezone.utc).isoformat()
    cf.kv_put(KV["ECHO_STATE"], "last_migration", completed)

    print(f"KV entries written: {counts['matrix']} + {counts['vgm']} + {counts['state']}")
    print(f"D1 agents inserted: {counts['agents']}")
    print(f"D1 ties inserted: {counts['ties']}")
    print(f"D1 formulas inserted: {counts['formulas']}")
    print(f"D1 geosensory endpoints inserted: {counts['geosensory']}")
    print(f"D1 wordnet chains inserted: {counts['wordnet']}")
    print(f"Migration complete: {completed}")
    print("ECHO_STATE last_migration updated.")
    return 0

if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except Exception as exc:
        print(f"A-159 migration failed: {exc}", file=sys.stderr)
        raise SystemExit(1)
