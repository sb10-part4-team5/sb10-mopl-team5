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

// ECS 컨테이너 로그 수집 — docker socket은 컨테이너 목록 조회(discovery)에만 쓰고,
// 실제 로그 내용은 호스트의 json-file 로그 파일을 직접 읽는다.
discovery.docker "containers" {
  host = "unix:///var/run/docker.sock"
}

discovery.relabel "mopl_containers" {
  targets = discovery.docker.containers.targets

  // keep/drop 액션은 이 환경에서 와일드카드(.+, .*)가 들어간 정규식이면
  // 매칭 여부와 무관하게 판정이 깨지는 문제가 있다(원인 불명, Alloy 버그로 추정).
  // 와일드카드 없는 정확한 문자열은 정상 동작하므로, ECS가 컨테이너마다 자동으로
  // 붙여주는 태스크 정의상의 컨테이너명 라벨(값이 항상 "mopl"로 고정)을 사용한다.
  rule {
    source_labels = ["__meta_docker_container_label_com_amazonaws_ecs_container_name"]
    regex         = "mopl"
    action        = "keep"
  }

  rule {
    source_labels = ["__meta_docker_container_id"]
    regex         = "(.+)"
    target_label  = "__path__"
    replacement   = "/var/lib/docker/containers/$${1}*/*-json.log"
  }

  rule {
    target_label = "job"
    replacement  = "mopl"
  }

  rule {
    target_label = "env"
    replacement  = "prod"
  }

  rule {
    target_label = "instance"
    replacement  = "__HOST_IP__"
  }
}

local.file_match "mopl_logs" {
  path_targets = discovery.relabel.mopl_containers.output
}

loki.source.file "mopl" {
  targets    = local.file_match.mopl_logs.targets
  forward_to = [loki.process.docker_json.receiver]
}

// docker json-file 포맷({"log":"...","stream":"...","time":"..."})을 벗겨서
// 실제 로그 텍스트만 뽑아내고, 원본 타임스탬프를 그대로 사용한다.
loki.process "docker_json" {
  forward_to = [loki.write.grafana_cloud.receiver]

  stage.json {
    expressions = {
      output = "log",
      stream = "stream",
      ts     = "time",
    }
  }

  stage.output {
    source = "output"
  }

  stage.timestamp {
    source = "ts"
    format = "RFC3339Nano"
  }

  stage.labels {
    values = {
      stream = "stream",
    }
  }

  // filename에는 컨테이너 ID가 박혀있어 배포/재시작마다 값이 바뀐다.
  // 라벨로 남기면 배포할 때마다 새 스트림이 생겨 고카디널리티 문제가 되므로 드롭한다.
  stage.label_drop {
    values = ["filename"]
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
