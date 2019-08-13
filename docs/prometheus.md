Run Scanner was built to be monitored by [Prometheus](https://prometheus.io/).
Add Run Scanner as a target of your Prometheus server.

Most variables exported by Run Scanner are prefixed with `miso_runscanner_`.
The default JVM metrics are exported with the prefix `jvm_`.

As a default, we suggest the following alerts:

```
groups:
- name: runscanner.rules
  rules:
  - alert: StuckRuns
    expr: sum_over_time(miso_runscanner_new_runs_scanned[1h]) - sum by(instance, environment,
      job) (rate(miso_runscanner_waiting_runs[1h])) < 3 and time() - process_start_time_seconds  > 8 * 3600
    annotations:
      description: The runs being processed by {{$labels.instance}} are not being
        processed and cleared.
      summary: Runscanner {{$labels.instance}} seems to be stuck
  - alert: BadRuns
    expr: miso_runscanner_directories_attempted - miso_runscanner_directories_accepted
      > 0
    annotations:
      description: The run scanner {{$labels.instance}} has found candidate sequencer
        output directories that it does not have permission to read.
      summary: Unreadable directories run directories on {{$labels.instance}}
  - alert: AutoInhibit
    expr: time() - process_start_time_seconds{job="runscanner"}  < 15 * 60 or sum(miso_runscanner_waiting_runs) by (environment, instance, job) - miso_runscanner_bad_runs > 5
    labels:
      scope: runscanner
    annotations:
      description: Run Scanner was restarted recently and probably needs time to finish scanning old runs
      summary: Runscanner cache is cold
```

`StuckRuns` will fire when Run Scanner no longer seems to be making progress
extracting data from run directories. This usually occurs if I/O latency has
increased or in the case of CPU starvation.

`BadRuns` will fire when there are unreadable runs due to permission errors.
This requires human intervention to correct the directory permissions. Run
Scanner will reattempt scanning the affected runs.

The `AutoInhibit` alert will fire after Run Scanner restarts until the cache is
warm. Since it can take a long period of time for Run Scanner to start up and
scan all available data, it may be useful to stop applications from attempting
access until the run cache is warm. To do this, have the application check for
an `AutoInhibit` alert firing and wait until later. This alert should not be
sent to humans. There is no action to be taken when firing.


If using [MISO](https://github.com/miso-lims/miso-lims) to collect runs from
Run Scanner, we suggest the following alert:

```
groups:
- name: miso.rules
  - alert: DroppedRuns
    expr: miso_runscanner_client_bad_runs > 0
    annotations:
      description: The runs being received by MISO {{$labels.instance}} are not being
        saved to the database.
      summary: Runs are failing to save on {{$labels.instance}}
```

`DroppedRuns` will fire when runs are failing to save in MISO. This can happen
for several reasons:

* the run is for an instrument that is not registered in MISO. Add the
  instrument to MISO or remove that path from Run Scanner's configuration.
* there is a version mismatch between Run Scanner and MISO. Upgrade the lagging
  software package. Check the release notes when upgrading MISO, as Run Scanner
  version changes should be noted there.
* there is a conflict with the MISO configuration: this can include mismatched
  or missing sequencing parameters or container models. Check the
  `miso_debug.log` on the MISO instance to determine the mismatch and correct
  it in MISO.
* the run data is irreconcilably mismatched. For example MISO and Run Scanner
  disagree on the sequencing platform for a run. If MISO is incorrect, delete
  the run and container in MISO and allow Run Scanner to recreate it.

Once a run is marked as dropped, it will not be sent to MISO again unless:
* MISO is restarted
* Run Scanner is restarted
* the run is updated (and it was not previously completed/failed)

Restarting one of MISO or Run Scanner is necessary to clear this alert after
taking corrective action.

If using Grafana, we have included a
[dashboard](https://github.com/miso-lims/runscanner/blob/master/grafana-dashboard.json)
that includes basic metrics for Run Scanner.
