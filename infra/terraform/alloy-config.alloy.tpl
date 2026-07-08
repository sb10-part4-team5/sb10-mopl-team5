logging {
  level = "info"
}

// Spring Boot /actuator/prometheus 스크랩
discovery.relabel "spring_boot" {
  targets = [{
    __address__ = "mopl:8080",
  }]

  rule {
    target_label = "instance"
    replacement  = "__HOST_IP__:8080"
  }
}

prometheus.scrape "spring_boot" {
  targets         = discovery.relabel.spring_boot.output
  metrics_path    = "/actuator/prometheus"
  scrape_interval = "60s"
  forward_to      = [prometheus.remote_write.grafana_cloud.receiver]
}

// Grafana Cloud Prometheus로 전송
prometheus.remote_write "grafana_cloud" {
  endpoint {
    url = "${prometheus_url}"

    basic_auth {
      username = "${prometheus_username}"
      password = env("GRAFANA_TOKEN")
    }
  }
}

// ECS 컨테이너 로그 수집 (docker socket, mopl 컨테이너만 필터링)
discovery.docker "containers" {
  host = "unix:///var/run/docker.sock"
}

discovery.relabel "mopl_containers" {
  targets = discovery.docker.containers.targets

  rule {
    source_labels = ["__meta_docker_container_name"]
    regex         = ".*-mopl-[^-]+$"
    action        = "keep"
  }
}

loki.source.docker "mopl" {
  host       = "unix:///var/run/docker.sock"
  targets    = discovery.relabel.mopl_containers.output
  forward_to = [loki.write.grafana_cloud.receiver]

  labels = {
    job      = "mopl",
    env      = "prod",
    instance = "__HOST_IP__",
  }
}

// Grafana Cloud Loki로 전송
loki.write "grafana_cloud" {
  endpoint {
    url = "${loki_url}/loki/api/v1/push"

    basic_auth {
      username = "${loki_username}"
      password = env("GRAFANA_TOKEN")
    }
  }
}
