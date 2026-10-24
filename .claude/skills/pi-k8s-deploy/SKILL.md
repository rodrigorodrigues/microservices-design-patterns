---
name: pi-k8s-deploy
description: Check Docker Hub for a new :latest image on a managed service and roll it out to the home Pi k8s cluster, the same way authentication-service was deployed on 2026-08-29 (SSH + kubectl rollout restart/status, startupProbe fix for slow-starting Spring Boot pods). Use when asked to check for or deploy new service images on the Pi cluster, or to add a service to the watch list.
---

# Deploying to the Pi k8s cluster

## Context

- Cluster: k8s on a Raspberry Pi, reachable at `ubuntu@192.168.1.46` via SSH key auth
  (already set up - `ssh-copy-id` was run once; never put the SSH password in this
  repo, it's public).
- Deployment manifests live in `~/k8s-files/deployment-<service>.yml` **on the Pi**,
  not in this git repo - edit them via `ssh`/`scp`, not locally.
- Images are published to Docker Hub as `fielcapao/microservices-design-patterns-<service>`,
  tag `:latest`.
- Managed services live in `services.yaml` next to this file. Add an entry there
  for each service this skill should watch/deploy - don't guess at services that
  aren't listed.

## Procedure

For each managed service in `services.yaml`:

1. **Digest on Docker Hub:**
   ```bash
   curl -s "https://hub.docker.com/v2/repositories/<repo>/tags/latest" | jq -r .digest
   ```
   (See "Private repos" below if this 401s.)

2. **Digest actually running in the cluster** (filter by container name, not
   index - sidecars like istio-proxy also show up in `containerStatuses`):
   ```bash
   ssh ubuntu@192.168.1.46 "kubectl get pods -l app=<deployment> -o jsonpath='{.items[0].status.containerStatuses[?(@.name==\"<container>\")].imageID}'"
   ```
   This prints `docker.io/<repo>@sha256:<digest>` - compare the `sha256:...`
   suffix against step 1's digest.

3. **If they match:** nothing to do for this service.

4. **If they differ, deploy:**
   ```bash
   ssh ubuntu@192.168.1.46 "kubectl rollout restart deployment/<deployment>"
   ssh ubuntu@192.168.1.46 "kubectl rollout status deployment/<deployment> --timeout=6m"
   ```
   `rollout status` blocks until the new pod is Ready or the timeout hits. With
   a correctly-sized startup probe (see below) this should take low minutes,
   not the ~17 minutes the mis-configured authentication-service probe used to
   cause. The old pod keeps serving traffic until the new one passes readiness,
   so there's no downtime either way.

5. **Report** per service: old digest -> new digest (short form is fine), and
   whether the rollout completed within the timeout.

6. **If `rollout status` times out or reports a failure:** stop, don't retry
   blindly, and don't roll back automatically. Show the user
   `kubectl get pods -l app=<deployment>` and the last ~30 lines of
   `kubectl logs deployment/<deployment>` so they can see why, and let them
   decide (rollback with `kubectl rollout undo deployment/<deployment>` is a
   separate, deliberate action, not an automatic fallback here).

## Before adding a new service to services.yaml

Check `~/k8s-files/deployment-<name>.yml` for a startup pattern like
`initialDelaySeconds: 1000` on liveness/readiness with no `startupProbe`. That
pattern makes step 4's `rollout status` look like it's hanging for the full
delay even once the pod is actually healthy. Fix it first (see below), then
add the service here.

## Fixing slow-startup probes (one-time, per service)

This mirrors the actual fix applied to authentication-service on 2026-08-29:
it had `initialDelaySeconds: 1000` (~17 min) on both probes, but its own
startup log showed it was ready in ~70 seconds:

```
Started AuthenticationServiceApplication in 70.587 seconds (process running for 76.503)
```

Find the real number for the service you're fixing before picking values -
don't reuse 1000s, and don't guess a small number either:

```bash
ssh ubuntu@192.168.1.46 "kubectl logs deployment/<deployment> | grep -i started"
```

Then replace the bare `initialDelaySeconds` probes with a `startupProbe` that
gates them, sized with headroom (~3x observed startup) over that real number,
and short liveness/readiness periods once startup passes:

```yaml
startupProbe:
  httpGet:
    scheme: HTTP
    path: /actuator/health/readiness
    port: <port>
  periodSeconds: 10
  failureThreshold: 24        # ~4 min budget here for a ~70s observed startup - rescale per service
livenessProbe:
  httpGet:
    scheme: HTTP
    path: /actuator/health/liveness
    port: <port>
  periodSeconds: 10
  failureThreshold: 3
readinessProbe:
  httpGet:
    scheme: HTTP
    path: /actuator/health/readiness
    port: <port>
  periodSeconds: 5
  failureThreshold: 3
```

Edit `~/k8s-files/deployment-<name>.yml` on the Pi (scp a local copy back and
forth if that's easier than editing in place), then:

```bash
ssh ubuntu@192.168.1.46 "kubectl apply -f ~/k8s-files/deployment-<name>.yml"
```

Applying a probe change starts a new rollout on its own - watch it the same
way as step 4 above.

## Private repos

The `fielcapao/microservices-design-patterns-*` repos this skill was built
against are public - the anonymous `curl` in step 1 works as-is. If a service
you add is actually private, get a token first and never hardcode the
credentials that produce it in this repo (public):

```bash
TOKEN=$(curl -s -H "Content-Type: application/json" \
  -d "{\"username\":\"$DOCKERHUB_USERNAME\",\"password\":\"$DOCKERHUB_TOKEN\"}" \
  https://hub.docker.com/v2/users/login | jq -r .token)
curl -s -H "Authorization: Bearer $TOKEN" "https://hub.docker.com/v2/repositories/<repo>/tags/latest" | jq -r .digest
```

Read `DOCKERHUB_USERNAME`/`DOCKERHUB_TOKEN` from the environment the user has
set up locally - don't write actual values into this file or `services.yaml`.

## Running this on a schedule ("listening" for changes)

This skill is a procedure, not a background process - invoking it once checks
every managed service and deploys anything that's changed, then stops. To
actually watch continuously, invoke it periodically yourself:

- `/loop 30m /pi-k8s-deploy` for a foreground polling loop in a session, or
- the `schedule` skill, for a recurring cloud-run cron job that doesn't need a
  session open.

Neither is set up by default - set one up explicitly if continuous watching
(rather than on-demand "check now") is what's wanted.
