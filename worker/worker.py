#!/usr/bin/env python3
"""Contract-000 inert worker. Standard library only; no network or writes to repo."""
import argparse, datetime as dt, hashlib, json, os, pathlib, time

ROLES = ("coordinator", "researcher", "experimenter", "auditor", "archivist")
def utc(): return dt.datetime.now(dt.timezone.utc).isoformat()
def run(duration=300, checkpoint=60, output="worker-results", live=False, sleeper=time.sleep, clock=time.monotonic):
    if duration < 1 or checkpoint < 1: raise ValueError("positive duration and checkpoint required")
    root = pathlib.Path(output); root.mkdir(parents=True, exist_ok=True)
    started = clock(); events = []; index = 0
    def record(kind, **details):
        item = {"sequence":len(events)+1,"at_utc":utc(),"kind":kind,**details}
        events.append(item)
        (root/"events.jsonl").write_text("".join(json.dumps(x,sort_keys=True)+"\n" for x in events))
    record("start",mode="live" if live else "inert",roles=ROLES,duration_seconds=duration,checkpoint_seconds=checkpoint)
    # Inert mode is a bounded deterministic simulation, not a sleeping five-minute job.
    if not live:
        for elapsed in range(checkpoint,duration+1,checkpoint):
            record("checkpoint",elapsed_seconds=min(elapsed,duration),status="inert-no-external-actions")
        if duration % checkpoint: record("checkpoint",elapsed_seconds=duration,status="inert-no-external-actions")
    else:
        while True:
            elapsed = clock()-started
            if elapsed >= duration: break
            target = min(duration,(index+1)*checkpoint)
            sleeper(max(0,target-elapsed))
            index += 1
            record("checkpoint",elapsed_seconds=min(duration,round(clock()-started,3)),status="inert-no-external-actions")
    record("finish",status="completed",external_calls=0,repository_mutations=0)
    digest=hashlib.sha256((root/"events.jsonl").read_bytes()).hexdigest()
    (root/"summary.json").write_text(json.dumps({"schema":"contract-000-inert-worker/v1","mode":"live" if live else "inert","roles":ROLES,"event_count":len(events),"sha256_events":digest,"external_calls":0,"repository_mutations":0},indent=2)+"\n")
    return events
if __name__=="__main__":
    p=argparse.ArgumentParser()
    p.add_argument("--duration",type=int,default=300)
    p.add_argument("--checkpoint",type=int,default=60)
    p.add_argument("--output",default="worker-results")
    p.add_argument("--live",action="store_true",help="Wait in real time; still inert, no external actions")
    a=p.parse_args(); run(a.duration,a.checkpoint,a.output,a.live)
